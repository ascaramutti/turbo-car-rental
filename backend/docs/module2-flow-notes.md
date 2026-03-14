# Module 2 — Document Verification (Driver Documents)

## Decisiones de diseño

### Scope del módulo
Este módulo maneja **solo documentos personales del Driver**. Los documentos de vehículo
(seguro, registro, inspección) pertenecen al **Módulo 3 (Vehicle Catalog)** porque están
vinculados al vehículo (`vehicle_id`), no al usuario.

**Justificación (de los reportes):**
- Assumption 10: *"Each Driver must upload many Documents (driver's license, study permit)"*
- Assumption 12: *"Each Vehicle must have many Documents (registration, insurance, inspection)"*
- Table Document: tiene `user_id` Y `vehicle_id` como FK → docs de driver usan `user_id`, docs de vehículo usan `vehicle_id`

### Document types en este módulo
| Tipo | Quién | Obligatorio | Descripción |
|------|-------|-------------|-------------|
| DRIVERS_LICENSE | Driver | Sí (todos) | Licencia de conducir (Class 4 o Class 5) |
| STUDY_PERMIT | Driver | Sí (internacionales) | Permiso de estudio para elegibilidad laboral |

### Document types en Módulo 3 (Vehicle)
| Tipo | Quién | Obligatorio | Vinculado a |
|------|-------|-------------|-------------|
| INSURANCE | CarOwner | Sí (por vehículo) | Vehicle |
| VEHICLE_REGISTRATION | CarOwner | Sí (por vehículo) | Vehicle |
| INSPECTION_REPORT | CarOwner | Sí (por vehículo) | Vehicle |

---

## Flujo completo — Driver Document Verification

### Paso 1: Driver sube documentos
```
Driver (autenticado con JWT)
  │
  ├─ POST /api/driver/documents/upload (file=license.pdf, documentType=DRIVERS_LICENSE)
  │   → Sistema valida: formato (PDF/JPG/PNG), tamaño (<5MB)
  │   → Si válido: guarda archivo en Docker volume, crea Document(status=PENDING)
  │   → Si inválido: error DOC-001 (formato) o DOC-002 (tamaño)
  │
  └─ POST /api/driver/documents/upload (file=permit.jpg, documentType=STUDY_PERMIT)
      → Mismo proceso
```

### Paso 2: Driver consulta estado
```
Driver
  │
  └─ GET /api/driver/documents/my
      → Respuesta: [{DRIVERS_LICENSE: PENDING}, {STUDY_PERMIT: PENDING}]
      → Dashboard muestra: "Documents under review"
      → Driver NO puede buscar/reservar vehículos aún
```

### Paso 3: Admin revisa documentos
```
Admin
  │
  ├─ GET /api/admin/documents/pending
  │   → Lista de todos los documentos PENDING con datos del usuario
  │
  └─ GET /api/admin/documents/user/{driverId}
      → Ve todos los docs del driver específico
```

### Paso 4: Admin aprueba o rechaza

#### 4a — Aprobación de DRIVERS_LICENSE (Class 4)
```
Admin
  │
  └─ PUT /api/admin/documents/{id}/review
     Body: { "action": "APPROVE", "licenseClass": "CLASS_4" }

     → Document.status = APPROVED
     → Document.reviewedBy = adminId
     → Document.reviewedAt = now()
     → Driver.licenseClass = "CLASS_4"
     → Driver.isWorkEligible = true
     → Driver habilitado para: TAXI + DELIVERY
     → Si STUDY_PERMIT también APPROVED → Driver.isVerified = true
```

#### 4b — Aprobación de DRIVERS_LICENSE (Class 5)
```
Admin
  │
  └─ PUT /api/admin/documents/{id}/review
     Body: { "action": "APPROVE", "licenseClass": "CLASS_5" }

     → Document.status = APPROVED
     → Driver.licenseClass = "CLASS_5"
     → Driver habilitado solo para: DELIVERY
     → Frontend muestra waiver de "Delivery Only" para firma digital
     → Si STUDY_PERMIT también APPROVED → Driver.isVerified = true
```

#### 4c — Aprobación de STUDY_PERMIT
```
Admin
  │
  └─ PUT /api/admin/documents/{id}/review
     Body: { "action": "APPROVE" }

     → Document.status = APPROVED
     → Si DRIVERS_LICENSE también APPROVED → Driver.isVerified = true
```

#### 4d — Rechazo
```
Admin
  │
  └─ PUT /api/admin/documents/{id}/review
     Body: { "action": "REJECT", "rejectionReason": "Document is blurry..." }

     → Document.status = REJECTED
     → Document.rejectionReason = "Document is blurry..."
     → Driver.isVerified sigue en false
     → Driver recibe notificación del rechazo con la razón
```

### Paso 5: Re-upload (PENDING o REJECTED)
```
El driver puede reemplazar un documento en cualquier momento ANTES de que sea aprobado.
Esto cubre dos escenarios:
  - Se equivocó al subir el archivo (status=PENDING, admin aún no revisó)
  - El admin rechazó el documento (status=REJECTED)

Driver
  │
  ├─ GET /api/driver/documents/my
  │   → Ve: [{DRIVERS_LICENSE: PENDING}]  ← se dio cuenta que subió el archivo equivocado
  │   → O:  [{DRIVERS_LICENSE: REJECTED, reason: "Document is blurry..."}]
  │
  └─ PUT /api/driver/documents/{id}/reupload (nuevo archivo)
      → Valida: status debe ser PENDING o REJECTED (si APPROVED → error DOC-005)
      → Reemplaza archivo anterior (borra el viejo del storage)
      → Document.status = PENDING (reset)
      → Document.rejectionReason = null (limpiado)
      → Document.reviewedAt = null (limpiado)
      → Document.reviewedBy = null (limpiado)
      → Vuelve al Paso 3 (Admin revisa de nuevo)

Si el documento ya fue APPROVED:
  → Error DOC-005: "Approved documents cannot be replaced"
  → Un documento aprobado es definitivo
```

### Regla de verificación
```
Driver.isVerified = true SOLO cuando:
  ✅ DRIVERS_LICENSE.status == APPROVED
  ✅ STUDY_PERMIT.status == APPROVED (si existe)

Mientras isVerified == false:
  ❌ No puede buscar vehículos
  ❌ No puede hacer bookings
  ❌ Dashboard muestra aviso de estado de documentos
```

---

## Error codes del módulo

| Código | HTTP | Descripción |
|--------|------|-------------|
| DOC-001 | 400 | Formato de archivo inválido (solo PDF/JPG/JPEG/PNG) |
| DOC-002 | 400 | Archivo excede 5MB |
| DOC-003 | 400 | Tipo de documento inválido (solo DRIVERS_LICENSE, STUDY_PERMIT) |
| DOC-004 | 409 | Ya existe un documento de este tipo pendiente o aprobado |
| DOC-005 | 400 | Documentos aprobados no pueden reemplazarse (solo PENDING o REJECTED) |
| DOC-006 | 403 | El documento no pertenece al usuario autenticado |
| DOC-007 | 404 | Documento no encontrado |
| DOC-008 | 400 | Razón de rechazo requerida al rechazar |
| DOC-009 | 400 | Clase de licencia requerida al aprobar DRIVERS_LICENSE |
| DOC-010 | 400 | Solo documentos PENDING pueden ser revisados |
| DOC-011 | 403 | Solo drivers pueden subir documentos en este módulo |

---

## Entidades impactadas

### Document (nueva)
```
document_id (PK), user_id (FK→User), vehicle_id (FK→Vehicle, nullable),
reviewed_by (FK→Admin, nullable), document_type, file_url, file_name,
file_size, upload_at, status, reviewed_at, rejection_reason
```
- En Módulo 2: `user_id` siempre set, `vehicle_id` siempre null
- En Módulo 3: `vehicle_id` set para docs de vehículo

### Driver (modificado en approval)
- `licenseClass`: se setea en aprobación de DRIVERS_LICENSE (CLASS_4 o CLASS_5)
- `isWorkEligible`: se setea true si CLASS_4
- `checkStatus`: se actualiza ("PENDING" → "APPROVED" o "REJECTED")

### User (verificación)
- `isVerified`: se setea true cuando todos los docs requeridos del driver están APPROVED

---

# Module 3 — Vehicle Catalog (Notas preliminares)

## Scope
Registro de vehículos, documentos de vehículo, fleet management, availability slots, location masking.

## Flujo esperado del CarOwner

### 1. Registro de vehículo
```
CarOwner (autenticado, email verificado)
  │
  └─ POST /api/vehicles
     Body: { vin, make, model, year, licensePlate, category, fuelType,
             hourlyRate, description, generalLocation, latitude, longitude }

     → Sistema valida:
       - Año del vehículo <= 9 años de antigüedad
       - VIN único
       - License plate único
     → Vehicle.status = PENDING (no activo aún, necesita docs)
```

### 2. Upload de documentos del vehículo
```
CarOwner
  │
  ├─ POST /api/vehicles/{vehicleId}/documents/upload
  │   (file=insurance.pdf, documentType=INSURANCE)
  │
  ├─ POST /api/vehicles/{vehicleId}/documents/upload
  │   (file=registration.pdf, documentType=VEHICLE_REGISTRATION)
  │
  └─ POST /api/vehicles/{vehicleId}/documents/upload
      (file=inspection.pdf, documentType=INSPECTION_REPORT)

     → Mismas reglas de validación: PDF/JPG, <5MB
     → Document.vehicle_id = vehicleId
     → Document.user_id = ownerUserId
     → Document.status = PENDING
```

### 3. Admin revisa docs del vehículo
```
Admin
  │
  └─ PUT /api/admin/documents/{id}/review
     Body: { "action": "APPROVE" }

     → Cuando TODOS los docs del vehículo están APPROVED:
       - Vehicle.isActive = true
       - Vehicle aparece en búsquedas de drivers
```

### 4. Availability slots
```
CarOwner
  │
  └─ POST /api/vehicles/{vehicleId}/slots
     Body: { date, startTime, endTime, shiftDuration }

     → Crea AvailabilitySlot(status=AVAILABLE)
     → Mínimo 4 horas por slot
     → Owner puede crear múltiples slots
```

### 5. Location masking
```
- Vehículos en búsqueda: coordenadas redondeadas a 1km radius
- 2 horas antes del booking: se revela dirección exacta al driver
- Implementado via LocationMaskService
```

## Document types del vehículo (Módulo 3)
| Tipo | Obligatorio | Descripción |
|------|-------------|-------------|
| INSURANCE | Sí | Certificado de seguro comercial del vehículo |
| VEHICLE_REGISTRATION | Sí | Registro del vehículo |
| INSPECTION_REPORT | Sí | Reporte de inspección mecánica (CVIP) |

## Entidades del Módulo 3
- **Vehicle**: vin, make, model, year, licensePlate, category, fuelType, hourlyRate, isActive, insuranceExpiryDate, inspectionStatus, generalLocation, latitude, longitude
- **AvailabilitySlot**: vehicleId, startTime, endTime, isBooked
- **Document** (reutilizada): con vehicle_id para docs del vehículo
