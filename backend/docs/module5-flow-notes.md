# Module 5 — Transaction Processor

## Decisiones de diseno

### Scope del modulo
Este modulo maneja el **procesamiento de pagos** para bookings confirmados via Stripe (Test Mode).
Implementa el split de comision 80/20 (80% owner payout, 20% platform fee), cobro de security deposit,
confirmacion de pago, manejo de webhooks, y reembolsos por cancelacion.

**Justificacion (del Progress Report 1 — Revenue Model):**
- Turbo aplica un modelo de comision 80/20 sobre cada transaccion
- Drivers pagan el rental fee + security deposit al confirmar booking
- Car Owners reciben 80% del rental fee
- Turbo retiene 20% como service fee
- Stripe Test Mode provee flujo completo sin dinero real (ADR-006)

### Domain name
- **Nombre:** `payment`
- **Package:** `com.turbo.payment`
- **Frontend module:** `src/modules/booking` (integrado en el modulo de booking)

### Dependencias con otros modulos
- **Module 4 (Booking):** Payment se crea para bookings CONFIRMED. BookingService llama `refundPaymentForCancelledBooking()` al cancelar.
- **Module 1 (Auth):** SecurityHelper para obtener usuario autenticado y validar acceso.

---

## Enums

### PaymentStatus
**Package:** `com.turbo.payment.model.enums`

| Valor | Significado | Transiciones desde |
|-------|-------------|-------------------|
| `PENDING` | PaymentIntent creado, esperando confirmacion de Stripe | (estado inicial) |
| `COMPLETED` | Pago exitoso (tarjeta cobrada) | PENDING |
| `FAILED` | Stripe fallo al procesar el pago | PENDING |
| `REFUNDED` | Pago reembolsado por cancelacion de booking | COMPLETED |

**State machine:**
```
PENDING ──[Stripe succeeds / frontend confirms]──> COMPLETED ──[booking cancelled]──> REFUNDED
  |
  └──[Stripe fails]──> FAILED
```

---

## Error Codes

### PaymentErrorCode
**Package:** `com.turbo.exception.error`

| Codigo | Nombre | HTTP Status | Descripcion |
|--------|--------|-------------|-------------|
| PAY-001 | BOOKING_NOT_FOUND | 404 | Booking no existe |
| PAY-002 | UNAUTHORIZED_PAYMENT_ACCESS | 403 | Usuario no tiene acceso al pago (no es driver ni owner del booking) |
| PAY-003 | BOOKING_NOT_CONFIRMABLE_FOR_PAYMENT | 400 | Booking no esta en status CONFIRMED |
| PAY-004 | STRIPE_CONFIG_MISSING | 503 | Stripe secret key o webhook secret no configurados |
| PAY-005 | PAYMENT_ALREADY_COMPLETED | 409 | Ya existe un pago COMPLETED para este booking |
| PAY-006 | STRIPE_PAYMENT_INTENT_FAILED | 502 | Stripe API fallo al crear PaymentIntent |
| PAY-007 | PAYMENT_NOT_FOUND | 404 | No existe registro de pago para el booking |
| PAY-008 | INVALID_STRIPE_WEBHOOK_SIGNATURE | 400 | Firma del webhook no coincide con el secret configurado |
| PAY-009 | STRIPE_WEBHOOK_PROCESSING_FAILED | 500 | Error interno procesando evento de webhook |
| PAY-010 | PAYMENT_REFUND_FAILED | 502 | Stripe API fallo al procesar reembolso |

---

## Entidades del modulo

### Payment (nueva)
**Table name:** `payments`
**Package:** `com.turbo.payment.model`

| # | Campo | Java Type | Columna | Nullable | Constraints | Descripcion |
|---|-------|-----------|---------|----------|-------------|-------------|
| 1 | `paymentId` | `Long` | `payment_id` PK | No | `@Id`, `@GeneratedValue(IDENTITY)` | Primary key auto-generada |
| 2 | `booking` | `Booking` | `booking_id` FK -> `bookings` | No | `@OneToOne(LAZY)`, `@JoinColumn(unique=true, nullable=false)` | Booking asociado. Relacion 1:1 estricta |
| 3 | `amount` | `BigDecimal` | `amount` DECIMAL(19,4) | No | Total cobrado (baseAmount + securityDeposit) | Monto total enviado a Stripe |
| 4 | `platformFee` | `BigDecimal` | `platform_fee` DECIMAL(19,4) | No | baseAmount * 0.20 | Comision de Turbo (20%) |
| 5 | `ownerPayout` | `BigDecimal` | `owner_payout` DECIMAL(19,4) | No | baseAmount * 0.80 | Pago al car owner (80%) |
| 6 | `securityDeposit` | `BigDecimal` | `security_deposit` DECIMAL(19,4) | No | Default $200.00 CAD (configurable) | Deposito de seguridad |
| 7 | `status` | `PaymentStatus` | `status` | No | `@Enumerated(STRING)`, default PENDING via `@PrePersist` | Estado actual del pago |
| 8 | `currency` | `String` | `currency` length=10 | No | Default "cad" | Codigo ISO 4217 de moneda |
| 9 | `stripePaymentIntentId` | `String` | `stripe_payment_intent_id` | Yes | `unique=true` | ID del PaymentIntent en Stripe |
| 10 | `createdAt` | `LocalDateTime` | `created_at` | No | `@PrePersist`, `updatable=false` | Timestamp de creacion |
| 11 | `updatedAt` | `LocalDateTime` | `updated_at` | No | `@PrePersist` + `@PreUpdate` | Timestamp de ultima modificacion |

> **Nota:** `amount` es el monto TOTAL cobrado al driver (rental + deposit). Para obtener el rental base: `amount - securityDeposit`.

> **Nota:** La precision DECIMAL(19,4) cumple con el NFR de Financial Accuracy del Progress Report 1.

---

## Configuracion

### application.properties
```properties
stripe.secret-key=${STRIPE_SECRET_KEY:}
stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET:}
stripe.currency=${STRIPE_CURRENCY:cad}
app.payment.security-deposit=${APP_SECURITY_DEPOSIT:200.00}
```

| Propiedad | Env Var | Default | Descripcion |
|-----------|---------|---------|-------------|
| `stripe.secret-key` | `STRIPE_SECRET_KEY` | (vacio) | Stripe secret key. Requerida para pagos |
| `stripe.webhook-secret` | `STRIPE_WEBHOOK_SECRET` | (vacio) | Secret para verificar firma de webhooks |
| `stripe.currency` | `STRIPE_CURRENCY` | `cad` | Moneda ISO 4217 |
| `app.payment.security-deposit` | `APP_SECURITY_DEPOSIT` | `200.00` | Monto del security deposit en CAD |

### SecurityConfig
- `/api/stripe/webhook` → `permitAll()` (sin JWT, verificacion por firma Stripe)
- `/api/driver/**` → requiere rol DRIVER
- `/api/bookings/**` → requiere autenticacion (acceso validado en service layer)

### pom.xml
```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>29.4.0</version>
</dependency>
```

---

## Modelo financiero

### Calculo del split (constantes en PaymentServiceImpl)
```
OWNER_PAYOUT_PERCENTAGE = 0.80
PLATFORM_FEE_PERCENTAGE = 0.20
MONEY_SCALE             = 4
CENTS_MULTIPLIER        = 100
```

### Formula
```
Dado:
  baseAmount      = booking.totalPrice  (totalHours * vehicle.hourlyRate)
  securityDeposit = 200.00 CAD          (desde config)

Entonces:
  platformFee  = baseAmount * 0.20   → setScale(4, HALF_UP)
  ownerPayout  = baseAmount * 0.80   → setScale(4, HALF_UP)
  totalCharge  = baseAmount + securityDeposit → setScale(4, HALF_UP)

Ejemplo con booking de 8h a $12.50/h:
  baseAmount      = 100.0000
  securityDeposit = 200.0000
  platformFee     = 20.0000
  ownerPayout     = 80.0000
  totalCharge     = 300.0000
  stripeCents     = 30000  (totalCharge * 100)
```

---

## Flujo completo — Transaction Processor

### Flujo 1: Driver — Crear PaymentIntent y pagar

#### Paso 1: Driver crea PaymentIntent
```
Driver (autenticado con JWT, booking en status CONFIRMED)
  |
  └─ POST /api/driver/bookings/{bookingId}/payments/intent

     → Sistema valida (en orden):
       1. Booking existe (si no → PAY-001)
       2. Booking pertenece al driver autenticado (si no → PAY-002)
       3. Booking.status == CONFIRMED (si no → PAY-003)
       4. Stripe secret key configurada (si no → PAY-004)
       5. Si ya existe Payment para este booking:
          - Si status == COMPLETED → PAY-005 (ya pagado)
          - Si status == PENDING → se reutiliza el registro existente (idempotente)
       6. Si no existe Payment → se crea uno nuevo

     → Calcula montos:
       - baseAmount = normalizeAmount(booking.totalPrice)
       - securityDeposit = normalizeAmount(config.securityDeposit)
       - platformFee = baseAmount * 0.20
       - ownerPayout = baseAmount * 0.80
       - totalCharge = baseAmount + securityDeposit

     → Llama Stripe API:
       - PaymentIntent.create(amount=totalCharge*100, currency="cad", metadata={bookingId})
       - automaticPaymentMethods = enabled

     → Guarda Payment:
       - booking, amount, platformFee, ownerPayout, securityDeposit
       - currency="cad", stripePaymentIntentId, status=PENDING

     → Respuesta: CreatePaymentIntentResponse
       - Incluye clientSecret para Stripe Elements en frontend
```

#### Paso 2: Frontend procesa pago con Stripe Elements
```
Frontend (React)
  |
  └─ Stripe Elements UI
     - Carga publishable key desde VITE_STRIPE_PUBLISHABLE_KEY
     - Renderiza formulario de tarjeta via <PaymentElement>
     - Driver ingresa datos de tarjeta
     - Llama stripe.confirmPayment({ elements, confirmParams })
     - Stripe procesa el cobro (test mode: usa tarjeta 4242 4242 4242 4242)
```

#### Paso 3: Frontend confirma pago en backend
```
Frontend
  |
  └─ PUT /api/driver/bookings/{bookingId}/payments/confirm
     Query: ?paymentIntentId=pi_3ABC123...

     → Sistema valida:
       1. Booking existe (si no → PAY-001)
       2. Booking pertenece al driver (si no → PAY-002)
       3. Payment existe para el booking (si no → PAY-007)
       4. Si Payment.status == COMPLETED → retorna respuesta existente (idempotente)
       5. paymentIntentId coincide con el almacenado (si no → PAY-002)

     → Payment.status = COMPLETED
     → Respuesta: PaymentResponse
```

> **Nota:** Este paso es redundante con el webhook. El frontend lo llama para feedback inmediato en la UI. Si el webhook llega primero, el confirm es un no-op idempotente.

### Flujo 2: Stripe — Webhook asincrono

```
Stripe
  |
  └─ POST /api/stripe/webhook
     Headers: Stripe-Signature: t=...,v1=...
     Body: JSON payload del evento

     → Sistema valida:
       1. Webhook secret configurado (si no → PAY-004)
       2. Firma valida via Webhook.constructEvent() (si no → PAY-008)

     → Deserializa evento y procesa segun tipo:
       - "payment_intent.succeeded":
         → Busca Payment por stripePaymentIntentId
         → Si existe → status = COMPLETED, save
       - "payment_intent.payment_failed":
         → Busca Payment por stripePaymentIntentId
         → Si existe → status = FAILED, save
       - Otros tipos → log.info("Ignoring unsupported event type: {}")

     → Respuesta: 200 "Webhook processed"
```

### Flujo 3: Consultar pago de un booking

```
Driver o Car Owner (autenticado con JWT)
  |
  └─ GET /api/bookings/{bookingId}/payments

     → Sistema valida:
       1. Booking existe (si no → PAY-001)
       2. Usuario es driver del booking O owner del vehiculo (si no → PAY-002)
       3. Payment existe para el booking (si no → PAY-007)

     → Respuesta: PaymentResponse
```

### Flujo 4: Reembolso por cancelacion de booking

```
BookingService (interno — no hay endpoint directo)
  |
  └─ refundPaymentForCancelledBooking(booking)
     Llamado por BookingServiceImpl.cancelBooking() despues de guardar booking como CANCELLED

     → Busca Payment por bookingId
       - Si no existe → no-op (booking no tenia pago)
     → Valida:
       - Payment.status == COMPLETED (si no → no-op)
       - stripePaymentIntentId != null (si no → no-op)
     → Llama Stripe API: Refund.create(paymentIntent=stripePaymentIntentId)
     → Payment.status = REFUNDED
     → Save

     → Si Stripe falla → PAY-010 (PAYMENT_REFUND_FAILED)
```

---

## Frontend — Integracion

### Archivos
- `src/modules/booking/api/bookingPaymentApi.js` — Funciones API (createPaymentIntent, confirmPayment, getBookingPayment)
- `src/modules/booking/components/PaymentModal.jsx` — Modal con Stripe Elements
- `src/modules/booking/pages/DriverBookingDetailPage.jsx` — Pagina de detalle con boton "Pay Now" y seccion de info de pago
- `src/modules/booking/constants/bookingConstants.js` — PAYMENT_STATUS enum, PAYMENT_STATUS_CONFIG, PAYMENT_ERROR_MESSAGES

### PaymentModal — Comportamiento
1. Al montar: llama `createPaymentIntent(bookingId)` → obtiene `clientSecret`
2. Muestra desglose: Rental Total, Security Deposit, Total
3. Renderiza Stripe `<PaymentElement>` con el clientSecret
4. Submit → `stripe.confirmPayment()` → si exito → `confirmPayment()` API → callback `onSuccess`
5. Error de Stripe → muestra mensaje inline
6. Warning si `VITE_STRIPE_PUBLISHABLE_KEY` no esta configurada

### DriverBookingDetailPage — Integracion
- Fetch payment en `fetchBooking()` para bookings CONFIRMED/IN_PROGRESS/COMPLETED
- Muestra seccion "Payment Information" si existe payment (status, amount, deposit, fee)
- Muestra boton "Pay Now" solo si status=CONFIRMED AND no existe payment aun
- `onSuccess`: cierra modal, toast "Payment successful", refetch booking

---

## Repository

### PaymentRepository
**Package:** `com.turbo.payment.repository`
**Extiende:** `JpaRepository<Payment, Long>`

| Metodo | Descripcion |
|--------|-------------|
| `findByBookingBookingId(Long bookingId)` | Busca payment por booking ID (derived query) |
| `findByStripePaymentIntentId(String id)` | Busca payment por Stripe PI ID (para webhooks) |

---

## Mappers

### PaymentControllerMapper (MapStruct)
**Package:** `com.turbo.payment.controller.mapper`

| Metodo | Input | Output |
|--------|-------|--------|
| `toCreatePaymentIntentCommand` | bookingId, userId | CreatePaymentIntentCommand |
| `toConfirmPaymentCommand` | bookingId, userId, paymentIntentId | ConfirmPaymentCommand |
| `toGetPaymentCommand` | bookingId, userId | GetPaymentCommand |
| `toHandleWebhookCommand` | payload, signatureHeader | HandleWebhookCommand |

### PaymentServiceMapper (MapStruct)
**Package:** `com.turbo.payment.service.mapper`

| Metodo | Input | Output |
|--------|-------|--------|
| `toCreatePaymentIntentResponse` | Payment, paymentIntentId, clientSecret | CreatePaymentIntentResponse |
| `toPaymentResponse` | Payment | PaymentResponse (booking.bookingId → bookingId) |

---

## Commands

| Command | Campos | Usado por |
|---------|--------|-----------|
| `CreatePaymentIntentCommand` | bookingId, userId | createPaymentIntent() |
| `ConfirmPaymentCommand` | bookingId, userId, paymentIntentId | confirmPayment() |
| `GetPaymentCommand` | bookingId, userId | getBookingPayment() |
| `HandleWebhookCommand` | payload, stripeSignatureHeader | handleWebhookEvent() |
