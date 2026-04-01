# Module 5 — Frontend Test Cases (Transaction Processor)

Casos de prueba manuales para validar el flujo completo de pagos desde el frontend.

---

## Usuarios de prueba

| Usuario | Role | Password | Descripcion |
|---------|------|----------|-------------|
| driver@turbo.com | DRIVER | driver123 | Driver verificado con bookings activos |
| owner@turbo.com | CAR_OWNER | owner123 | Car owner con vehiculos y bookings |
| admin@turbo.com | ADMIN | admin123 | Admin (no interactua con pagos) |

> **Prerequisito:** Debe existir al menos un booking en status CONFIRMED sin pago asociado. Stripe test mode debe estar configurado (STRIPE_SECRET_KEY en backend, VITE_STRIPE_PUBLISHABLE_KEY en frontend).

> **Tarjeta de prueba Stripe:** `4242 4242 4242 4242`, cualquier fecha futura, cualquier CVC, cualquier codigo postal.

---

## A. Driver — Payment Intent Creation

### A.1 Boton "Pay Now" visible solo en bookings CONFIRMED sin pago
- [ ] Iniciar sesion como driver
- [ ] Navegar a `/driver/bookings` → tab "Confirmed"
- [ ] Click en un booking CONFIRMED que NO tenga pago previo
- [ ] Verificar que aparece el boton "Pay Now" en la seccion de acciones
- [ ] Verificar que tambien aparecen "Start Shift" y "Cancel Booking"

### A.2 Boton "Pay Now" NO aparece en otros estados
- [ ] Navegar a un booking PENDING → verificar que NO hay boton "Pay Now"
- [ ] Navegar a un booking COMPLETED → verificar que NO hay boton "Pay Now"
- [ ] Navegar a un booking CANCELLED → verificar que NO hay boton "Pay Now"
- [ ] Navegar a un booking REJECTED → verificar que NO hay boton "Pay Now"

### A.3 Boton "Pay Now" NO aparece si ya hay pago
- [ ] Navegar a un booking CONFIRMED que YA tiene pago COMPLETED
- [ ] Verificar que el boton "Pay Now" NO aparece
- [ ] Verificar que la seccion "Payment" muestra status "Paid"

---

## B. Driver — Payment Modal

### B.1 Modal se abre con desglose de pago
- [ ] Click en "Pay Now" desde un booking CONFIRMED
- [ ] Verificar que se abre el modal con titulo "Payment"
- [ ] Verificar que muestra el desglose:
  - "Rental Total" = totalPrice del booking (ej: $200.00 CAD)
  - "Security Deposit" = $200.00 CAD
  - "Total" = Rental + Deposit (ej: $400.00 CAD, en naranja)
- [ ] Verificar que los montos coinciden con lo que muestra el detalle del booking

### B.2 Modal muestra formulario de Stripe
- [ ] Verificar que debajo del desglose aparece el formulario de tarjeta de Stripe
- [ ] Verificar que hay campos para: numero de tarjeta, fecha de expiracion, CVC
- [ ] Verificar que hay botones "Pay Now" y "Cancel"
- [ ] Verificar que el boton "Pay Now" del modal esta deshabilitado hasta que Stripe valida los campos

### B.3 Modal se cierra con boton X
- [ ] Click en el icono X en la esquina superior derecha del modal
- [ ] Verificar que el modal se cierra
- [ ] Verificar que se vuelve a ver la pagina de detalle del booking

### B.4 Modal se cierra con boton Cancel
- [ ] Click en "Cancel" en el formulario de pago
- [ ] Verificar que el modal se cierra sin procesar pago

### B.5 Stripe no configurado — muestra advertencia
- [ ] Si VITE_STRIPE_PUBLISHABLE_KEY no esta seteada en el frontend
- [ ] Click en "Pay Now"
- [ ] Verificar que el modal muestra: "Payment processing is not configured. Please set the VITE_STRIPE_PUBLISHABLE_KEY environment variable."
- [ ] Verificar que hay boton "Close" para cerrar

---

## C. Driver — Payment Processing (Happy Path)

### C.1 Pago exitoso con tarjeta de prueba
- [ ] Abrir el modal de pago desde un booking CONFIRMED
- [ ] Esperar que cargue el formulario de Stripe
- [ ] Ingresar tarjeta de prueba: `4242 4242 4242 4242`
- [ ] Ingresar fecha futura: `12/30`
- [ ] Ingresar CVC: `123`
- [ ] Ingresar codigo postal: `V5K 0A1`
- [ ] Click en "Pay Now"
- [ ] Verificar que el boton cambia a "Processing..." con spinner
- [ ] Verificar que aparece "Payment Successful!" con icono verde
- [ ] Verificar que aparece "Redirecting..."
- [ ] Verificar que el modal se cierra automaticamente (~1.5 segundos)
- [ ] Verificar que aparece toast "Payment successful"
- [ ] Verificar que la pagina de detalle ahora muestra seccion "Payment" con status "Paid"
- [ ] Verificar que el boton "Pay Now" ya NO aparece

### C.2 Verificar desglose post-pago
- [ ] Despues de un pago exitoso, en la seccion "Payment":
  - Status: "Paid" (verde)
  - Amount: monto total cobrado (ej: $400.00 CAD)
  - Security Deposit: $200.00
  - Platform Fee: 20% del rental (ej: $40.00)

---

## D. Driver — Payment Error Paths

### D.1 Tarjeta declinada
- [ ] Abrir modal de pago
- [ ] Ingresar tarjeta de prueba que falla: `4000 0000 0000 0002`
- [ ] Completar fecha, CVC, postal
- [ ] Click en "Pay Now"
- [ ] Verificar que aparece mensaje de error en rojo: "Your card was declined."
- [ ] Verificar que el boton "Pay Now" vuelve a estar habilitado (puede reintentar)
- [ ] Verificar que el boton "Cancel" sigue disponible

### D.2 Tarjeta con fondos insuficientes
- [ ] Ingresar tarjeta: `4000 0000 0000 9995`
- [ ] Click en "Pay Now"
- [ ] Verificar que aparece error de Stripe indicando fondos insuficientes

### D.3 Error de red al crear payment intent
- [ ] Desconectar el backend (matar el proceso en puerto 8080)
- [ ] Click en "Pay Now"
- [ ] Verificar que el modal muestra un mensaje de error (no se queda en loading infinito)
- [ ] Verificar que hay boton "Close" para cerrar el modal

---

## E. Owner — Payment Visibility

### E.1 Owner ve informacion de pago en sus bookings
- [ ] Iniciar sesion como owner
- [ ] Navegar a `/owner/bookings`
- [ ] Click en un booking que tenga pago asociado
- [ ] Verificar que la seccion "Payment" muestra: status, amount, security deposit, platform fee

### E.2 Owner NO ve boton "Pay Now"
- [ ] En cualquier booking del owner (CONFIRMED, COMPLETED, etc.)
- [ ] Verificar que NUNCA aparece el boton "Pay Now"
- [ ] Solo el driver puede pagar

---

## F. Payment Status Display

### F.1 Status "Payment Pending" (amber)
- [ ] Si el driver creo un payment intent pero no completo el pago
- [ ] Verificar que la seccion de payment muestra "Payment Pending" en amarillo/amber

### F.2 Status "Paid" (green)
- [ ] Despues de un pago exitoso
- [ ] Verificar que muestra "Paid" en verde

### F.3 Status "Payment Failed" (red)
- [ ] Si Stripe reporto un fallo via webhook
- [ ] Verificar que muestra "Payment Failed" en rojo

### F.4 Status "Refunded" (gray)
- [ ] Si un booking con pago COMPLETED fue cancelado
- [ ] Verificar que muestra "Refunded" en gris

---

## G. Refund on Cancellation

### G.1 Cancelar booking con pago COMPLETED genera refund
- [ ] Tener un booking CONFIRMED con pago COMPLETED
- [ ] Click en "Cancel Booking"
- [ ] Ingresar razon de cancelacion
- [ ] Confirmar cancelacion
- [ ] Verificar que el booking pasa a CANCELLED
- [ ] Verificar que el payment status cambia a "Refunded"

### G.2 Cancelar booking sin pago no genera error
- [ ] Tener un booking PENDING o CONFIRMED sin pago
- [ ] Cancelar el booking
- [ ] Verificar que la cancelacion funciona normalmente sin errores de payment

---

## H. Seguridad

### H.1 Endpoint de payment requiere autenticacion
- [ ] Cerrar sesion
- [ ] Intentar acceder directamente a `/driver/bookings/{id}` sin token
- [ ] Verificar que redirige a login

### H.2 Owner no puede acceder a endpoints de driver
- [ ] Iniciar sesion como owner
- [ ] Intentar navegar a `/driver/bookings`
- [ ] Verificar que no tiene acceso (redirige o muestra error)
