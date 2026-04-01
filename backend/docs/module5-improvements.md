# Module 5 — Mejoras Futuras

Findings identificados durante el desarrollo y testing del modulo de pagos.

---

## 1. Auto-cancel de bookings CONFIRMED con start window expirada

**Modulo afectado:** Module 4 (Booking) + Module 5 (Payment)

**Problema actual:** Cuando un booking esta CONFIRMED y el driver no hace pickup dentro de la ventana permitida (startTime + 1h), el booking queda en status CONFIRMED indefinidamente. Si el driver intenta iniciar el shift, recibe BOOK-018 ("Booking start window has expired"), pero el booking nunca transiciona automaticamente.

**Impacto:**
- El car owner tiene su vehiculo "reservado" sin uso
- Si el driver ya pago, el dinero queda retenido sin resolucion
- No hay penalidad ni consecuencia para el driver no-show

**Solucion propuesta:**
- Scheduled task (cron) que busque bookings CONFIRMED donde `startTime + 1h < now()`
- Auto-transicionar a nuevo status `NO_SHOW` o reusar `CANCELLED` con `cancelledBy=SYSTEM`
- Logica financiera (requiere decision de negocio del equipo):
  - **Security deposit:** retener como penalidad al driver (no-show)
  - **Rental amount:** devolver al driver (no uso el vehiculo)
  - **Owner compensation:** pagar un porcentaje del rental al owner (ej: 25-50%) como compensacion por la reserva perdida
  - **Platform fee:** retener (Turbo proceso la transaccion)
- Si no hay pago asociado: auto-cancel simple sin movimiento financiero

**Prioridad:** Media — requiere definicion de politica de negocio por el equipo (CEO/CFO)

---

## 2. Earnings Dashboard para Car Owners

**Origen:** Progress Report 1 — "A view for owners to track rental history, real-time revenue, and pending payouts"

**Status:** No implementado en Module 5 MVP

**Requiere:** Endpoint GET /api/owner/earnings con total earnings, pending payouts, transaction history

---

## 3. Admin Payout Management + Owner Bank Account Registration

**Origen:** Progress Report 1 — "A control panel for Admins to audit trips and trigger Completed payment transfers to Car Owners" + "Direct Deposit Payment — directly sent to their selected bank account"

**Status:** No implementado en Module 5 MVP

**Estado actual:**
- `CarOwner` tiene campos `bankAccountNumber` y `bankName` en el modelo pero nunca se capturan (no hay formulario ni endpoint)
- El dinero del driver entra a la cuenta Stripe de Turbo y se queda ahi
- `ownerPayout` se calcula y guarda pero no se transfiere al owner
- La pagina de Earnings muestra cuanto le toca al owner, pero no hay mecanismo de cobro

**Solucion en dos fases:**
1. **Admin Manual Payout (scope curso):** Endpoint `POST /api/admin/payouts/{paymentId}/release` que marca un payment como "paid out". Admin ejecuta manualmente. Tracking interno sin transferencia bancaria real.
2. **Stripe Connect (futuro):** Owner registra cuenta bancaria via Stripe Connect onboarding. Turbo hace `Transfer.create()` automatico al connected account del owner. Funciona en test mode con cuentas bancarias fake de Stripe.

---

## 4. Security Deposit como Pre-Auth Hold separado

**Origen:** Progress Report 1 / SD6 — "placePreAuthHold(securityDeposit)"

**Status actual:** El security deposit se cobra junto al rental en un solo charge

**Mejora:** Usar Stripe `capture_method=manual` para hacer un hold separado del deposit, que se libera al completar el shift sin danos

---

## 5. Email Receipt post-pago

**Origen:** SD6 step 11 — "sendReceipt(driver, bookingID)"

**Status:** No implementado

**Requiere:** Integrar EmailService para enviar confirmacion de pago al driver

---

## 6. Gemini Security Findings (infraestructura compartida)

- **CRITICAL:** Externalizar `jwt.secret` a variable de entorno (actualmente hardcoded en application.properties)
- **CRITICAL:** Externalizar DB credentials a variables de entorno
- **MEDIUM:** Rate limiting en endpoint `/api/stripe/webhook`
