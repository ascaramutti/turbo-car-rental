# Module 2 — Frontend Test Cases (Document Verification)

Casos de prueba manuales para validar el flujo completo del modulo de verificacion de documentos desde el frontend.

---

## Usuarios de prueba

| Usuario | Role | Password | Descripcion |
|---------|------|----------|-------------|
| demo (driver) | DRIVER | (ver seed data) | Driver con documentos existentes |
| admin | ADMIN | (ver seed data) | Administrador para revisar documentos |

---

## A. Driver — Upload de documentos

### A.1 Pagina de documentos carga correctamente
- [ x] Iniciar sesion como driver
- [ x] Navegar a `/driver/documents`
- [ x] Verificar que aparece titulo "Document Verification"
- [ x] Verificar que aparecen las 2 cards: Driver's License y Study Permit
- [ x] Verificar que aparece el banner "Verification Required"
- [ x] Verificar texto de ayuda: formatos aceptados (PDF, JPG, PNG) y limite de tamanio

### A.2 Upload exitoso — Driver's License (PDF)
- [ x] En la card "Driver's License", hacer click en "Select File"
- [ x] Seleccionar un archivo PDF valido (< 5MB)
- [ x] Verificar que aparece toast de exito
- [ x] Verificar que la card cambia a estado "Under Review" (PENDING)
- [ x] Verificar que aparece el nombre del archivo subido
- [ x] Verificar que aparecen los botones "View Document", "Download", y "Replace File"

### A.3 Upload exitoso — Study Permit (JPG/PNG)
- [ x] En la card "Study Permit", hacer click en "Select File"
- [ x] Seleccionar un archivo JPG o PNG valido (< 5MB)
- [ x] Verificar que aparece toast de exito
- [ x] Verificar que la card cambia a estado "Under Review"

### A.4 Upload fallido — Formato invalido
- [ x] Intentar subir un archivo .txt, .docx, .zip, etc.
- [ x] Verificar que aparece mensaje de error de formato invalido (DOC-001)
- [ x] Verificar que la card NO cambia de estado

### A.5 Upload fallido — Archivo excede 5MB
- [ x] Intentar subir un archivo mayor a 5MB
- [ x] Verificar que aparece mensaje de error de tamanio (DOC-002)
- [ x] Verificar que la card NO cambia de estado

---

## B. Driver — Visualizacion de documentos

### B.1 Ver documento PDF en modal
- [ x] Tener un documento subido (ej: license.pdf)
- [ x] Hacer click en "View Document"
- [ x] Verificar que se abre el FilePreviewModal
- [ x] Verificar que muestra el nombre del archivo en el header
- [ x] Verificar que el PDF se renderiza correctamente (no pantalla en blanco)
- [ x] Verificar que el fondo del area de contenido es gris (para diferenciar del PDF)
- [ x] Verificar que el PDF tiene sombra visible
- [ x] Verificar que se puede hacer scroll completo del PDF (parte superior no cortada)
- [ x] Verificar que NO aparecen warnings en consola (standardFontDataUrl, Options prop)

### B.2 Ver documento imagen en modal
- [ x] Tener un documento de imagen subido (ej: permit.jpg)
- [ x] Hacer click en "View Document"
- [x ] Verificar que la imagen se renderiza centrada en el modal
- [ x] Verificar que la imagen no se desborda del modal

### B.3 Paginacion de PDF multi-pagina
- [ x] Subir un PDF de multiples paginas
- [ x] Abrir el modal de preview
- [ x] Verificar que aparece "Page 1 / N" en el header
- [ x] Verificar que aparecen controles de paginacion en el footer
- [ x] Hacer click en "siguiente" y verificar que cambia a pagina 2
- [ x] Verificar que el boton "anterior" esta deshabilitado en pagina 1
- [ x] Verificar que el boton "siguiente" esta deshabilitado en ultima pagina

### B.4 Cerrar modal de preview
- [ x] Abrir el modal de preview
- [ x] Hacer click en la X del header
- [ x] Verificar que el modal se cierra correctamente

### B.5 Descargar documento
- [ x] Hacer click en "Download" en una card de documento subido
- [ x] Verificar que el archivo se descarga con el nombre correcto
- [ x] Verificar que el spinner de descarga aparece y desaparece

---

## C. Driver — Re-upload de documentos

### C.1 Reemplazar documento PENDING
- [ x] Tener un documento en estado PENDING
- [ x] Verificar que aparece el boton "Replace File"
- [ x] Hacer click en "Replace File" y seleccionar nuevo archivo
- [ x] Verificar toast de exito
- [ x] Verificar que el documento sigue en PENDING
- [ x] Verificar que el nombre del archivo actualizado

### C.2 Reemplazar documento REJECTED
- [ x] Tener un documento en estado REJECTED (rechazado por admin)
- [ x] Verificar que aparece la razon de rechazo en la card
- [ x] Verificar que aparece el boton "Replace File"
- [ x] Hacer click en "Replace File" y seleccionar nuevo archivo
- [ x] Verificar que el estado vuelve a PENDING
- [ x] Verificar que la razon de rechazo desaparece

### C.3 No se puede reemplazar documento APPROVED
- [ x] Tener un documento en estado APPROVED
- [ x] Verificar que NO aparece el boton "Replace File"

---

## D. Driver — Banner de verificacion

### D.1 Sin documentos subidos
- [ x] No tener ningun documento subido
- [ x] Verificar banner: "Verification Required" con texto "Upload your documents below"

### D.2 Documentos pendientes
- [ x] Tener 1 o 2 documentos en PENDING
- [ x] Verificar banner: "Verification Required" con texto "X document(s) still need approval"

### D.3 Mezcla aprobado + pendiente
- [ x] Tener 1 APPROVED y 1 PENDING
- [ x] Verificar banner: "Verification Required" con texto "1 document(s) still need approval"

### D.4 Todos aprobados
- [ x] Tener ambos documentos APPROVED
- [ x] Verificar banner: "Account Verified" (verde)
- [ x] Verificar texto: "All documents approved. You can now browse and book vehicles."

---

## E. Admin — Portal de verificacion

### E.1 Pagina de documentos pendientes carga correctamente
- [ x] Iniciar sesion como admin
- [ x] Navegar a `/admin/documents`
- [ x] Verificar que aparece titulo "Verification Portal"
- [ x] Verificar subtitulo "Review and verify driver documents"

### E.2 Lista de documentos pendientes
- [ x] Verificar que aparecen todos los documentos con status PENDING
- [ x] Verificar que cada card muestra: nombre del usuario, User ID, tipo de documento
- [ x] Verificar que cada card muestra: nombre del archivo, fecha de upload
- [ x] Verificar que aparece badge con la cantidad de documentos pendientes

### E.3 Estado vacio — sin documentos pendientes
- [ x] Aprobar/rechazar todos los documentos
- [ x] Verificar que aparece: "No pending documents" con mensaje "All documents have been reviewed"

### E.4 Ver documento desde admin
- [ x] Hacer click en "View" en una card de documento
- [ x] Verificar que se abre el FilePreviewModal
- [ x] Verificar que el PDF/imagen se renderiza correctamente
- [ x] Cerrar el modal

### E.5 Descargar documento desde admin
- [ x] Hacer click en "Download" en una card de documento
- [ x] Verificar que el archivo se descarga correctamente

---

## F. Admin — Aprobacion de documentos

### F.1 Aprobar Driver's License con Class 4
- [ x] En la card de DRIVERS_LICENSE, verificar que aparece "License Classification"
- [ x] Verificar que Class 4 esta seleccionado por defecto
- [ x] Hacer click en "Approve"
- [ x] Verificar toast de exito: "Driver's License approved"
- [ x] Verificar que la card desaparece de la lista de pendientes
- [ x] Verificar que el badge de pendientes se actualiza

### F.2 Aprobar Driver's License con Class 5
- [ x] En la card de DRIVERS_LICENSE, seleccionar "Class 5"
- [ x] Verificar que el boton de Class 5 se activa (azul) y Class 4 se desactiva
- [ x] Hacer click en "Approve"
- [ x] Verificar toast de exito

### F.3 Aprobar Study Permit
- [ x] En la card de STUDY_PERMIT, verificar que NO aparece "License Classification"
- [ x] Hacer click en "Approve"
- [ x] Verificar toast de exito: "Study Permit approved"

### F.4 Verificacion completa del driver
- [ x] Aprobar ambos documentos (DRIVERS_LICENSE + STUDY_PERMIT)
- [ x] Iniciar sesion como el driver
- [ x] Verificar que el banner cambia a "Account Verified"

---

## G. Admin — Rechazo de documentos

### G.1 Abrir formulario de rechazo
- [ x] Hacer click en "Reject" en una card
- [ x] Verificar que aparece el textarea para la razon de rechazo
- [ x] Verificar que aparecen botones "Confirm Rejection" y "Cancel"
- [ x] Verificar que los botones "Approve" y "Reject" originales desaparecen

### G.2 Rechazar con razon valida
- [ x] Escribir una razon de rechazo (ej: "Document is blurry and unreadable")
- [ x] Verificar el contador de caracteres (X/500)
- [ x] Hacer click en "Confirm Rejection"
- [ x] Verificar toast de exito
- [ x] Verificar que la card desaparece de la lista

### G.3 Rechazar sin razon — error
- [ x] Abrir formulario de rechazo
- [ x] Dejar el textarea vacio o con solo espacios
- [ x] Verificar que el boton "Confirm Rejection" esta deshabilitado
- [ x] Si se intenta enviar: verificar toast de error "Please provide a rejection reason"

### G.4 Cancelar rechazo
- [ x] Abrir formulario de rechazo
- [ x] Escribir algo en el textarea
- [ x] Hacer click en "Cancel"
- [ x] Verificar que vuelve a los botones originales (Approve/Reject)
- [ x] Verificar que el textarea se limpia

### G.5 Driver ve el rechazo
- [ x] Despues de que el admin rechaza un documento
- [ x] Iniciar sesion como el driver
- [ x] Verificar que la card muestra status "Rejected" (rojo)
- [ x] Verificar que aparece la razon de rechazo
- [ x] Verificar que aparece el boton "Replace File" para re-subir

### G.6 Limite de caracteres en razon de rechazo
- [ x] Abrir formulario de rechazo
- [ x] Escribir mas de 500 caracteres
- [ x] Verificar que el textarea no permite mas de 500 caracteres
- [ x] Verificar que el contador muestra "500/500"

---

## H. Admin — Ver todos los documentos de un usuario

### H.1 Abrir modal de documentos por usuario
- [ x] Hacer click en "View all documents for [nombre]" en una card
- [ x] Verificar que se abre el UserDocumentsModal
- [ x] Verificar que muestra el nombre del usuario y su User ID

### H.2 Contenido del modal
- [ x] Verificar que aparecen todos los documentos del usuario (PENDING, APPROVED, REJECTED)
- [ x] Verificar que cada documento muestra: tipo, nombre de archivo, fecha de upload
- [ x] Verificar colores por status: amarillo (PENDING), verde (APPROVED), rojo (REJECTED)
- [ x] Verificar iconos por status: reloj (PENDING), check (APPROVED), X (REJECTED)
- [ x] Verificar que documentos revisados muestran la fecha de revision
- [ x] Verificar que documentos rechazados muestran la razon

### H.3 Ver y descargar desde el modal
- [ x] Hacer click en "View" en un documento del modal
- [ x] Verificar que se abre el FilePreviewModal encima del UserDocumentsModal
- [ x] Cerrar el FilePreviewModal y verificar que el UserDocumentsModal sigue abierto
- [ x] Hacer click en "Download" y verificar que descarga correctamente

### H.4 Cerrar modal
- [ x] Hacer click en la X del header
- [ x] Verificar que el modal se cierra

---

## I. Validaciones visibles desde frontend

Errores que se pueden reproducir directamente desde la UI.

| Codigo | Escenario | Como reproducir | Verificar |
|--------|-----------|-----------------|-----------|
| DOC-001 | Formato invalido | Subir un .txt, .docx, .zip, etc. | Toast de error antes de enviar al backend (validacion frontend) |
| DOC-002 | Archivo > 5MB | Subir un archivo mayor a 5MB | Toast de error antes de enviar al backend (validacion frontend) |
| DOC-010 | Admin revisa documento ya procesado | Admin A y Admin B abren la misma lista. Admin A aprueba. Admin B intenta aprobar el mismo doc. | Toast de error del backend |

---

## J. Flujo completo end-to-end

### J.1 Flujo feliz completo
1. [ x] Login como driver
2. [ x] Ir a `/driver/documents` — ver banner "Verification Required"
3. [ x] Subir Driver's License (PDF)
4. [ x] Subir Study Permit (JPG)
5. [ x] Verificar ambas cards en "Under Review"
6. [ x] Logout
7. [ x] Login como admin
8. [ x] Ir a `/admin/documents` — ver 2 documentos pendientes
9. [ ]x View Document del license — verificar PDF se renderiza en modal
10. [ x] Aprobar license con Class 4
11. [ x] Aprobar study permit
12. [ x] Verificar lista queda vacia: "No pending documents"
13. [ x] Logout
14. [ x] Login como driver
15. [ x] Verificar banner: "Account Verified" (verde)
16. [ x] Verificar ambos documentos en estado APPROVED

### J.2 Flujo con rechazo y re-upload
1. [ x] Login como driver y subir license
2. [ x] Login como admin
3. [ x] Rechazar license con razon "Document is blurry"
4. [ x] Login como driver
5. [ x] Verificar card REJECTED con razon "Document is blurry"
6. [ x] Hacer click en "Replace File" y subir nuevo archivo
7. [ x] Verificar que vuelve a PENDING
8. [ x] Login como admin
9. [ x] Verificar que el documento re-aparece en la lista de pendientes
10. [ x] Aprobar el documento
11. [ x] Login como driver — verificar APPROVED
