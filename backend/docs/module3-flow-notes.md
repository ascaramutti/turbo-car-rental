# Module 3 — Vehicle Catalog

## Decisiones de diseno

### Scope del modulo
Este modulo maneja el **registro de vehiculos**, **documentos de vehiculo**, **activacion con availableUntil**
y **location masking**. Los documentos del vehiculo reutilizan la entidad `Document` del Modulo 2,
diferenciandose por el campo `vehicle_id` (en lugar de solo `user_id`).

**Justificacion (de los reportes):**
- Assumption 12: *"Each Vehicle must have many Documents (registration, insurance, inspection)"*
- Table Document: tiene `vehicle_id` como FK → docs de vehiculo se vinculan al vehiculo, no al usuario directamente
- El endpoint de revision de Admin (`PUT /api/admin/documents/{id}/review`) se **reutiliza** del Modulo 2

### Document types en este modulo
| Tipo | Quien | Obligatorio | Descripcion |
|------|-------|-------------|-------------|
| INSURANCE | CarOwner | Si (por vehiculo) | Certificado de seguro comercial del vehiculo |
| VEHICLE_REGISTRATION | CarOwner | Si (por vehiculo) | Registro del vehiculo |
| INSPECTION_REPORT | CarOwner | Opcional (requerido para TAXI_AND_DELIVERY) | Reporte de inspeccion mecanica (CVIP) |

Estos tipos se agregan al **enum `DocumentType` existente** del Modulo 2. No se crea un nuevo enum.

### VehicleCategory
| Valor | Descripcion |
|-------|-------------|
| SEDAN | Sedan estandar |
| SUV | Vehiculo utilitario deportivo |
| VAN | Van / minivan |
| TRUCK | Camioneta / pickup |
| COMPACT | Auto compacto |

### FuelType
| Valor | Descripcion |
|-------|-------------|
| GASOLINE | Gasolina |
| DIESEL | Diesel |
| ELECTRIC | Electrico |
| HYBRID | Hibrido |

### VehicleStatus
| Valor | Significado | Condicion |
|-------|-------------|-----------|
| PENDING | Vehiculo registrado, necesita documentos aprobados | Estado inicial al crear vehiculo |
| APPROVED | Todos los documentos aprobados, aprobado por admin | Admin aprueba con service type |
| INACTIVE | Desactivado por el owner (soft delete) | Manual via DELETE del owner |

> **Nota:** Document rejection happens at document level (Module 2). Vehicle only has PENDING and APPROVED.

### Validaciones de vehiculo
- **Ano**: `year` no puede ser mayor a 20 anos de antiguedad (`currentYear - year <= 20`)
- **Restriccion de servicio por edad**: Vehiculos con mas de 9 anos solo pueden ser DELIVERY_ONLY (no TAXI_AND_DELIVERY)
- **VIN**: debe ser unico en la base de datos
- **License plate**: debe ser unica en la base de datos

### Location masking
- En busquedas: coordenadas redondeadas a **~1km de radio** para proteger la ubicacion exacta
- **2 horas antes** del booking: se revela la direccion exacta al driver
- Implementado via `LocationMaskService`

### Vehicle availability (availableUntil)
- Al activar un vehiculo, el owner debe indicar un `availableUntil` datetime (fecha/hora futura)
- Vehicle is available when `isActive=true` AND `availableUntil > now()`
- El frontend valida que `availableUntil` sea una fecha futura antes de llamar al API
- Al desactivar, `isActive` se pone en `false`

---

## Flujo completo — Vehicle Catalog

### Paso 1: CarOwner registra un vehiculo
```
CarOwner (autenticado con JWT, email verificado)
  |
  └─ POST /api/owner/vehicles
     Body: { vin, make, model, year, licensePlate, category, fuelType,
             description }

     → Sistema valida:
       - Ano del vehiculo <= 20 anos de antiguedad (si no → VEH-001)
       - VIN unico (si duplicado → VEH-002)
       - License plate unica (si duplicada → VEH-003)
     → Vehicle.status = PENDING (no activo aun, necesita docs)
     → Vehicle.isActive = false
     → Respuesta: vehicleId + datos del vehiculo
```

### Paso 2: CarOwner consulta sus vehiculos
```
CarOwner
  |
  ├─ GET /api/owner/vehicles
  |   → Lista de todos los vehiculos del owner con su status
  |   → [{vehicle1: PENDING, docs: 0/3}, {vehicle2: ACTIVE, docs: 3/3}]
  |
  └─ GET /api/owner/vehicles/{id}
      → Detalle completo del vehiculo
      → Si el vehiculo no existe → VEH-004
      → Si no pertenece al usuario → VEH-005
```

### Paso 3: CarOwner actualiza info del vehiculo
```
CarOwner
  |
  └─ PUT /api/owner/vehicles/{id}
     Body: { make, model, year, licensePlate, category, fuelType, description, ... }

     → Valida ownership (si no → VEH-005)
     → Valida que el vehiculo exista (si no → VEH-004)
     → Actualiza campos permitidos
     → Respuesta: vehiculo actualizado
```

### Paso 4: CarOwner sube documentos del vehiculo
```
CarOwner
  |
  ├─ POST /api/owner/vehicles/{vehicleId}/documents/upload
  |   (file=insurance.pdf, documentType=INSURANCE)
  |   → Sistema valida: formato (PDF/JPG/JPEG/PNG), tamano (<5MB)
  |   → Si valido: guarda archivo en Docker volume, crea Document(status=PENDING)
  |   → Document.vehicle_id = vehicleId
  |   → Document.user_id = ownerUserId
  |   → Si formato invalido → DOC-001
  |   → Si tamano > 5MB → DOC-002
  |
  ├─ POST /api/owner/vehicles/{vehicleId}/documents/upload
  |   (file=registration.pdf, documentType=VEHICLE_REGISTRATION)
  |   → Mismo proceso
  |
  └─ POST /api/owner/vehicles/{vehicleId}/documents/upload
      (file=inspection.pdf, documentType=INSPECTION_REPORT)
      → Mismo proceso
```

### Paso 5: CarOwner consulta documentos del vehiculo
```
CarOwner
  |
  ├─ GET /api/owner/vehicles/{vehicleId}/documents
  |   → Lista de documentos del vehiculo con su status
  |   → [{INSURANCE: PENDING}, {VEHICLE_REGISTRATION: APPROVED}, ...]
  |
  ├─ GET /api/owner/vehicles/{vehicleId}/documents/{docId}/view
  |   → Visualiza el documento inline (Content-Disposition: inline)
  |
  └─ GET /api/owner/vehicles/{vehicleId}/documents/{docId}/download
      → Descarga el documento (Content-Disposition: attachment)
```

### Paso 6: Admin revisa documentos del vehiculo
```
Reutiliza el mismo endpoint del Modulo 2.
Los documentos de vehiculo aparecen junto con los de driver en la lista de pendientes.

Admin
  |
  ├─ GET /api/admin/documents/pending
  |   → Lista de TODOS los documentos PENDING (driver + vehiculo)
  |   → Cada documento indica si es de usuario (user_id) o vehiculo (vehicle_id)
  |
  ├─ GET /api/admin/documents/{id}/view
  |   → Visualiza el archivo del documento para revisarlo
  |
  └─ PUT /api/admin/documents/{id}/review
     Body: { "action": "APPROVE" }
     (o)
     Body: { "action": "REJECT", "rejectionReason": "Insurance expired..." }
```

### Paso 7: Aprobacion de documentos → Clasificacion del vehiculo

#### 7a — Aprobacion individual de documento
```
Admin
  |
  └─ PUT /api/admin/documents/{id}/review
     Body: { "action": "APPROVE" }

     → Document.status = APPROVED
     → Document.reviewedBy = adminId
     → Document.reviewedAt = now()
     → Response incluye vehicleId (si es documento de vehiculo)
```

#### 7b — Frontend detecta clasificacion lista
```
Despues de aprobar un documento de vehiculo:
  → Frontend llama GET /api/admin/vehicles/{vehicleId}/classification-check
  → Backend verifica: ¿TODOS los documentos subidos para este vehiculo estan APPROVED?
    - Si owner subio 2 docs (obligatorios) y ambos APPROVED → ready = true
    - Si owner subio 3 docs (obligatorios + INSPECTION) y los 3 APPROVED → ready = true
    - Si hay docs no aprobados aun → ready = false
  → Si ready = true:
    - Frontend abre modal de clasificacion con opciones segun reglas
```

#### 7c — Reglas de clasificacion (modal)
```
El modal muestra opciones de serviceType segun:

| Condicion | Opciones disponibles |
|-----------|---------------------|
| Vehiculo >9 anos | Solo DELIVERY_ONLY (sin eleccion) |
| Vehiculo <=9 anos sin INSPECTION aprobado | Solo DELIVERY_ONLY |
| Vehiculo <=9 anos con INSPECTION aprobado | TAXI_AND_DELIVERY o DELIVERY_ONLY |

Admin selecciona serviceType y confirma:
  → PUT /api/admin/vehicles/{vehicleId}/approve
     Body: { "serviceType": "DELIVERY_ONLY" }
  → Vehicle.status = APPROVED
  → Vehicle.serviceType = serviceType
  → Vehicle.isActive = false (owner debe activar manualmente)
```

#### 7d — Rechazo de documento
```
Admin
  |
  └─ PUT /api/admin/documents/{id}/review
     Body: { "action": "REJECT", "rejectionReason": "Insurance certificate is expired" }

     → Document.status = REJECTED
     → Document.rejectionReason = "Insurance certificate is expired"
     → Vehicle.status sigue en PENDING
     → Vehicle.isActive sigue en false
     → CarOwner puede re-subir el documento (Paso 8)
```

### Paso 8: Re-upload de documento (PENDING o REJECTED)
```
El CarOwner puede reemplazar un documento en cualquier momento ANTES de que sea aprobado.
Esto cubre dos escenarios:
  - Se equivoco al subir el archivo (status=PENDING, admin aun no reviso)
  - El admin rechazo el documento (status=REJECTED)

CarOwner
  |
  ├─ GET /api/owner/vehicles/{vehicleId}/documents
  |   → Ve: [{INSURANCE: REJECTED, reason: "Insurance certificate is expired"}]
  |
  └─ PUT /api/owner/vehicles/{vehicleId}/documents/{docId}/reupload (nuevo archivo)
      → Valida: status debe ser PENDING o REJECTED (si APPROVED → error DOC-005)
      → Reemplaza archivo anterior (borra el viejo del storage)
      → Document.status = PENDING (reset)
      → Document.rejectionReason = null (limpiado)
      → Document.reviewedAt = null (limpiado)
      → Document.reviewedBy = null (limpiado)
      → Vuelve al Paso 6 (Admin revisa de nuevo)

Si el documento ya fue APPROVED:
  → Error DOC-005: "Approved documents cannot be replaced"
  → Un documento aprobado es definitivo
```

### Paso 9: CarOwner activa vehiculo con availableUntil
```
CarOwner
  |
  └─ PUT /api/owner/vehicles/{vehicleId}/activate
     Body: { "availableUntil": "2026-03-20T17:00:00", "generalLocation": "Downtown Montreal",
             "latitude": 45.5017, "longitude": -73.5673, "hourlyRate": 15.50 }

     → Valida:
       - Vehicle.status == APPROVED (si no → VEH-009)
       - availableUntil es una fecha futura (si no → VEH-012)
       - hourlyRate >= 1.00
     → Vehicle.isActive = true
     → Vehicle.availableUntil = availableUntil
     → Vehicle.hourlyRate = hourlyRate (set at activation, not registration)
     → El vehiculo aparece en busquedas de drivers (mientras availableUntil > now())
```

### Paso 10: CarOwner desactiva un vehiculo
```
CarOwner
  |
  └─ PUT /api/owner/vehicles/{vehicleId}/deactivate
     → Valida ownership (si no → VEH-005)
     → Vehicle.isActive = false
     → El vehiculo desaparece de busquedas
```

---

## Flujo completo — Location Masking

### Busqueda de vehiculos (Driver)
```
Driver (autenticado, isVerified == true)
  |
  └─ GET /api/vehicles/search?lat=49.28&lng=-123.12&radius=10
      → Respuesta: lista de vehiculos dentro del radio
      → Coordenadas enmascaradas (~1km radius)
      → generalLocation visible (ej: "Downtown Vancouver")
      → latitude/longitude redondeadas para proteger ubicacion exacta
```

### Revelacion de ubicacion exacta (2h antes del booking)
```
Driver con booking confirmado
  |
  └─ GET /api/bookings/{id}/vehicle-location
      → Si faltan > 2h para el booking: coordenadas enmascaradas
      → Si faltan <= 2h para el booking: coordenadas exactas del vehiculo
      → Implementado via LocationMaskService
```

---

## Regla de activacion del vehiculo
```
Vehicle is available when:
  ✅ Vehicle.status == APPROVED
  ✅ Vehicle.isActive == true
  ✅ Vehicle.availableUntil > now()

Mientras isActive == false:
  ❌ No aparece en busquedas de drivers
  ❌ Dashboard del CarOwner muestra aviso de estado de documentos
```

---

## Endpoints del modulo

### CarOwner — Vehiculos (`/api/owner/vehicles`)
| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | `/api/owner/vehicles` | Registrar vehiculo nuevo |
| GET | `/api/owner/vehicles` | Listar mis vehiculos |
| GET | `/api/owner/vehicles/{id}` | Detalle de un vehiculo |
| PUT | `/api/owner/vehicles/{id}` | Actualizar info del vehiculo |
| PUT | `/api/owner/vehicles/{vehicleId}/activate` | Activar vehiculo con availableUntil |
| PUT | `/api/owner/vehicles/{vehicleId}/deactivate` | Desactivar vehiculo |

### CarOwner — Documentos del vehiculo (`/api/owner/vehicles/{vehicleId}/documents`)
| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | `.../documents/upload` | Subir documento del vehiculo |
| GET | `.../documents` | Listar documentos del vehiculo |
| GET | `.../documents/{docId}/view` | Ver documento inline |
| GET | `.../documents/{docId}/download` | Descargar documento |
| PUT | `.../documents/{docId}/reupload` | Reemplazar documento (PENDING/REJECTED) |

### Admin — Vehiculos (`/api/admin/vehicles`)
| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | `/api/admin/vehicles/{id}/classification-check` | Verificar si vehiculo esta listo para clasificar |
| PUT | `/api/admin/vehicles/{id}/approve` | Aprobar vehiculo con service type |

### Admin — Revision de documentos (reutilizado del Modulo 2)
| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | `/api/admin/documents/pending` | Listar documentos pendientes (driver + vehiculo) |
| GET | `/api/admin/documents/{id}/view` | Ver documento para revision |
| PUT | `/api/admin/documents/{id}/review` | Aprobar o rechazar documento |

---

## Error codes del modulo

### Errores de vehiculo
| Codigo | HTTP | Descripcion |
|--------|------|-------------|
| VEH-001 | 400 | Ano del vehiculo invalido (mas de 20 anos de antiguedad) |
| VEH-002 | 409 | VIN duplicado (ya existe un vehiculo con ese VIN) |
| VEH-003 | 409 | License plate duplicada (ya existe un vehiculo con esa placa) |
| VEH-004 | 404 | Vehiculo no encontrado |
| VEH-005 | 403 | El vehiculo no pertenece al usuario autenticado |
| VEH-006 | 400 | Vehiculo no activo (requiere todos los documentos aprobados para esta accion) |
| VEH-009 | 400 | Vehiculo debe estar aprobado antes de activar |
| VEH-012 | 400 | availableUntil debe ser una fecha futura |
| VEH-013 | 400 | Vehiculos con mas de 9 anos solo pueden ser DELIVERY_ONLY |

### Errores de documentos (reutilizados del Modulo 2)
| Codigo | HTTP | Descripcion |
|--------|------|-------------|
| DOC-001 | 400 | Formato de archivo invalido (solo PDF/JPG/JPEG/PNG) |
| DOC-002 | 400 | Archivo excede 5MB |
| DOC-003 | 400 | Tipo de documento invalido |
| DOC-004 | 409 | Ya existe un documento de este tipo pendiente o aprobado |
| DOC-005 | 400 | Documentos aprobados no pueden reemplazarse (solo PENDING o REJECTED) |
| DOC-006 | 403 | El documento no pertenece al usuario autenticado |
| DOC-007 | 404 | Documento no encontrado |
| DOC-008 | 400 | Razon de rechazo requerida al rechazar |
| DOC-009 | 400 | Clase de licencia requerida al aprobar DRIVERS_LICENSE |
| DOC-010 | 400 | Solo documentos PENDING pueden ser revisados |
| DOC-011 | 403 | Solo el owner del vehiculo puede subir documentos en este modulo |

---

## Entidades del modulo

### Vehicle (nueva)
```
vehicle_id (PK), owner_id (FK→CarOwner), vin, make, model, year,
license_plate, category (enum: SEDAN/SUV/VAN/TRUCK/COMPACT),
fuel_type (enum: GASOLINE/DIESEL/ELECTRIC/HYBRID),
hourly_rate, description, general_location, latitude, longitude,
status (enum: PENDING/APPROVED/INACTIVE), is_active (boolean),
available_until (datetime, nullable),
created_at, updated_at
```
- `status` y `is_active` se sincronizan: APPROVED + activado → isActive=true, PENDING/INACTIVE → isActive=false
- `owner_id` vincula al CarOwner que registro el vehiculo
- `latitude`/`longitude` se enmascaran en busquedas publicas (~1km radius)
- `available_until` indica hasta cuando el vehiculo esta disponible para renta

### Document (reutilizada del Modulo 2)
```
document_id (PK), user_id (FK→User), vehicle_id (FK→Vehicle, nullable),
reviewed_by (FK→Admin, nullable), document_type, file_url, file_name,
file_size, upload_at, status, reviewed_at, rejection_reason
```
- En Modulo 2: `user_id` siempre set, `vehicle_id` siempre null (docs del driver)
- En Modulo 3: `vehicle_id` set para docs del vehiculo, `user_id` = owner del vehiculo
- `document_type` ahora incluye: INSURANCE, VEHICLE_REGISTRATION, INSPECTION_REPORT
