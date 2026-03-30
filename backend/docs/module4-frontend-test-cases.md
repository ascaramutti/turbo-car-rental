# Module 4 — Frontend Test Cases (Booking Orchestrator)

Casos de prueba manuales para validar el flujo completo del modulo de booking desde el frontend.

---

## Usuarios de prueba

| Usuario | Role | Password | Descripcion |
|---------|------|----------|-------------|
| driver@turbo.com | DRIVER | (ver seed data) | Driver verificado (isVerified=true) con licencia CLASS_4 o CLASS_5 |
| owner@turbo.com | CAR_OWNER | (ver seed data) | Car owner con vehiculos APPROVED y activos |

> **Prerequisito:** El driver debe estar verificado (ambos documentos APPROVED) para poder crear bookings. Los vehiculos del owner deben estar en status APPROVED con isActive=true para aparecer en busquedas.

---

## A. Driver — Vehicle Search

### A.1 Pagina de busqueda carga correctamente
- [ ] Iniciar sesion como driver (verificado)
- [ ] Navegar a `/driver/search`
- [ ] Verificar que aparece el formulario de busqueda
- [ ] Verificar que los filtros opcionales estan presentes: Category, Fuel Type, Price Range (min/max)
- [ ] Verificar que los campos de fecha/hora Start Time y End Time estan disponibles
- [ ] Verificar que aparece el boton "Search"

### A.2 Busqueda sin filtros muestra todos los vehiculos disponibles
- [ ] En la pagina `/driver/search`, no ingresar ningun filtro
- [ ] Hacer click en "Search"
- [ ] Verificar que aparece una lista de vehiculos disponibles
- [ ] Verificar que todos los vehiculos mostrados tienen status APPROVED e isActive=true
- [ ] Verificar que se muestran cards de vehiculos con informacion basica

### A.3 Busqueda con filtro de categoria
- [ ] Seleccionar una categoria en el filtro (ej: SEDAN)
- [ ] Hacer click en "Search"
- [ ] Verificar que todos los vehiculos mostrados son de la categoria seleccionada
- [ ] Cambiar a otra categoria (ej: SUV) y verificar que los resultados cambian

### A.4 Busqueda con filtro de tipo de combustible
- [ ] Seleccionar un tipo de combustible (ej: ELECTRIC)
- [ ] Hacer click en "Search"
- [ ] Verificar que todos los vehiculos mostrados son del tipo de combustible seleccionado

### A.5 Busqueda con filtro de rango de precio
- [ ] Ingresar minPrice (ej: $10) y maxPrice (ej: $25)
- [ ] Hacer click en "Search"
- [ ] Verificar que todos los vehiculos mostrados tienen hourlyRate entre $10 y $25

### A.6 Card de vehiculo muestra make, model, year, hourly rate y ubicacion general
- [ ] Realizar una busqueda con resultados
- [ ] En cada card de vehiculo, verificar que aparece: Make, Model, Year
- [ ] Verificar que aparece el hourly rate (ej: "$20/hr")
- [ ] Verificar que aparece la ubicacion general (ej: "Downtown Vancouver")
- [ ] Verificar que NO aparecen coordenadas exactas (solo ubicacion enmascarada ~1km)

### A.7 Advertencia de service type mostrada cuando aplica
- [ ] Iniciar sesion como driver con licencia CLASS_5
- [ ] Buscar vehiculos
- [ ] Verificar que vehiculos TAXI_AND_DELIVERY muestran advertencia: "Your Class 5 license restricts you to delivery services only"
- [ ] Verificar que vehiculos DELIVERY_ONLY NO muestran advertencia (no hay restriccion adicional)
- [ ] Iniciar sesion como driver con licencia CLASS_4
- [ ] Verificar que vehiculos DELIVERY_ONLY muestran advertencia: "This vehicle is classified for delivery services only"
- [ ] Verificar que vehiculos TAXI_AND_DELIVERY NO muestran advertencia

### A.8 Ubicacion enmascarada en resultados de busqueda
- [ ] Realizar una busqueda con resultados
- [ ] Verificar que las coordenadas mostradas son enmascaradas (redondeadas a ~1km de precision)
- [ ] Verificar que se muestra el nombre del area general (ej: "Downtown Vancouver"), no coordenadas exactas

### A.9 Click en vehiculo navega a la pagina de detalle
- [ ] En los resultados de busqueda, hacer click en la card de un vehiculo
- [ ] Verificar que navega a `/driver/vehicles/{vehicleId}` o a la pagina de detalle del vehiculo
- [ ] Verificar que la pagina de detalle carga correctamente sin errores

---

## B. Driver — Create Booking

### B.1 Pagina de detalle de vehiculo muestra informacion completa
- [ ] Navegar al detalle de un vehiculo (desde busqueda o URL directa)
- [ ] Verificar que aparece: Make, Model, Year, Category, Fuel Type
- [ ] Verificar que aparece el Hourly Rate
- [ ] Verificar que aparece la ubicacion general (enmascarada)
- [ ] Verificar que aparece el nombre del owner y su rating
- [ ] Verificar que se muestra el Service Type del vehiculo
- [ ] Verificar que si aplica una advertencia de service type, esta sea visible

### B.2 Formulario de booking con date/time pickers para startTime y endTime
- [ ] En la pagina de detalle del vehiculo, verificar que aparece el formulario de booking
- [ ] Verificar que hay un picker de fecha/hora para "Start Time"
- [ ] Verificar que hay un picker de fecha/hora para "End Time"
- [ ] Verificar que aparece el boton "Book This Vehicle" (o equivalente)

### B.3 Validacion onBlur: startTime debe ser en el futuro
- [ ] En el formulario de booking, ingresar una fecha/hora pasada en Start Time
- [ ] Hacer blur (click fuera del campo)
- [ ] Verificar que aparece un mensaje de error: "Start time must be in the future" (o equivalente)
- [ ] Verificar que el boton de submit esta deshabilitado o que el error bloquea el envio

### B.4 Validacion onBlur: endTime minimo 4h despues del startTime
- [ ] Ingresar un Start Time valido (futuro)
- [ ] Ingresar un End Time que sea menos de 4h despues del Start Time (ej: solo 2h de diferencia)
- [ ] Hacer blur en el campo End Time
- [ ] Verificar que aparece error: "Minimum booking duration is 4 hours" (o equivalente)
- [ ] Verificar que no permite submit con este valor

### B.5 Validacion onBlur: endTime maximo 24h despues del startTime
- [ ] Ingresar un Start Time valido (futuro)
- [ ] Ingresar un End Time que sea mas de 24h despues del Start Time (ej: 25h de diferencia)
- [ ] Hacer blur en el campo End Time
- [ ] Verificar que aparece error: "Maximum booking duration is 24 hours" (o equivalente)
- [ ] Verificar que no permite submit con este valor

### B.6 Crear booking exitosamente — estado PENDING, toast de confirmacion
- [ ] Ingresar Start Time valido (futuro)
- [ ] Ingresar End Time valido (entre 4h y 24h despues del Start Time)
- [ ] Hacer click en "Book This Vehicle"
- [ ] Verificar que aparece toast de exito (ej: "Booking request submitted successfully")
- [ ] Verificar que el nuevo booking aparece con status PENDING
- [ ] Verificar que se muestra el total de horas y el precio total calculado
- [ ] Verificar que se navega a la pagina del booking o al dashboard de bookings

### B.7 Error: vehiculo no disponible (BOOK-003)
- [ ] Intentar crear un booking para un vehiculo que ha sido desactivado por el owner (isActive=false)
- [ ] Verificar que aparece toast de error: "Vehicle is not currently available for rent" (BOOK-003)

### B.8 Error: conflicto de tiempo (BOOK-008)
- [ ] Intentar crear un booking para un vehiculo que ya tiene un booking CONFIRMED o IN_PROGRESS en el mismo horario
- [ ] Verificar que aparece toast de error: "Vehicle is already booked for the requested time slot" (BOOK-008)

### B.9 Error: excede ventana de disponibilidad (BOOK-004)
- [ ] Intentar crear un booking con endTime mayor al availableUntil del vehiculo
- [ ] Verificar que aparece toast de error: "Booking end time exceeds vehicle availability window" (BOOK-004)
- [ ] Nota: el frontend deberia prevenir esto si muestra el availableUntil en el formulario

### B.10 Error: duracion minima 4h (BOOK-007)
- [ ] Si la validacion onBlur no bloquea completamente, intentar enviar un formulario con duracion menor a 4h
- [ ] Verificar que aparece toast de error del backend: "Minimum booking duration is 4 hours" (BOOK-007)

---

## C. Driver — Booking Management

### C.1 Pagina My Bookings muestra lista con badges de status
- [ ] Navegar a `/driver/bookings`
- [ ] Verificar que aparece la lista de bookings del driver
- [ ] Verificar que cada booking muestra un badge de status con el color correspondiente:
  - PENDING: amarillo/naranja
  - CONFIRMED: verde o azul
  - IN_PROGRESS: azul o morado
  - COMPLETED: verde oscuro o gris
  - CANCELLED: rojo o gris
  - REJECTED: rojo

### C.2 Tabs de filtro por status
- [ ] En la pagina `/driver/bookings`, verificar que aparecen tabs de filtro: All, Pending, Confirmed, In Progress, Completed, Cancelled
- [ ] Hacer click en el tab "Pending" — verificar que solo aparecen bookings PENDING
- [ ] Hacer click en el tab "Confirmed" — verificar que solo aparecen bookings CONFIRMED
- [ ] Hacer click en el tab "All" — verificar que aparecen todos los bookings

### C.3 Pagina de detalle del booking muestra informacion completa
- [ ] Hacer click en un booking de la lista
- [ ] Verificar que navega a `/driver/bookings/{bookingId}`
- [ ] Verificar que aparece: vehiculo (make, model, year), fechas de inicio y fin
- [ ] Verificar que aparece el total de horas y el precio total
- [ ] Verificar que aparece el status actual con su badge
- [ ] Verificar que aparece el nombre del owner
- [ ] Verificar que aparece la ubicacion de pickup (enmascarada si > 2h antes, exacta si <= 2h)

### C.4 Cancelar booking PENDING con razon — estado CANCELLED
- [ ] Tener un booking en estado PENDING
- [ ] En la pagina de detalle, verificar que aparece el boton "Cancel Booking"
- [ ] Hacer click en "Cancel Booking"
- [ ] Verificar que aparece un campo para ingresar la razon de cancelacion
- [ ] Ingresar una razon valida (ej: "Schedule conflict")
- [ ] Confirmar la cancelacion
- [ ] Verificar que aparece toast de exito
- [ ] Verificar que el booking cambia a status CANCELLED
- [ ] Verificar que el badge de status se actualiza a CANCELLED

### C.5 Cancelar booking CONFIRMED con razon — estado CANCELLED
- [ ] Tener un booking en estado CONFIRMED
- [ ] En la pagina de detalle, verificar que aparece el boton "Cancel Booking"
- [ ] Hacer click en "Cancel Booking" e ingresar una razon
- [ ] Confirmar la cancelacion
- [ ] Verificar que el booking cambia a status CANCELLED
- [ ] Verificar toast de exito

### C.6 Validacion de razon onBlur: requerida, max 500 caracteres, patron valido
- [ ] Abrir el formulario de cancelacion
- [ ] Dejar el campo de razon vacio y hacer blur — verificar error "Reason is required" (o equivalente)
- [ ] Ingresar mas de 500 caracteres — verificar que el campo no acepta mas o muestra error
- [ ] Ingresar caracteres especiales no permitidos (ej: `<script>`) — verificar error de patron
- [ ] Patron valido: letras, numeros, espacios, puntuacion basica (.,!?()'+:-/)

### C.7 Visualizacion de ubicacion: enmascarada cuando >2h antes del startTime, exacta cuando <=2h
- [ ] Tener un booking CONFIRMED con startTime a mas de 2h en el futuro
- [ ] En la pagina de detalle, verificar que la ubicacion mostrada es general/enmascarada
- [ ] Verificar que aparece mensaje informativo: "Exact location will be available 2 hours before your booking" (o equivalente)
- [ ] Esperar (o usar seed data) hasta que queden <= 2h para el startTime
- [ ] Verificar que la ubicacion exacta (coordenadas precisas) ahora se muestra
- [ ] Verificar mensaje: "Exact pickup location is now available" (o equivalente)

---

## D. Driver — Shift Lifecycle

### D.1 Boton "Start Shift" visible solo para bookings CONFIRMED
- [ ] Tener un booking CONFIRMED
- [ ] En la pagina de detalle, verificar que aparece el boton "Start Shift" (o "Start Pickup")
- [ ] Verificar que el boton NO aparece para bookings PENDING, IN_PROGRESS, COMPLETED, CANCELLED o REJECTED

### D.2 Iniciar shift: upload de fotos de pickup (1-10) — estado IN_PROGRESS
- [ ] Tener un booking CONFIRMED con startTime dentro de la ventana valida (entre startTime-15min y startTime+1h)
- [ ] Hacer click en "Start Shift"
- [ ] Verificar que aparece un componente de upload de fotos (campo "pickupPhotos", acepta multiples archivos)
- [ ] Seleccionar al menos 1 foto valida (JPG, JPEG o PNG, < 5MB cada una)
- [ ] Verificar que se pueden seleccionar hasta 10 fotos
- [ ] Confirmar el inicio del shift
- [ ] Verificar que aparece toast de exito
- [ ] Verificar que el booking cambia a status IN_PROGRESS
- [ ] Verificar que las fotos de pickup quedan registradas y visibles

### D.3 Boton "Complete Shift" visible solo para bookings IN_PROGRESS
- [ ] Tener un booking IN_PROGRESS
- [ ] En la pagina de detalle, verificar que aparece el boton "Complete Shift" (o "Return Vehicle")
- [ ] Verificar que el boton NO aparece para otros estados

### D.4 Completar shift: upload de fotos de retorno (1-10) — estado COMPLETED
- [ ] Tener un booking IN_PROGRESS
- [ ] Hacer click en "Complete Shift"
- [ ] Verificar que aparece el componente de upload de fotos (campo "returnPhotos", acepta multiples archivos)
- [ ] Seleccionar al menos 1 foto valida (JPG, JPEG o PNG, < 5MB cada una)
- [ ] Verificar que se pueden seleccionar hasta 10 fotos
- [ ] Confirmar la finalizacion del shift
- [ ] Verificar que aparece toast de exito
- [ ] Verificar que el booking cambia a status COMPLETED
- [ ] Verificar que las fotos de retorno quedan registradas y visibles

### D.5 Error: intentar iniciar shift demasiado temprano (BOOK-017)
- [ ] Tener un booking CONFIRMED con startTime a mas de 15 minutos en el futuro
- [ ] Intentar hacer click en "Start Shift" (si el boton esta disponible antes de la ventana)
- [ ] Verificar que aparece toast de error: "Booking cannot be started yet (too early)" (BOOK-017)
- [ ] Nota: el frontend puede deshabilitar el boton hasta 15min antes del startTime como prevencion

### D.6 Error: foto requerida para iniciar shift (BOOK-020)
- [ ] En el flujo de "Start Shift", intentar confirmar sin seleccionar ninguna foto
- [ ] Verificar que el boton de confirmar esta deshabilitado O que aparece error: "At least one pickup photo is required to start the shift" (BOOK-020)

### D.7 Error: foto requerida para completar shift (BOOK-021)
- [ ] En el flujo de "Complete Shift", intentar confirmar sin seleccionar ninguna foto
- [ ] Verificar que el boton de confirmar esta deshabilitado O que aparece error: "At least one return photo is required to complete the shift" (BOOK-021)

### D.8 Error: mas de 10 fotos seleccionadas (BOOK-026)
- [ ] En el flujo de "Start Shift" o "Complete Shift", intentar seleccionar mas de 10 fotos
- [ ] Verificar que el componente de upload previene la seleccion de mas de 10 archivos O que aparece error: "Maximum 10 photos allowed per upload" (BOOK-026)

---

## E. Owner — Booking Management

### E.1 Pagina Owner Bookings muestra bookings de vehiculos propios
- [ ] Iniciar sesion como car owner
- [ ] Navegar a `/owner/bookings`
- [ ] Verificar que aparece la lista de bookings para los vehiculos del owner
- [ ] Verificar que cada booking muestra: nombre del driver, vehiculo, fechas, precio total, status

### E.2 Tabs de filtro por status (owner)
- [ ] En la pagina `/owner/bookings`, verificar que aparecen tabs de filtro: All, Pending, Confirmed, In Progress, Completed, Cancelled
- [ ] Hacer click en "Pending" — verificar que solo aparecen bookings PENDING
- [ ] Hacer click en "All" — verificar que aparecen todos los bookings

### E.3 Booking PENDING muestra botones Confirm y Reject
- [ ] Tener un booking PENDING para uno de los vehiculos del owner
- [ ] En la pagina de detalle del booking (`/owner/bookings/{bookingId}`), verificar que aparecen los botones "Confirm" y "Reject"
- [ ] Verificar que NO aparece el boton "Cancel" en estado PENDING (el owner no puede cancelar PENDING, solo CONFIRMED)

### E.4 Confirmar booking — estado CONFIRMED, toast
- [ ] En la pagina de detalle de un booking PENDING, hacer click en "Confirm"
- [ ] Verificar que aparece confirmacion o dialogo de confirmacion (opcional)
- [ ] Confirmar la accion
- [ ] Verificar que aparece toast de exito (ej: "Booking confirmed successfully")
- [ ] Verificar que el booking cambia a status CONFIRMED
- [ ] Verificar que los botones "Confirm" y "Reject" desaparecen

### E.5 Rechazar booking con razon — estado REJECTED
- [ ] En la pagina de detalle de un booking PENDING, hacer click en "Reject"
- [ ] Verificar que aparece un campo para ingresar la razon de rechazo
- [ ] Ingresar una razon valida (ej: "Vehicle is unavailable for those dates")
- [ ] Confirmar el rechazo
- [ ] Verificar que aparece toast de exito
- [ ] Verificar que el booking cambia a status REJECTED
- [ ] Verificar que la razon de rechazo queda visible en el detalle del booking

### E.6 Cancelar booking CONFIRMED con razon — estado CANCELLED
- [ ] Tener un booking CONFIRMED para uno de los vehiculos del owner
- [ ] En la pagina de detalle, verificar que aparece el boton "Cancel Booking"
- [ ] Hacer click en "Cancel Booking" e ingresar una razon (ej: "Emergency - vehicle needs repairs")
- [ ] Confirmar la cancelacion
- [ ] Verificar que aparece toast de exito
- [ ] Verificar que el booking cambia a status CANCELLED con cancelledBy=OWNER

### E.7 Validacion de razon de cancelacion/rechazo onBlur (owner)
- [ ] Abrir el formulario de rechazo o cancelacion
- [ ] Dejar el campo vacio y hacer blur — verificar error "Reason is required"
- [ ] Ingresar mas de 500 caracteres — verificar que el campo limita o muestra error
- [ ] Ingresar caracteres fuera del patron permitido — verificar error de validacion
- [ ] Verificar que el boton de confirmar esta deshabilitado si la razon es invalida

### E.8 No se muestran botones de accion para estados terminales
- [ ] Ver el detalle de un booking con status COMPLETED
- [ ] Verificar que NO aparecen botones de accion (no Confirm, no Reject, no Cancel)
- [ ] Ver el detalle de un booking con status CANCELLED
- [ ] Verificar que NO aparecen botones de accion
- [ ] Ver el detalle de un booking con status REJECTED
- [ ] Verificar que NO aparecen botones de accion

---

## F. Validaciones Cross-Reference

Errores que se pueden reproducir directamente desde la UI.

| Codigo | Escenario | Como reproducir | Verificar |
|--------|-----------|-----------------|-----------|
| BOOK-001 | Driver no verificado intenta crear booking | Iniciar sesion con driver cuyo isVerified=false e intentar crear booking | Toast de error: "Driver must be verified before creating bookings" |
| BOOK-002 | Vehiculo no aprobado | Intentar crear booking para vehiculo con status != APPROVED (edge case, no deberia aparecer en busqueda) | Toast de error del backend |
| BOOK-003 | Vehiculo inactivo | Intentar crear booking para vehiculo desactivado por owner | Toast de error: "Vehicle is not currently available for rent" |
| BOOK-004 | Excede ventana de disponibilidad | Seleccionar endTime mayor al availableUntil del vehiculo | Toast de error: "Booking end time exceeds vehicle availability window". Idealmente prevenido en frontend |
| BOOK-005 | startTime en el pasado | Ingresar startTime pasado en el formulario | Validacion onBlur previene antes del backend |
| BOOK-006 | endTime antes de startTime | Ingresar endTime anterior al startTime | Validacion onBlur previene antes del backend |
| BOOK-007 | Duracion menor a 4h | Ingresar rango de menos de 4h entre start y end | Validacion onBlur previene. Si llega al backend: toast de error "Minimum booking duration is 4 hours" |
| BOOK-008 | Conflicto de tiempo (vehiculo) | Intentar crear booking en horario ya reservado por otro booking activo | Toast de error: "Vehicle is already booked for the requested time slot" |
| BOOK-009 | Driver reserva su propio vehiculo | Owner con rol dual intenta reservar su propio vehiculo | Toast de error: "You cannot book your own vehicle" |
| BOOK-010 | Driver con booking conflictivo | Intentar crear un segundo booking CONFIRMED en el mismo horario (ya confirmado otro) | Toast de error: "You already have an active booking during this time slot" |
| BOOK-013 | Owner confirma booking no-PENDING | Intentar confirmar un booking que ya fue confirmado, rechazado, etc. | Toast de error del backend: "Only pending bookings can be confirmed" |
| BOOK-014 | Owner rechaza booking no-PENDING | Intentar rechazar un booking ya procesado | Toast de error del backend: "Only pending bookings can be rejected" |
| BOOK-015 | Cancelar booking en estado terminal | Intentar cancelar booking COMPLETED, CANCELLED o REJECTED | Toast de error: "Only pending or confirmed bookings can be cancelled". Prevenido en frontend ocultando el boton |
| BOOK-016 | Iniciar shift en booking no-CONFIRMED | Intentar iniciar shift en booking PENDING o COMPLETED | Toast de error: "Only confirmed bookings can be started". Prevenido ocultando boton "Start Shift" |
| BOOK-017 | Iniciar shift demasiado temprano | Intentar iniciar shift mas de 15 min antes del startTime | Toast de error: "Booking cannot be started yet (too early)" |
| BOOK-018 | Ventana de inicio expirada | Intentar iniciar shift mas de 1h despues del startTime sin haber iniciado | Toast de error: "Booking start window has expired" |
| BOOK-019 | Completar shift no IN_PROGRESS | Intentar completar shift en estado distinto a IN_PROGRESS | Toast de error del backend. Prevenido ocultando boton "Complete Shift" |
| BOOK-020 | Foto de pickup faltante | Confirmar "Start Shift" sin seleccionar ninguna foto | Boton deshabilitado o toast de error: "At least one pickup photo is required to start the shift" |
| BOOK-021 | Foto de retorno faltante | Confirmar "Complete Shift" sin seleccionar ninguna foto | Boton deshabilitado o toast de error: "At least one return photo is required to complete the shift" |
| BOOK-026 | Mas de 10 fotos seleccionadas | Seleccionar mas de 10 archivos en "Start Shift" o "Complete Shift" | Componente de upload limita a 10 archivos o toast de error: "Maximum 10 photos allowed per upload" |
| BOOK-022 | Razon de cancelacion vacia | Enviar cancelacion sin ingresar razon | Validacion onBlur previene. Toast de error si llega al backend: "Cancellation reason is required" |
| BOOK-023 | Razon de rechazo vacia | Enviar rechazo sin ingresar razon | Validacion onBlur previene. Toast de error si llega al backend: "Rejection reason is required" |
| BOOK-025 | Duracion mayor a 24h | Ingresar rango de mas de 24h entre start y end | Validacion onBlur previene. Si llega al backend: toast de error "Maximum booking duration is 24 hours" |
| DOC-001 | Foto con formato invalido | Subir .txt, .docx, .gif como foto de pickup/return | Toast de error: "Invalid file format" |
| DOC-002 | Foto excede 5MB | Subir archivo de mas de 5MB como foto de pickup/return | Toast de error: "File too large" |

---

## G. End-to-End Flows

### G.1 Flujo feliz — Driver busca, crea booking, Owner confirma, Driver inicia shift, Driver completa shift

1. [ ] Login como driver (verificado)
2. [ ] Navegar a `/driver/search`
3. [ ] Buscar vehiculos disponibles (con o sin filtros)
4. [ ] Seleccionar un vehiculo de los resultados — verificar card con make/model/year/rate/ubicacion
5. [ ] Navegar al detalle del vehiculo — verificar informacion completa
6. [ ] Ingresar Start Time valido (ej: manana a las 08:00) y End Time (ej: manana a las 16:00 = 8h)
7. [ ] Hacer click en "Book This Vehicle" — verificar toast de exito y status PENDING
8. [ ] Navegar a `/driver/bookings` — verificar que el nuevo booking aparece con status PENDING
9. [ ] Logout
10. [ ] Login como car owner
11. [ ] Navegar a `/owner/bookings` — verificar que aparece el booking PENDING
12. [ ] Abrir el detalle del booking — verificar info del driver, vehiculo, fechas, precio
13. [ ] Hacer click en "Confirm" — verificar toast de exito y status CONFIRMED
14. [ ] Logout
15. [ ] Login como driver
16. [ ] Navegar a `/driver/bookings` — verificar que el booking muestra status CONFIRMED
17. [ ] Abrir el detalle — verificar que la ubicacion esta enmascarada (si > 2h antes del startTime)
18. [ ] Cuando llegue la ventana de inicio (startTime - 15min), hacer click en "Start Shift"
19. [ ] Seleccionar al menos 1 foto de pickup valida (JPG/JPEG/PNG, < 5MB) — confirmar
20. [ ] Verificar toast de exito y status IN_PROGRESS
21. [ ] Hacer click en "Complete Shift" — seleccionar al menos 1 foto de retorno valida
22. [ ] Confirmar finalizacion
23. [ ] Verificar toast de exito y status COMPLETED
24. [ ] Verificar que todas las fotos de pickup y return son visibles en el detalle del booking

### G.2 Flujo de rechazo — Driver crea booking, Owner rechaza con razon

1. [ ] Login como driver (verificado)
2. [ ] Buscar vehiculo disponible y crear booking con fechas validas
3. [ ] Verificar que booking queda en PENDING
4. [ ] Logout
5. [ ] Login como car owner
6. [ ] Navegar a `/owner/bookings` — verificar booking PENDING
7. [ ] Abrir el detalle del booking
8. [ ] Hacer click en "Reject"
9. [ ] Ingresar razon de rechazo (ej: "Vehicle is unavailable for those dates")
10. [ ] Confirmar el rechazo
11. [ ] Verificar toast de exito y status REJECTED
12. [ ] Logout
13. [ ] Login como driver
14. [ ] Navegar a `/driver/bookings` — verificar que el booking muestra status REJECTED
15. [ ] Abrir el detalle — verificar que aparece la razon de rechazo ingresada por el owner
16. [ ] Verificar que NO aparecen botones de accion (estado terminal)

### G.3 Flujo de cancelacion por driver — Driver crea booking, Driver cancela con razon

1. [ ] Login como driver (verificado)
2. [ ] Crear un booking en estado PENDING
3. [ ] Navegar al detalle del booking
4. [ ] Hacer click en "Cancel Booking"
5. [ ] Ingresar razon de cancelacion (ej: "Schedule conflict")
6. [ ] Confirmar la cancelacion
7. [ ] Verificar toast de exito y status CANCELLED
8. [ ] Verificar que aparece la razon de cancelacion en el detalle
9. [ ] Verificar que el badge de status muestra CANCELLED
10. [ ] Verificar que NO aparecen botones de accion en el booking cancelado
11. [ ] Repetir el flujo con un booking en estado CONFIRMED para validar cancelacion desde CONFIRMED

### G.4 Flujo de cancelacion por owner — Driver crea, Owner confirma, Owner cancela

1. [ ] Login como driver y crear booking (queda PENDING)
2. [ ] Logout
3. [ ] Login como car owner
4. [ ] Navegar a `/owner/bookings` y confirmar el booking (queda CONFIRMED)
5. [ ] Abrir el detalle del booking CONFIRMED
6. [ ] Hacer click en "Cancel Booking"
7. [ ] Ingresar razon (ej: "Emergency - vehicle needs repairs")
8. [ ] Confirmar la cancelacion
9. [ ] Verificar toast de exito y status CANCELLED
10. [ ] Logout
11. [ ] Login como driver
12. [ ] Navegar a `/driver/bookings` — verificar que el booking muestra status CANCELLED
13. [ ] Verificar que aparece la razon de cancelacion del owner
14. [ ] Verificar que cancelledBy indica que fue cancelado por el OWNER
