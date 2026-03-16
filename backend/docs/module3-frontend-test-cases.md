# Module 3 — Frontend Test Cases (Vehicle Catalog)

Casos de prueba manuales para validar el flujo completo del modulo de catalogo de vehiculos desde el frontend.

---

## Usuarios de prueba

| Usuario | Role | Password | Descripcion |
|---------|------|----------|-------------|
| owner (car owner) | CAR_OWNER | (ver seed data) | Car owner con vehiculos existentes |
| admin@turbo.com | ADMIN | admin123 | Administrador para aprobar documentos y clasificar vehiculos |

---

## A. CarOwner — Vehicle Registration

### A.1 Pagina de vehiculos carga correctamente
- [ x] Iniciar sesion como car owner
- [ x] Navegar a `/owner/vehicles`
- [ x] Verificar que aparece titulo "My Vehicles"
- [ x] Verificar que aparece boton "Register New Vehicle"

### A.2 Abrir formulario de registro
- [ x] En la pagina "My Vehicles", hacer click en "Register New Vehicle"
- [ x] Verificar que se abre el modal con el formulario de registro
- [ x] Verificar que aparecen los campos: VIN, Make, Model, Year, License Plate, Category, Fuel Type, Description
- [ x] Verificar que NO aparecen campos de Hourly Rate ni ubicacion (estos se capturan al activar)

### A.3 Validacion onBlur en todos los campos del formulario
- [ x] Hacer click en el campo VIN, dejarlo vacio, hacer blur — verificar que aparece error de validacion
- [ x] Hacer click en el campo Make, dejarlo vacio, hacer blur — verificar error
- [ x] Hacer click en el campo Model, dejarlo vacio, hacer blur — verificar error
- [ x] Ingresar Year invalido (ej: 1990) y hacer blur — verificar error de validacion
- [ x] Hacer click en el campo License Plate, dejarlo vacio, hacer blur — verificar error
- [ x] Verificar que cada campo valida al perder foco (onBlur), no solo al submit

### A.4 Registrar vehiculo exitosamente
- [ x] Completar todos los campos requeridos con datos validos
- [ x] Hacer click en "Register Vehicle"
- [ x] Verificar que aparece toast de exito
- [ x] Verificar que el modal se cierra
- [ x] Verificar que el nuevo vehiculo aparece en la lista

### A.5 Error por VIN duplicado
- [ x] Intentar registrar un vehiculo con un VIN que ya existe
- [ x] Verificar que aparece toast de error con mensaje de VIN duplicado (VEH-002)

### A.6 Error por ano muy antiguo
- [ x] Intentar ingresar un ano mayor a 20 anos de antiguedad
- [ x] Verificar que la validacion onBlur bloquea el valor (no permite submit)
- [ x] Nota: el error VEH-001 del backend no se alcanza porque el frontend lo previene

### A.7 Error por ano muy en el futuro
- [ x] Intentar ingresar un ano mayor a currentYear+1 (ej: 2028 si estamos en 2026)
- [ x] Verificar que la validacion onBlur bloquea el valor
- [ x] Nota: el error VEH-014 del backend no se alcanza porque el frontend lo previene

### A.8 Vehiculo aparece en la lista despues del registro
- [ x] Despues de registrar un vehiculo exitosamente
- [ x] Verificar que la card del vehiculo muestra: make, model, year, plate
- [ x] Verificar que el hourly rate NO se muestra en la card (es null porque no se ha activado)
- [ x] Verificar que el status es "Pending Docs"

---

## B. CarOwner — Vehicle Detail Page

### B.1 Navegar a la pagina de detalle
- [ x] En la pagina "My Vehicles", hacer click en una card de vehiculo
- [ x] Verificar que navega a `/owner/vehicles/{id}`
- [ x] Verificar que carga correctamente sin errores

### B.2 Muestra informacion del vehiculo
- [ x] Verificar que aparece el titulo con year, make, model (ej: "2022 Toyota Camry")
- [ x] Verificar que aparece la placa
- [ x] Verificar que aparece VIN, Category, Fuel Type
- [ x] Verificar que Location, Coordinates y Hourly Rate NO aparecen o muestran N/A (se setean al activar, no al registrar)
- [ x] Verificar que aparece el badge de status

### B.3 Muestra seccion de documentos
- [ x] Verificar que aparece heading "Vehicle Documents"
- [ x] Verificar que aparece texto de ayuda con formatos aceptados y limite de tamano

### B.4 Editar vehiculo via modal
- [ x] Hacer click en el boton de editar (icono lapiz)
- [ x] Verificar que se abre el modal "Edit Vehicle"
- [ x] Verificar que los campos estan pre-llenados con los datos actuales
- [ x] Verificar que el campo VIN esta deshabilitado (no se puede cambiar)
- [ x] Verificar que el formulario de edicion NO tiene campo Hourly Rate (se establece al activar)
- [ x] Verificar validacion onBlur en todos los campos del modal de edicion
- [ x] Cambiar un campo (ej: description) y hacer click en "Update Vehicle"
- [ x] Verificar toast de exito
- [ x] Verificar que el dato se actualizo en la pagina

---

## C. CarOwner — Vehicle Documents

### C.1 Muestra cards de tipos de documento
- [ x] En la pagina de detalle del vehiculo, ir a la seccion "Vehicle Documents"
- [ x] Verificar que aparecen 3 cards: Insurance Certificate, Vehicle Registration, Inspection Report (CVIP)
- [ x] Verificar que Insurance y Vehicle Registration aparecen como "Required"
- [ x] Verificar que Inspection Report aparece como "Optional — Required for Taxi + Delivery only"

### C.2 Upload de documento exitoso
- [ x] En una card sin documento, hacer click en "Select File"
- [ x] Seleccionar un archivo PDF/JPG/PNG valido (< 5MB)
- [ x] Verificar que aparece toast de exito
- [ x] Verificar que la card cambia a estado "Under Review" (PENDING)
- [ x] Verificar que aparece el nombre del archivo subido
- [ x] Verificar que aparecen los botones "View Document", "Download", "Replace File"

### C.3 Ver documento en modal
- [ x] Tener un documento subido
- [ x] Hacer click en "View Document"
- [ x] Verificar que se abre el FilePreviewModal
- [ x] Verificar que el PDF/imagen se renderiza correctamente
- [ x] Cerrar el modal

### C.4 Descargar documento
- [ x] Hacer click en "Download" en una card de documento subido
- [ x] Verificar que el archivo se descarga con el nombre correcto

### C.5 Reemplazar documento rechazado
- [ x] Tener un documento en estado REJECTED (rechazado por admin)
- [ x] Verificar que aparece la razon de rechazo en la card
- [ x] Verificar que aparece el boton "Replace File"
- [ x] Hacer click en "Replace File" y seleccionar nuevo archivo
- [ x] Verificar que el estado vuelve a PENDING
- [ x] Verificar que la razon de rechazo desaparece

---

## D. CarOwner — Activate / Deactivate

### D.1 No se puede activar vehiculo PENDING
- [ x] Tener un vehiculo con status PENDING (documentos no aprobados)
- [ x] Verificar que aparece mensaje: "Vehicle documents are pending admin approval"
- [ x] Verificar que NO aparece boton "List for Rent"

### D.2 Despues de clasificacion admin: muestra campos de activacion y boton "List for Rent"
- [ x] Tener un vehiculo con status APPROVED y isActive=false
- [ x] Verificar que aparece badge "Not Listed"
- [ x] Verificar que aparece un campo datetime-local para "Available Until"
- [ x] Verificar que aparece un campo para "General Location"
- [ x] Verificar que aparecen campos para "Latitude" y "Longitude"
- [ x] Verificar que aparece un campo para "Hourly Rate"
- [ x] Verificar que aparece boton "List for Rent" (verde)

### D.3 Activar vehiculo con availableUntil, ubicacion y hourlyRate
- [ x] Seleccionar una fecha/hora futura en el campo datetime-local
- [ x] Ingresar la ubicacion: General Location, Latitude, Longitude
- [ x] Ingresar el Hourly Rate (minimo $1.00)
- [ x] Hacer click en "List for Rent"
- [ x] Verificar que aparece toast de exito: "Vehicle is now listed for rent"
- [ x] Verificar que aparece badge "Listed for Rent" (verde)
- [ x] Verificar que aparece texto "Available until: ..." con la fecha seleccionada
- [ x] Verificar que aparece la ubicacion ingresada
- [ x] Verificar que aparece el hourly rate ingresado
- [ x] Verificar que aparece el tipo de servicio asignado por admin

### D.4 No se puede activar sin campos requeridos
- [ x] Con un vehiculo APPROVED y isActive=false
- [ x] Verificar que el boton "List for Rent" esta deshabilitado cuando "Available Until" esta vacio
- [ x] Ingresar solo la fecha — verificar que el boton se habilita
- [ x] Hacer click en "List for Rent" sin ubicacion ni hourly rate — verificar toast de error del backend

### D.6 Desactivar vehiculo
- [ x] Con un vehiculo activo (Listed for Rent), hacer click en "Remove from Rent"
- [ x] Verificar que aparece toast de exito: "Vehicle removed from rent listings"
- [ x] Verificar que aparece badge "Not Listed"
- [ x] Verificar que aparece boton "List for Rent" de nuevo con campos de activacion

### D.7 Re-activar vehiculo
- [ x] Con un vehiculo desactivado (Not Listed) y status APPROVED
- [ x] Seleccionar nueva fecha/hora futura
- [ x] Ingresar ubicacion y hourly rate
- [ x] Hacer click en "List for Rent"
- [ x] Verificar que vuelve a estar activo con badge "Listed for Rent"

---

## E. Admin — Document Review & Vehicle Classification

### E.1 Admin ve documentos de vehiculo en la cola de pendientes
- [ x] Iniciar sesion como admin (admin@turbo.com / admin123)
- [ x] Navegar al Verification Portal
- [ x] Verificar que aparecen documentos de vehiculo junto con documentos de driver
- [ x] Verificar que cada documento de vehiculo muestra su tipo: "Insurance Certificate", "Vehicle Registration", o "Inspection Report (CVIP)"
- [ x] Verificar que aparece el nombre del owner y su user ID

### E.2 Aprobar documento de vehiculo (no el ultimo)
- [ x] Seleccionar un documento de vehiculo (ej: INSURANCE)
- [ x] Hacer click en "Approve"
- [ x] Verificar toast de exito: "Insurance Certificate approved"
- [ x] Verificar que el documento desaparece de la cola de pendientes
- [ x] Verificar que NO aparece modal de clasificacion (aun faltan documentos por aprobar)

### E.3 Aprobar ultimo documento — modal de clasificacion aparece automaticamente
- [ x] Tener un vehiculo con solo 2 documentos subidos (INSURANCE + VEHICLE_REGISTRATION)
- [ x] Aprobar el primero — no aparece modal
- [ x] Aprobar el segundo (ultimo pendiente) — el modal de clasificacion se abre automaticamente
- [ x] Verificar que el modal muestra:
  - Titulo "Classify Vehicle" con subtitulo "All documents approved"
  - Info del vehiculo: year, make, model, vehicle ID
  - Seccion "Approved Documents" con lista de documentos y boton "View" en cada uno
  - Seccion "Service Type" con las opciones disponibles

### E.4 Clasificar vehiculo como DELIVERY_ONLY (sin INSPECTION_REPORT)
- [ x] En el modal de clasificacion de un vehiculo sin INSPECTION_REPORT
- [ x] Verificar que aparece alerta: "No inspection report — only Delivery service available"
- [ x] Verificar que la unica opcion es "Delivery Only" (pre-seleccionada)
- [ x] Hacer click en "Classify Vehicle"
- [ x] Verificar toast de exito: "Vehicle classified as Delivery Only"
- [ x] Verificar que el modal se cierra

### E.5 Clasificar vehiculo con opciones (<=9 anos + INSPECTION aprobado)
- [ ] Tener un vehiculo <=9 anos con 3 documentos subidos (incluyendo INSPECTION_REPORT)
- [ ] Aprobar los 3 documentos — modal aparece al aprobar el 3ro
- [ ] Verificar que aparecen 2 opciones: "Taxi + Delivery" y "Delivery Only"
- [ ] Seleccionar "Taxi + Delivery"
- [ ] Hacer click en "Classify Vehicle"
- [ ] Verificar toast de exito: "Vehicle classified as Taxi + Delivery"
- [ ] Login como car owner — verificar que el vehiculo muestra status APPROVED y service type "Taxi + Delivery"

### E.6 Vehiculo >9 anos con INSPECTION — solo DELIVERY_ONLY
- [ x] Tener un vehiculo >9 anos con 3 documentos subidos (incluyendo INSPECTION_REPORT)
- [ x] Aprobar los 3 documentos — modal aparece al aprobar el 3ro
- [ x] Verificar que aparece alerta: "Vehicle age exceeds taxi eligibility — only Delivery service available"
- [ x] Verificar que la unica opcion es "Delivery Only"
- [ x] Clasificar como "Delivery Only"

### E.7 Ver documentos desde el modal de clasificacion
- [ x] Cuando el modal de clasificacion este abierto
- [ x] Verificar que cada documento muestra icono verde, tipo de documento y boton "View"
- [ x] Hacer click en "View" de un documento
- [ x] Verificar que se abre el FilePreviewModal con el contenido del documento
- [ x] Cerrar el preview y verificar que el modal de clasificacion sigue abierto

### E.8 Reclasificacion despues de agregar tercer documento
- [ x] Tener un vehiculo ya clasificado como DELIVERY_ONLY (con solo 2 docs)
- [ x] Login como car owner — subir INSPECTION_REPORT
- [ x] Login como admin — aprobar el INSPECTION_REPORT
- [ x] Verificar que el modal de clasificacion se abre de nuevo
- [ x] Si vehiculo <=9 anos: verificar que ahora aparece la opcion "Taxi + Delivery"
- [x] Reclasificar como "Taxi + Delivery"
- [ x] Login como car owner — verificar que el service type cambio

### E.9 Rechazo de documento de vehiculo
- [ x] Seleccionar un documento de vehiculo pendiente
- [ x] Hacer click en "Reject"
- [ x] Ingresar razon de rechazo
- [ x] Hacer click en "Confirm Rejection"
- [ x] Verificar toast de exito con nombre del tipo de documento
- [ x] Login como car owner — verificar que el documento muestra status REJECTED con la razon

---

## F. Validaciones visibles desde frontend

Errores que se pueden reproducir directamente desde la UI.

| Codigo | Escenario | Como reproducir | Verificar |
|--------|-----------|-----------------|-----------|
| VEH-001 | Ano muy antiguo | Ingresar ano > 20 anos de antiguedad en formulario | Validacion onBlur bloquea antes de llegar al backend |
| VEH-002 | VIN duplicado | Registrar vehiculo con VIN existente | Toast de error del backend |
| VEH-003 | Placa duplicada | Registrar vehiculo con placa existente | Toast de error del backend |
| VEH-012 | availableUntil en el pasado | Activar vehiculo con fecha pasada | Validacion onBlur: "Available until must be in the future" |
| VEH-013 | Taxi restringido por edad | Vehiculo >9 anos en modal de clasificacion | Modal solo muestra DELIVERY_ONLY (prevenido en frontend) |
| VEH-014 | Ano muy en el futuro | Ingresar ano > currentYear+1 | Validacion onBlur bloquea antes de llegar al backend |
| VEH-015 | Taxi sin inspeccion | Vehiculo sin INSPECTION en modal de clasificacion | Modal solo muestra DELIVERY_ONLY (prevenido en frontend) |

---

## G. Flujo completo end-to-end

### G.1 Flujo feliz — Solo documentos obligatorios (DELIVERY_ONLY)
1. [ x] Login como car owner
2. [ x] Ir a `/owner/vehicles` — ver pagina "My Vehicles"
3. [ x] Registrar un vehiculo nuevo con todos los campos
4. [ x] Navegar al detalle del vehiculo
5. [ x] Subir 2 documentos: Insurance, Vehicle Registration
6. [ x] Verificar las 2 cards en "Under Review"
7. [ x] Logout
8. [ x] Login como admin
9. [ x] Aprobar el 1er documento — no aparece modal
10. [ x] Aprobar el 2do documento — modal de clasificacion aparece
11. [ x] Verificar que solo aparece opcion "Delivery Only" (sin inspection report)
12. [ x] Clasificar como "Delivery Only"
13. [ x] Logout
14. [ x] Login como car owner
15. [ x] Ir al detalle del vehiculo — verificar status APPROVED
16. [ x] Activar vehiculo: ingresar availableUntil, ubicacion, hourlyRate
17. [ x] Hacer click en "List for Rent" — verificar badge "Listed for Rent"
18. [ x] Hacer click en "Remove from Rent" — verificar badge "Not Listed"

### G.2 Flujo feliz — Con documento opcional (TAXI_AND_DELIVERY)
1. [ x] Login como car owner
2. [ x] Registrar vehiculo con ano reciente (<=9 anos de antiguedad)
3. [ x] Subir 3 documentos: Insurance, Vehicle Registration, Inspection Report
4. [ x] Logout
5. [ x] Login como admin
6. [ x] Aprobar los 3 documentos uno por uno
7. [ x] Al aprobar el 3ro: modal de clasificacion aparece con 2 opciones
8. [ x] Seleccionar "Taxi + Delivery"
9. [ x] Clasificar — verificar toast de exito
10. [ x] Logout
11. [ x] Login como car owner
12. [ x] Verificar status APPROVED con service type "Taxi + Delivery"
13. [ x] Activar vehiculo y verificar que funciona correctamente

### G.3 Flujo de reclasificacion
1. [ x] Completar flujo G.1 (vehiculo clasificado como DELIVERY_ONLY con 2 docs)
2. [ x] Login como car owner
3. [ x] Subir INSPECTION_REPORT (3er documento)
4. [ x] Logout
5. [ x] Login como admin
6. [ x] Aprobar el INSPECTION_REPORT — modal de clasificacion aparece
7. [ x] Reclasificar como "Taxi + Delivery" (si vehiculo <=9 anos)
8. [ x] Logout
9. [ x] Login como car owner
10. [ x] Verificar que el service type cambio a "Taxi + Delivery"
