# Module 4 — Booking Orchestrator

## Decisiones de diseno

### Scope del modulo
Este modulo maneja el **booking lifecycle completo**: busqueda de vehiculos disponibles, creacion de bookings,
confirmacion/rechazo por owners, inicio/finalizacion de shift con fotos, y cancelacion por ambas partes.
Conecta la "oferta" (vehiculos registrados y activados en Modulo 3) con la "demanda" (drivers verificados en Modulo 2).

**Justificacion (del brief aprobado):**
- El Booking Orchestrator es el modulo transaccional central de TURBO
- Drivers verificados buscan vehiculos disponibles y crean bookings por turnos (minimo 4h, maximo 24h)
- Car Owners reciben solicitudes y pueden confirmar o rechazar
- Fotos de pickup/return documentan la condicion del vehiculo
- Ubicacion exacta se revela 2h antes del booking confirmado

### Domain name
- **Nombre:** `booking`
- **Package:** `com.turbo.booking`
- **Frontend module:** `src/modules/booking`

---

## Enums

### BookingStatus
**Package:** `com.turbo.booking.model.enums`

| Valor | Significado | Transiciones desde |
|-------|-------------|-------------------|
| `PENDING` | Booking solicitado por driver, esperando decision del owner | (estado inicial) |
| `CONFIRMED` | Owner acepto el booking | PENDING |
| `IN_PROGRESS` | Driver inicio el shift (recogio vehiculo) | CONFIRMED |
| `COMPLETED` | Shift finalizado, vehiculo devuelto | IN_PROGRESS |
| `CANCELLED` | Cancelado por cualquiera de las partes | PENDING, CONFIRMED |
| `REJECTED` | Owner rechazo la solicitud | PENDING |

**State machine:**
```
PENDING ──[owner confirms]──> CONFIRMED ──[driver starts shift]──> IN_PROGRESS ──[shift ends]──> COMPLETED
  |                                |
  |──[owner rejects]──> REJECTED   |──[either cancels]──> CANCELLED
  |
  |──[either cancels]──> CANCELLED
```

---

## Entidades del modulo

### Booking (nueva)
**Table name:** `bookings`
**Package:** `com.turbo.booking.model`

| # | Campo | Java Type | Columna | Nullable | Constraints | Descripcion |
|---|-------|-----------|---------|----------|-------------|-------------|
| 1 | `bookingId` | `Long` | `booking_id` PK | No | `@Id`, `@GeneratedValue(IDENTITY)` | Primary key auto-generada |
| 2 | `driver` | `Driver` | `driver_id` FK -> `users` | No | `@ManyToOne(LAZY)`, `@JoinColumn(name = "driver_id", nullable = false)` | Driver que solicito el booking. FK a `com.turbo.user.model.Driver` |
| 3 | `vehicle` | `Vehicle` | `vehicle_id` FK -> `vehicles` | No | `@ManyToOne(LAZY)`, `@JoinColumn(name = "vehicle_id", nullable = false)` | Vehiculo reservado. FK a `com.turbo.vehicle.model.Vehicle` |
| 4 | `status` | `BookingStatus` | `status` | No | `@Enumerated(STRING)`, default `PENDING` | Estado actual del booking |
| 5 | `startTime` | `LocalDateTime` | `start_time` | No | Debe ser futuro al momento de creacion | Fecha/hora de inicio del shift |
| 6 | `endTime` | `LocalDateTime` | `end_time` | No | Debe ser >= startTime + 4h y <= startTime + 24h | Fecha/hora de fin del shift |
| 7 | `totalHours` | `Integer` | `total_hours` | No | Derivado: `Duration.between(startTime, endTime).toHours()` | Total de horas reservadas |
| 8 | `totalPrice` | `BigDecimal` | `total_price` precision=10, scale=2 | No | `totalHours * vehicle.hourlyRate` | Costo total calculado |
| 9 | `pickupLocation` | `String` | `pickup_location` length=200 | Yes | Se setea en la activacion del booking | Ubicacion de pickup legible |
| 10 | `pickupLatitude` | `Double` | `pickup_latitude` | Yes | Se revela 2h antes del start | Latitud exacta del pickup |
| 11 | `pickupLongitude` | `Double` | `pickup_longitude` | Yes | Se revela 2h antes del start | Longitud exacta del pickup |
| 12 | `cancellationReason` | `String` | `cancellation_reason` length=500 | Yes | Requerido cuando status transiciona a CANCELLED | Razon de cancelacion |
| 13 | `cancelledBy` | `String` | `cancelled_by` | Yes | `DRIVER`, `OWNER`, o `SYSTEM` | Quien inicio la cancelacion (SYSTEM para auto-cancelaciones) |
| 14 | `confirmedAt` | `LocalDateTime` | `confirmed_at` | Yes | Se setea cuando owner confirma | Timestamp de confirmacion |
| 15 | `startedAt` | `LocalDateTime` | `started_at` | Yes | Se setea cuando driver inicia el shift | Timestamp de inicio de shift |
| 16 | `completedAt` | `LocalDateTime` | `completed_at` | Yes | Se setea al completar | Timestamp de finalizacion |
| 17 | `cancelledAt` | `LocalDateTime` | `cancelled_at` | Yes | Se setea al cancelar | Timestamp de cancelacion |
| 18 | `createdAt` | `LocalDateTime` | `created_at` | No | `@PrePersist`, `updatable = false` | Timestamp de creacion del registro |
| 19 | `updatedAt` | `LocalDateTime` | `updated_at` | No | `@PrePersist` + `@PreUpdate` | Timestamp de ultima modificacion |

> **Nota:** Las fotos de pickup y return se almacenan en la tabla `booking_photos` via relacion `@OneToMany`. Ver entidad `BookingPhoto` abajo.

> **Nota:** `pickupLocation`, `pickupLatitude`, `pickupLongitude` se copian del vehiculo al momento de la confirmacion del booking (snapshot de la ubicacion del vehiculo).

### BookingPhoto (nueva)
**Table name:** `booking_photos`
**Package:** `com.turbo.booking.model`

| # | Campo | Java Type | Columna | Nullable | Constraints | Descripcion |
|---|-------|-----------|---------|----------|-------------|-------------|
| 1 | photoId | Long | photo_id PK | No | @Id, @GeneratedValue(IDENTITY) | Primary key |
| 2 | booking | Booking | booking_id FK | No | @ManyToOne(LAZY) | Booking al que pertenece |
| 3 | photoType | String | photo_type | No | "PICKUP" o "RETURN" | Tipo de foto |
| 4 | fileUrl | String | file_url length=500 | No | Ruta al archivo | URL del archivo almacenado |
| 5 | fileName | String | file_name length=255 | No | Nombre original | Nombre del archivo subido |
| 6 | fileSize | Long | file_size | No | Max 5MB | Tamaño en bytes |
| 7 | uploadedAt | LocalDateTime | uploaded_at | No | @PrePersist | Timestamp de subida |

---

## Flujo completo — Booking Orchestrator

### Flujo 1: Driver — Busqueda y creacion de booking

#### Paso 1: Driver busca vehiculos disponibles
```
Driver (autenticado con JWT, isVerified == true)
  |
  └─ GET /api/driver/bookings/vehicles/search
     Query params: ?latitude=49.28&longitude=-123.12&radiusKm=10
                   &startTime=2026-03-20T08:00:00&endTime=2026-03-20T16:00:00
                   &category=SEDAN&serviceType=TAXI_AND_DELIVERY
                   &minPrice=10&maxPrice=30&fuelType=GASOLINE

     → Sistema filtra vehiculos:
       - Vehicle.isActive == true
       - Vehicle.status == APPROVED
       - Vehicle.availableUntil > now()
       - Filtros opcionales: category, serviceType, fuelType, minPrice, maxPrice
       - Si lat/lng presentes: filtro por radio via Haversine (in-memory)
       - Excluye vehiculos con bookings conflictivos (PENDING/CONFIRMED/IN_PROGRESS)
         en el rango startTime-endTime solicitado
     → Coordenadas enmascaradas (~1km radius): Math.round(lat * 100) / 100.0
     → Cada resultado incluye effectiveServiceType y serviceTypeWarning:
       - Si driver tiene CLASS_5 y vehiculo es TAXI_AND_DELIVERY:
         effectiveServiceType = "DELIVERY_ONLY"
         serviceTypeWarning = "Your Class 5 license restricts you to delivery services only"
       - Si driver tiene CLASS_4 y vehiculo es DELIVERY_ONLY:
         effectiveServiceType = "DELIVERY_ONLY"
         serviceTypeWarning = "This vehicle is classified for delivery services only"
       - Si driver tiene CLASS_4 y vehiculo es TAXI_AND_DELIVERY:
         effectiveServiceType = "TAXI_AND_DELIVERY"
         serviceTypeWarning = null (sin warning)
       - Si driver tiene CLASS_5 y vehiculo es DELIVERY_ONLY:
         effectiveServiceType = "DELIVERY_ONLY"
         serviceTypeWarning = null (sin warning)
     → Respuesta: List<VehicleSearchResponse>
```

#### Paso 2: Driver ve detalle de un vehiculo
```
Driver
  |
  └─ GET /api/driver/bookings/vehicles/{vehicleId}
     → Detalle completo del vehiculo (ubicacion aun enmascarada)
     → Incluye: licensePlate, ownerId, vin (visible en detalle)
     → Incluye: ownerFullName, ownerRating
     → Si vehiculo no existe → VEH-004
     → Respuesta: VehicleBookingDetailResponse
```

#### Paso 3: Driver crea un booking
```
Driver
  |
  └─ POST /api/driver/bookings
     Body: { "vehicleId": 5, "startTime": "2026-03-20T08:00:00", "endTime": "2026-03-20T16:00:00" }

     → Sistema valida (en orden):
       1. Driver.isVerified == true (si no → BOOK-001)
       2. Vehicle existe (si no → VEH-004)
       3. Vehicle.status == APPROVED (si no → BOOK-002)
       4. Vehicle.isActive == true (si no → BOOK-003)
       5. endTime <= Vehicle.availableUntil (si no → BOOK-004)
       6. startTime > now() (si no → BOOK-005)
       7. endTime > startTime (si no → BOOK-006)
       8. endTime - startTime >= 4h (si no → BOOK-007)
       9. endTime - startTime <= 24h (si no → BOOK-025)
       10. No hay bookings conflictivos (PENDING/CONFIRMED/IN_PROGRESS)
           para el mismo vehiculo en el rango (si hay → BOOK-008)
       11. Driver no es el owner del vehiculo (si lo es → BOOK-009)
       12. Driver no tiene bookings conflictivos en cualquier vehiculo (si tiene → BOOK-010)
     → Booking.status = PENDING
     → totalHours = Duration.between(startTime, endTime).toHours()
     → totalPrice = totalHours * vehicle.hourlyRate
     → Precio se calcula y almacena al crear; cambios en hourlyRate no afectan bookings existentes
     → Email de notificacion al owner → "New booking request for your [make model]"
     → Respuesta: BookingResponse
```

#### Paso 4: Driver consulta sus bookings
```
Driver
  |
  ├─ GET /api/driver/bookings
  |   Query: ?status=PENDING (opcional)
  |   → Lista de bookings del driver, filtrable por status
  |   → Respuesta: List<BookingResponse>
  |
  └─ GET /api/driver/bookings/{bookingId}
     → Detalle completo del booking
     → Si booking no existe → BOOK-011
     → Si booking no pertenece al driver → BOOK-012
     → Incluye info del vehiculo y owner
     → Ubicacion: enmascarada si > 2h antes del startTime, exacta si <= 2h
     → Respuesta: BookingDetailResponse
```

#### Paso 5a: Booking confirmado por owner
```
Driver
  |
  └─ GET /api/driver/bookings/{bookingId}/vehicle-location
     → Si status es CONFIRMED o IN_PROGRESS:
       - Si faltan > 2h para startTime:
         isExactLocation = false
         latitude/longitude = enmascaradas
         message = "Exact location will be available 2 hours before your booking"
       - Si faltan <= 2h para startTime:
         isExactLocation = true
         latitude/longitude = exactas del vehiculo
         message = "Exact pickup location is now available"
     → Si status es COMPLETED:
       isExactLocation = true
       latitude/longitude = exactas (referencia)
     → Si status es PENDING/CANCELLED/REJECTED → BOOK-016 (no es CONFIRMED)
       Nota: endpoint solo valido para bookings CONFIRMED/IN_PROGRESS/COMPLETED
     → Respuesta: VehicleLocationResponse
```

#### Paso 5b: Booking rechazado por owner
```
Driver
  |
  └─ GET /api/driver/bookings/{bookingId}
     → Ve status = REJECTED
     → cancellationReason visible (razon del rechazo del owner)
```

#### Paso 6: Driver inicia shift (pickup)
```
Driver
  |
  └─ PUT /api/driver/bookings/{bookingId}/start
     Body: multipart/form-data con 1-10 fotos de pickup (campo: "pickupPhotos")

     → Valida:
       - Booking existe (si no → BOOK-011)
       - Booking pertenece al driver (si no → BOOK-012)
       - Status == CONFIRMED (si no → BOOK-016)
       - Hora actual >= startTime - 15min (si no → BOOK-017: "too early")
       - Hora actual <= startTime + 1h (si no → BOOK-018: "window expired")
       - Al menos 1 foto presente (si no → BOOK-020)
       - No mas de 10 fotos (si hay mas → BOOK-026)
       - Foto formato valido: JPG/JPEG/PNG (si no → DOC-001)
       - Foto < 5MB por archivo (si no → DOC-002)
     → Booking.status = IN_PROGRESS
     → Booking.startedAt = now()
     → Fotos guardadas en Docker volume: bookings/{bookingId}/pickup/
     → Registros en tabla booking_photos con photoType="PICKUP"
     → Respuesta: BookingResponse
```

#### Paso 7: Driver completa shift (return)
```
Driver
  |
  └─ PUT /api/driver/bookings/{bookingId}/complete
     Body: multipart/form-data con 1-10 fotos de return (campo: "returnPhotos")

     → Valida:
       - Booking existe (si no → BOOK-011)
       - Booking pertenece al driver (si no → BOOK-012)
       - Status == IN_PROGRESS (si no → BOOK-019)
       - Al menos 1 foto presente (si no → BOOK-021)
       - No mas de 10 fotos (si hay mas → BOOK-026)
       - Foto formato valido: JPG/JPEG/PNG (si no → DOC-001)
       - Foto < 5MB por archivo (si no → DOC-002)
     → Booking.status = COMPLETED
     → Booking.completedAt = now()
     → Fotos guardadas en Docker volume: bookings/{bookingId}/return/
     → Registros en tabla booking_photos con photoType="RETURN"
     → Email de notificacion al owner → "Booking completed for your [make model]"
     → Respuesta: BookingResponse
```

#### Alternativa: Driver cancela booking
```
Driver
  |
  └─ PUT /api/driver/bookings/{bookingId}/cancel
     Body: { "reason": "Schedule conflict" }

     → Valida:
       - Booking existe (si no → BOOK-011)
       - Booking pertenece al driver (si no → BOOK-012)
       - Status == PENDING o CONFIRMED (si no → BOOK-015)
       - reason no vacia (si vacia → BOOK-022)
     → Booking.status = CANCELLED
     → Booking.cancelledBy = "DRIVER"
     → Booking.cancelledAt = now()
     → Booking.cancellationReason = reason
     → Email de notificacion al owner → "Booking cancelled by driver"
     → Respuesta: BookingResponse
```

---

### Flujo 2: Car Owner — Gestion de bookings

#### Paso 1: Owner ve solicitudes de booking
```
CarOwner (autenticado con JWT, email verificado)
  |
  ├─ GET /api/owner/bookings
  |   Query: ?status=PENDING&vehicleId=5 (ambos opcionales)
  |   → Lista de bookings para vehiculos del owner
  |   → Filtrable por status y/o vehicleId
  |   → Muestra: nombre del driver, vehiculo, fechas, precio total
  |   → Respuesta: List<BookingResponse>
  |
  └─ GET /api/owner/bookings/{bookingId}
     → Detalle completo del booking
     → Si booking no existe → BOOK-011
     → Si el vehiculo del booking no pertenece al owner → BOOK-012
     → Respuesta: BookingDetailResponse
```

#### Paso 2a: Owner confirma booking
```
CarOwner
  |
  └─ PUT /api/owner/bookings/{bookingId}/confirm
     (sin body)

     → Valida:
       - Booking existe (si no → BOOK-011)
       - Vehiculo del booking pertenece al owner (si no → BOOK-012)
       - Status == PENDING (si no → BOOK-013)
       - Vehicle.isActive == true (si no → BOOK-003)
     → Booking.status = CONFIRMED
     → Booking.confirmedAt = now()
     → Copia ubicacion del vehiculo al booking:
       - pickupLocation = vehicle.generalLocation
       - pickupLatitude = vehicle.latitude
       - pickupLongitude = vehicle.longitude
     → Auto-reject: otros bookings PENDING del mismo driver que se solapan
       con el rango confirmado → status = REJECTED con reason
       "Auto-rejected: driver already has a confirmed booking for this time slot"
     → Email de notificacion al driver → "Your booking has been confirmed"
     → Respuesta: BookingResponse
```

#### Paso 2b: Owner rechaza booking
```
CarOwner
  |
  └─ PUT /api/owner/bookings/{bookingId}/reject
     Body: { "reason": "Vehicle is unavailable for those dates" }

     → Valida:
       - Booking existe (si no → BOOK-011)
       - Vehiculo del booking pertenece al owner (si no → BOOK-012)
       - Status == PENDING (si no → BOOK-014)
       - reason no vacia (si vacia → BOOK-023)
     → Booking.status = REJECTED
     → Booking.cancellationReason = reason
     → Email de notificacion al driver → "Your booking request was rejected"
     → Respuesta: BookingResponse
```

#### Paso 3: Owner cancela booking confirmado
```
CarOwner
  |
  └─ PUT /api/owner/bookings/{bookingId}/cancel
     Body: { "reason": "Emergency - vehicle needs repairs" }

     → Valida:
       - Booking existe (si no → BOOK-011)
       - Vehiculo del booking pertenece al owner (si no → BOOK-012)
       - Status == CONFIRMED (owner solo puede cancelar CONFIRMED, no PENDING ni IN_PROGRESS)
         (si no → BOOK-015)
       - reason no vacia (si vacia → BOOK-022)
     → Booking.status = CANCELLED
     → Booking.cancelledBy = "OWNER"
     → Booking.cancelledAt = now()
     → Booking.cancellationReason = reason
     → Email de notificacion al driver → "Booking cancelled by vehicle owner"
     → Respuesta: BookingResponse
```

---

### Flujo 3: Admin — Vista de plataforma

#### Paso 1: Admin ve todos los bookings
```
Admin (autenticado con JWT)
  |
  └─ GET /api/admin/bookings
     Query: ?status=IN_PROGRESS&driverId=2&vehicleId=5 (todos opcionales)
     → Lista de TODOS los bookings de la plataforma
     → Filtrable por status, driverId, vehicleId
     → Respuesta: List<AdminBookingResponse>
```

#### Paso 2: Admin ve detalle de un booking
```
Admin
  |
  └─ GET /api/admin/bookings/{bookingId}
     → Detalle completo con fotos de pickup/return
     → Info del driver, owner, vehiculo
     → Para resolucion de disputas
     → Si booking no existe → BOOK-011
     → Respuesta: AdminBookingDetailResponse
```

---

## Flujo completo — Location Masking

### Busqueda de vehiculos (Driver)
```
Driver (autenticado, isVerified == true)
  |
  └─ GET /api/driver/bookings/vehicles/search?latitude=49.28&longitude=-123.12&radiusKm=10
      → Respuesta: lista de vehiculos dentro del radio
      → Coordenadas enmascaradas (~1km radius)
        - maskedLatitude = Math.round(lat * 100) / 100.0
        - maskedLongitude = Math.round(lng * 100) / 100.0
      → generalLocation visible (ej: "Downtown Vancouver")
```

### Revelacion de ubicacion exacta (2h antes del booking)
```
Driver con booking confirmado
  |
  └─ GET /api/driver/bookings/{bookingId}/vehicle-location

     | Condicion | Lat/Lng devueltas |
     |-----------|-------------------|
     | Busqueda de vehiculos (cualquier momento) | Enmascaradas (~1km) |
     | Booking detail, > 2h antes del startTime | Enmascaradas (~1km) |
     | Booking detail, <= 2h antes del startTime Y status CONFIRMED/IN_PROGRESS | Exactas |
     | Booking status COMPLETED | Exactas (referencia) |

      → Si faltan > 2h: coordenadas enmascaradas + message informativo
      → Si faltan <= 2h: coordenadas exactas del vehiculo
      → Implementado via LocationMaskService
```

---

## Auto-cancelacion y auto-completion

### Auto-cancelacion de bookings PENDING
```
@Scheduled task: cada 5 minutos

→ Busca bookings donde:
  - status == PENDING
  - startTime <= now()
→ Para cada uno:
  - Booking.status = CANCELLED
  - Booking.cancelledBy = "SYSTEM"
  - Booking.cancellationReason = "Auto-cancelled: owner did not respond before booking start time"
  - Booking.cancelledAt = now()
```

### Auto-completion de bookings IN_PROGRESS
```
@Scheduled task: cada 5 minutos

→ Busca bookings donde:
  - status == IN_PROGRESS
  - endTime + 2h <= now()
→ Para cada uno:
  - Booking.status = COMPLETED
  - Booking.completedAt = now()
  - Flagged para revision de admin
```

---

## Service type matching — Logica informacional

La coincidencia de tipo de servicio es **informacional**, NO un bloqueo duro. Cualquier driver puede reservar cualquier vehiculo.

```
| Driver licenseClass | Vehicle serviceType   | effectiveServiceType | serviceTypeWarning |
|---------------------|-----------------------|----------------------|--------------------|
| CLASS_4             | TAXI_AND_DELIVERY     | TAXI_AND_DELIVERY    | null               |
| CLASS_4             | DELIVERY_ONLY         | DELIVERY_ONLY        | "This vehicle is classified for delivery services only" |
| CLASS_5             | TAXI_AND_DELIVERY     | DELIVERY_ONLY        | "Your Class 5 license restricts you to delivery services only" |
| CLASS_5             | DELIVERY_ONLY         | DELIVERY_ONLY        | null               |
```

Se incluye `effectiveServiceType` y `serviceTypeWarning` en:
- `VehicleSearchResponse` (busqueda)
- `BookingDetailResponse` (detalle del booking)

---

## Validaciones por campo — DTOs

### CreateBookingRequest

| Campo | Java Type | Required | Constraint | Regex/Pattern | Descripcion |
|-------|-----------|----------|------------|---------------|-------------|
| `vehicleId` | `Long` | Si | `@NotNull` | N/A (numeric) | ID del vehiculo a reservar |
| `startTime` | `LocalDateTime` | Si | `@NotNull`, `@Future` | ISO-8601 datetime | Fecha/hora de inicio del shift |
| `endTime` | `LocalDateTime` | Si | `@NotNull` | ISO-8601 datetime | Fecha/hora de fin del shift. Validaciones de negocio: >= startTime + 4h, <= startTime + 24h |

### CancelBookingRequest

| Campo | Java Type | Required | Constraint | Regex/Pattern | Descripcion |
|-------|-----------|----------|------------|---------------|-------------|
| `reason` | `String` | Si | `@NotBlank`, `@Size(max=500)` | `^[a-zA-Z0-9\u00C0-\u00FF\s.,!?()'+:\-/]*$` | Razon de cancelacion. Letras, numeros, espacios, puntuacion basica |

### RejectBookingRequest

| Campo | Java Type | Required | Constraint | Regex/Pattern | Descripcion |
|-------|-----------|----------|------------|---------------|-------------|
| `reason` | `String` | Si | `@NotBlank`, `@Size(max=500)` | `^[a-zA-Z0-9\u00C0-\u00FF\s.,!?()'+:\-/]*$` | Razon de rechazo. Letras, numeros, espacios, puntuacion basica |

### Vehicle Search Query Parameters

| Param | Java Type | Required | Default | Constraint | Regex/Pattern |
|-------|-----------|----------|---------|------------|---------------|
| `latitude` | `Double` | No | - | `@DecimalMin(-90)`, `@DecimalMax(90)` | N/A (numeric) |
| `longitude` | `Double` | No | - | `@DecimalMin(-180)`, `@DecimalMax(180)` | N/A (numeric) |
| `radiusKm` | `Double` | No | `10.0` | `@DecimalMin(1)`, `@DecimalMax(100)` | N/A (numeric) |
| `startTime` | `LocalDateTime` | No | - | Si presente, debe ser futuro | ISO-8601 datetime |
| `endTime` | `LocalDateTime` | No | - | Si presente, debe ser despues de startTime | ISO-8601 datetime |
| `category` | `String` | No | - | | `^(SEDAN\|SUV\|VAN\|TRUCK\|COMPACT)$` |
| `serviceType` | `String` | No | - | | `^(TAXI_AND_DELIVERY\|DELIVERY_ONLY)$` |
| `minPrice` | `BigDecimal` | No | - | `@DecimalMin(0)` | N/A (numeric) |
| `maxPrice` | `BigDecimal` | No | - | `@DecimalMin(0)` | N/A (numeric) |
| `fuelType` | `String` | No | - | | `^(GASOLINE\|DIESEL\|ELECTRIC\|HYBRID)$` |

---

## Validation constants planeadas

### BookingValidationConstraints
**Package:** `com.turbo.booking.validation`

```java
public static final int MIN_SHIFT_HOURS = 4;
public static final int MAX_SHIFT_HOURS = 24;
public static final int EARLY_START_MINUTES = 15;
public static final int LATE_START_HOURS = 1;
public static final int LOCATION_REVEAL_HOURS = 2;
public static final int AUTO_CANCEL_CHECK_MINUTES = 5;
public static final int AUTO_COMPLETE_GRACE_HOURS = 2;
public static final int MAX_REASON_LENGTH = 500;
public static final int MAX_PICKUP_LOCATION_LENGTH = 200;
public static final int MAX_PHOTO_URL_LENGTH = 500;
public static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;
public static final String REASON_PATTERN = "^[a-zA-Z0-9À-ÿ\\s.,!?()'+:\\-/]*$";
public static final String CANCELLED_BY_PATTERN = "^(DRIVER|OWNER|SYSTEM)$";
public static final String BOOKING_STATUS_PATTERN = "^(PENDING|CONFIRMED|IN_PROGRESS|COMPLETED|CANCELLED|REJECTED)$";
public static final String CATEGORY_PATTERN = "^(SEDAN|SUV|VAN|TRUCK|COMPACT)$";
public static final String SERVICE_TYPE_PATTERN = "^(TAXI_AND_DELIVERY|DELIVERY_ONLY)$";
public static final String FUEL_TYPE_PATTERN = "^(GASOLINE|DIESEL|ELECTRIC|HYBRID)$";
```

### BookingValidationMessages
**Package:** `com.turbo.booking.validation`

```java
public static final String VEHICLE_ID_REQUIRED = "Vehicle ID is required";
public static final String START_TIME_REQUIRED = "Start time is required";
public static final String START_TIME_FUTURE = "Start time must be in the future";
public static final String END_TIME_REQUIRED = "End time is required";
public static final String REASON_REQUIRED = "Reason is required";
public static final String REASON_PATTERN_MSG = "Reason can only contain letters, numbers, spaces, and basic punctuation";
public static final String PICKUP_PHOTO_REQUIRED = "At least one pickup photo is required to start the shift";
public static final String RETURN_PHOTO_REQUIRED = "At least one return photo is required to complete the shift";
```

---

## Endpoints del modulo

### Driver — Bookings (`/api/driver/bookings`)
Secured by: `hasAuthority(DRIVER)` via SecurityConfig `/api/driver/**` rule

| # | Metodo | Endpoint | Descripcion |
|---|--------|----------|-------------|
| 1 | GET | `/api/driver/bookings/vehicles/search` | Buscar vehiculos disponibles |
| 2 | GET | `/api/driver/bookings/vehicles/{vehicleId}` | Ver detalle de vehiculo para booking |
| 3 | POST | `/api/driver/bookings` | Crear un nuevo booking |
| 4 | GET | `/api/driver/bookings` | Listar mis bookings (como driver) |
| 5 | GET | `/api/driver/bookings/{bookingId}` | Ver detalle de un booking |
| 6 | PUT | `/api/driver/bookings/{bookingId}/cancel` | Cancelar un booking PENDING o CONFIRMED |
| 7 | PUT | `/api/driver/bookings/{bookingId}/start` | Iniciar shift (multipart/form-data with 1-10 pickup photos) |
| 8 | PUT | `/api/driver/bookings/{bookingId}/complete` | Completar shift (multipart/form-data with 1-10 return photos) |
| 9 | GET | `/api/driver/bookings/{bookingId}/vehicle-location` | Obtener ubicacion del vehiculo (enmascarada o exacta) |

### Car Owner — Bookings (`/api/owner/bookings`)
Secured by: `hasAuthority(CAR_OWNER)` via SecurityConfig `/api/owner/**` rule

| # | Metodo | Endpoint | Descripcion |
|---|--------|----------|-------------|
| 10 | GET | `/api/owner/bookings` | Listar bookings de mis vehiculos |
| 11 | GET | `/api/owner/bookings/{bookingId}` | Ver detalle de un booking |
| 12 | PUT | `/api/owner/bookings/{bookingId}/confirm` | Confirmar un booking PENDING |
| 13 | PUT | `/api/owner/bookings/{bookingId}/reject` | Rechazar un booking PENDING |
| 14 | PUT | `/api/owner/bookings/{bookingId}/cancel` | Cancelar un booking CONFIRMED |

### Admin — Bookings (`/api/admin/bookings`)
Secured by: `hasAuthority(ADMIN)` via SecurityConfig `/api/admin/**` rule

| # | Metodo | Endpoint | Descripcion |
|---|--------|----------|-------------|
| 15 | GET | `/api/admin/bookings` | Listar todos los bookings (vista de plataforma) |
| 16 | GET | `/api/admin/bookings/{bookingId}` | Ver detalle completo de un booking (con fotos) |

---

## Error codes del modulo

### Errores de booking (BOOK-XXX)
**Package:** `com.turbo.exception.error.BookingErrorCode`

| Codigo | HTTP | Mensaje | Cuando | Endpoints que lo disparan |
|--------|------|---------|--------|---------------------------|
| `BOOK-001` | 403 | Driver must be verified before creating bookings | `driver.getIsVerified() == false` | POST /api/driver/bookings |
| `BOOK-002` | 400 | Vehicle is not approved for rentals | `vehicle.getStatus() != APPROVED` | POST /api/driver/bookings |
| `BOOK-003` | 400 | Vehicle is not currently available for rent | `vehicle.getIsActive() == false` | POST /api/driver/bookings, PUT .../confirm |
| `BOOK-004` | 400 | Booking end time exceeds vehicle availability window | `endTime > vehicle.getAvailableUntil()` | POST /api/driver/bookings |
| `BOOK-005` | 400 | Booking start time must be in the future | `startTime <= now()` | POST /api/driver/bookings |
| `BOOK-006` | 400 | Booking end time must be after start time | `endTime <= startTime` | POST /api/driver/bookings |
| `BOOK-007` | 400 | Minimum booking duration is 4 hours | `endTime - startTime < 4h` | POST /api/driver/bookings |
| `BOOK-008` | 409 | Vehicle is already booked for the requested time slot | Booking existente conflictivo para el vehiculo | POST /api/driver/bookings |
| `BOOK-009` | 400 | You cannot book your own vehicle | `vehicle.getOwner().getUserId().equals(driverId)` | POST /api/driver/bookings |
| `BOOK-010` | 409 | You already have an active booking during this time slot | Driver tiene booking conflictivo | POST /api/driver/bookings |
| `BOOK-011` | 404 | Booking not found | `bookingId` no existe | GET/PUT .../bookings/{id}, GET .../vehicle-location |
| `BOOK-012` | 403 | You do not have permission to access this booking | Booking no pertenece al usuario solicitante | GET/PUT .../bookings/{id}, GET .../vehicle-location |
| `BOOK-013` | 400 | Only pending bookings can be confirmed | `status != PENDING` al confirmar | PUT /api/owner/bookings/{id}/confirm |
| `BOOK-014` | 400 | Only pending bookings can be rejected | `status != PENDING` al rechazar | PUT /api/owner/bookings/{id}/reject |
| `BOOK-015` | 400 | Only pending or confirmed bookings can be cancelled | `status != PENDING && status != CONFIRMED` al cancelar | PUT .../bookings/{id}/cancel |
| `BOOK-016` | 400 | Only confirmed bookings can be started | `status != CONFIRMED` al iniciar | PUT /api/driver/bookings/{id}/start |
| `BOOK-017` | 400 | Booking cannot be started yet (too early) | Mas de 15min antes del startTime | PUT /api/driver/bookings/{id}/start |
| `BOOK-018` | 400 | Booking start window has expired | Mas de 1h despues del startTime sin iniciar | PUT /api/driver/bookings/{id}/start |
| `BOOK-019` | 400 | Only in-progress bookings can be completed | `status != IN_PROGRESS` al completar | PUT /api/driver/bookings/{id}/complete |
| `BOOK-020` | 400 | At least one pickup photo is required to start the shift | Foto faltante al iniciar | PUT /api/driver/bookings/{id}/start |
| `BOOK-021` | 400 | At least one return photo is required to complete the shift | Foto faltante al completar | PUT /api/driver/bookings/{id}/complete |
| `BOOK-022` | 400 | Cancellation reason is required | Razon vacia al cancelar | PUT .../bookings/{id}/cancel |
| `BOOK-023` | 400 | Rejection reason is required | Razon vacia al rechazar | PUT /api/owner/bookings/{id}/reject |
| `BOOK-025` | 400 | Maximum booking duration is 24 hours | `endTime - startTime > 24h` | POST /api/driver/bookings |
| `BOOK-026` | 400 | Maximum 10 photos allowed per upload | Mas de 10 archivos en start/complete | PUT /api/driver/bookings/{id}/start, PUT /api/driver/bookings/{id}/complete |

> **Nota:** BOOK-024 fue intencionalmente omitido. Originalmente se planeo para bloquear bookings por mismatch de service type, pero la decision de diseno final es que el service type matching es **informacional** (warnings, no errores). Ver seccion "Service type matching".

### Error codes reutilizados de otros modulos

| Codigo | Origen | Cuando se usa en Module 4 |
|--------|--------|---------------------------|
| `VEH-004` | `com.turbo.exception.error.VehicleErrorCode.VEHICLE_NOT_FOUND` | Vehiculo no encontrado en creacion de booking o detalle de vehiculo |
| `AUTH-002` | `com.turbo.exception.error.AuthErrorCode.USER_NOT_FOUND` | Usuario del token JWT no encontrado (via SecurityHelper) |
| `DOC-001` | `com.turbo.exception.error.DocumentErrorCode.INVALID_FILE_FORMAT` | Foto de pickup/return con formato invalido |
| `DOC-002` | `com.turbo.exception.error.DocumentErrorCode.FILE_TOO_LARGE` | Foto de pickup/return excede 5MB |

---

## Reglas de negocio y restricciones

### State transitions
```
| Transicion | Quien puede trigger | Reglas adicionales |
|-----------|--------------------|--------------------|
| PENDING -> CONFIRMED | CAR_OWNER (owner del vehiculo) | Vehicle.isActive == true |
| PENDING -> REJECTED | CAR_OWNER (owner del vehiculo) | Rejection reason requerida |
| PENDING -> CANCELLED | DRIVER o CAR_OWNER | Cancellation reason requerida |
| CONFIRMED -> IN_PROGRESS | DRIVER | startTime - 15min <= now() <= startTime + 1h, pickup photo requerida |
| CONFIRMED -> CANCELLED | DRIVER o CAR_OWNER | Cancellation reason requerida |
| IN_PROGRESS -> COMPLETED | DRIVER | Return photo requerida |
| CANCELLED/REJECTED/COMPLETED | (terminal) | No mas transiciones |
```

### Pickup/return photos
| Regla | Valor |
|-------|-------|
| Cantidad | Min 1 foto, Max 10 fotos por evento (pickup o return) |
| Formatos aceptados | JPG, JPEG, PNG (solo fotos) |
| Tamano maximo | 5MB por foto |
| Storage | Docker volume, mismo patron que `FileStorageService` en Module 2 |
| Subdirectorio pickup | `bookings/{bookingId}/pickup/` |
| Subdirectorio return | `bookings/{bookingId}/return/` |

### Calculo de precio
```
totalHours = Duration.between(startTime, endTime).toHours()
totalPrice = totalHours * vehicle.hourlyRate

- BigDecimal precision: (10, 2) matching Vehicle.hourlyRate
- Precio se calcula y almacena al crear el booking
- Cambios en hourlyRate despues del booking NO afectan bookings existentes
```

### Regla de PENDING multiples
```
- Un driver puede tener multiples bookings PENDING que se solapan
  (puede "aplicar" a varios vehiculos)
- Al confirmar un booking → auto-reject de otros PENDING del mismo driver
  que se solapan con el rango confirmado
- Solo un booking CONFIRMED/IN_PROGRESS por slot de tiempo por driver
```

---

## Security restrictions por rol

| Rol | Endpoints accesibles | Restricciones |
|-----|---------------------|---------------|
| DRIVER | `/api/driver/bookings/**` | Solo ve/modifica sus propios bookings. Debe tener `isVerified == true` para crear bookings |
| CAR_OWNER | `/api/owner/bookings/**` | Solo ve/modifica bookings de vehiculos que le pertenecen |
| ADMIN | `/api/admin/bookings/**` | Ve todos los bookings, modo lectura (sin acciones de modificacion en Module 4) |

> SecurityConfig ya maneja estos patrones de URL, **no se requieren cambios** al SecurityConfig.

---

## Dependencias con modulos previos

### Module 1 — Auth & Security
| Que | Donde | Como |
|-----|-------|------|
| JWT authentication | `com.turbo.config.JwtAuthFilter` | Todos los endpoints requieren JWT valido |
| User resolution | `com.turbo.config.SecurityHelper.getCurrentUser()` | Resuelve `User` entity desde SecurityContext en cada controller |
| Role-based access | `com.turbo.config.SecurityConfig` | `/api/driver/**` -> DRIVER, `/api/owner/**` -> CAR_OWNER, `/api/admin/**` -> ADMIN |
| Error code reuse | `com.turbo.exception.error.AuthErrorCode.USER_NOT_FOUND` | Cuando el usuario del JWT no existe |
| Error interface | `com.turbo.exception.error.ErrorCode` | BookingErrorCode implementa esta interfaz |
| BusinessException | `com.turbo.exception.BusinessException` | Lanza excepciones con BookingErrorCode |

### Module 2 — Document Verification
| Que | Donde | Como |
|-----|-------|------|
| Driver verification | `com.turbo.user.model.User.isVerified` | Creacion de booking requiere `driver.getIsVerified() == true` |
| Driver entity | `com.turbo.user.model.Driver` | FK en Booking. Campos usados: `licenseClass`, `isWorkEligible`, `rating` |
| File storage | `com.turbo.document.service.FileStorageService` | Reutilizar para almacenamiento de fotos pickup/return |
| File response helper | `com.turbo.document.util.FileResponseHelper` | Reutilizar para servir archivos de fotos |
| Error code reuse | `com.turbo.exception.error.DocumentErrorCode.INVALID_FILE_FORMAT` | Cuando foto tiene formato invalido (DOC-001) |
| Error code reuse | `com.turbo.exception.error.DocumentErrorCode.FILE_TOO_LARGE` | Cuando foto excede 5MB (DOC-002) |

### Module 3 — Vehicle Catalog
| Que | Donde | Como |
|-----|-------|------|
| Vehicle entity | `com.turbo.vehicle.model.Vehicle` | FK en Booking. Campos: `hourlyRate`, `isActive`, `availableUntil`, `status`, `serviceType`, `generalLocation`, `latitude`, `longitude` |
| Vehicle repository | `com.turbo.vehicle.repository.VehicleRepository` | Query de vehiculos disponibles para busqueda |
| Vehicle enums | `com.turbo.vehicle.model.enums.VehicleStatus` | `APPROVED` para validar vehiculo aprobado |
| Vehicle enums | `com.turbo.vehicle.model.enums.VehicleCategory` | Filtro de categoria en busqueda |
| Vehicle enums | `com.turbo.vehicle.model.enums.FuelType` | Filtro de tipo de combustible en busqueda |
| Vehicle enums | `com.turbo.vehicle.model.enums.ServiceType` | Filtro y logica de service type matching |
| CarOwner entity | `com.turbo.user.model.CarOwner` | Accedido via `vehicle.getOwner()` para info del owner en respuestas |
| Error code reuse | `com.turbo.exception.error.VehicleErrorCode.VEHICLE_NOT_FOUND` | Cuando vehiculo no existe (VEH-004) |

---

## Backend file structure planeada

```
backend/src/main/java/com/turbo/booking/
  |
  |-- model/
  |   |-- Booking.java                          # @Entity
  |   |-- BookingPhoto.java                     # @Entity, tabla booking_photos
  |   |-- enums/
  |       |-- BookingStatus.java                 # PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED
  |
  |-- repository/
  |   |-- BookingRepository.java                 # JpaRepository<Booking, Long>
  |   |-- BookingPhotoRepository.java            # JpaRepository<BookingPhoto, Long>
  |
  |-- service/
  |   |-- BookingService.java                    # Interface
  |   |-- LocationMaskService.java               # Interface para logica de location masking
  |   |-- impl/
  |   |   |-- BookingServiceImpl.java            # @Service implementation
  |   |   |-- LocationMaskServiceImpl.java       # @Service implementation
  |   |-- command/
  |   |   |-- CreateBookingCommand.java
  |   |   |-- ConfirmBookingCommand.java
  |   |   |-- RejectBookingCommand.java
  |   |   |-- CancelBookingCommand.java
  |   |   |-- StartBookingCommand.java
  |   |   |-- CompleteBookingCommand.java
  |   |   |-- GetBookingCommand.java
  |   |   |-- SearchVehiclesCommand.java
  |
  |-- controller/
  |   |-- DriverBookingController.java           # @RestController /api/driver/bookings
  |   |-- OwnerBookingController.java            # @RestController /api/owner/bookings
  |   |-- AdminBookingController.java            # @RestController /api/admin/bookings
  |   |-- mapper/
  |       |-- DriverBookingControllerMapper.java # MapStruct: Request -> Command, Entity -> Response
  |       |-- OwnerBookingControllerMapper.java  # MapStruct
  |       |-- AdminBookingControllerMapper.java  # MapStruct
  |
  |-- dto/
  |   |-- CreateBookingRequest.java
  |   |-- CancelBookingRequest.java
  |   |-- RejectBookingRequest.java
  |   |-- BookingResponse.java
  |   |-- BookingDetailResponse.java
  |   |-- AdminBookingResponse.java
  |   |-- AdminBookingDetailResponse.java
  |   |-- VehicleSearchResponse.java
  |   |-- VehicleBookingDetailResponse.java
  |   |-- VehicleLocationResponse.java
  |
  |-- validation/
      |-- BookingValidationConstraints.java      # Constants: MIN_SHIFT_HOURS, EARLY_START_MINUTES, etc.
      |-- BookingValidationMessages.java         # Validation annotation messages

backend/src/main/java/com/turbo/exception/error/
  |-- BookingErrorCode.java                      # enum implements ErrorCode (BOOK-001 to BOOK-026)
```

---

## Frontend file structure planeada

```
frontend/src/modules/booking/
  |
  |-- api/
  |   |-- bookingApi.js                          # Axios calls to /driver/bookings and /owner/bookings
  |
  |-- constants/
  |   |-- bookingConstants.js                    # BOOKING_STATUS, STATUS_CONFIG, MIN_SHIFT_HOURS, etc.
  |
  |-- components/
  |   |-- BookingCard.jsx                        # Booking summary card (reusado en listas)
  |   |-- BookingStatusBadge.jsx                 # Badge de status con color
  |   |-- VehicleSearchCard.jsx                  # Card de vehiculo en resultados de busqueda
  |   |-- BookingForm.jsx                        # Date/time picker para crear booking
  |   |-- PhotoUpload.jsx                        # Componente de upload de fotos pickup/return
  |   |-- LocationMap.jsx                        # Componente de mapa mostrando ubicacion enmascarada/exacta
  |
  |-- pages/
  |   |-- DriverSearchPage.jsx                   # Pagina de busqueda de vehiculos para drivers
  |   |-- DriverBookingsPage.jsx                 # Dashboard de bookings del driver
  |   |-- DriverBookingDetailPage.jsx            # Detalle de un booking (vista driver)
  |   |-- OwnerBookingsPage.jsx                  # Gestion de bookings del owner
  |   |-- OwnerBookingDetailPage.jsx             # Detalle de un booking (vista owner)
  |
  |-- utils/
  |   |-- bookingValidation.js                   # Validaciones frontend (min 4h, max 24h, fechas futuras, etc.)
  |
  |-- tests/
      |-- BookingCard.test.jsx
      |-- BookingForm.test.jsx
      |-- DriverSearchPage.test.jsx
      |-- DriverBookingsPage.test.jsx
      |-- OwnerBookingsPage.test.jsx
      |-- bookingValidation.test.js
```

---

## Service Layer Commands

| Command | Campos | Mapeado desde |
|---------|--------|---------------|
| `CreateBookingCommand` | `driverId`, `vehicleId`, `startTime`, `endTime` | `CreateBookingRequest` + SecurityHelper |
| `ConfirmBookingCommand` | `bookingId`, `ownerId` | path var + SecurityHelper |
| `RejectBookingCommand` | `bookingId`, `ownerId`, `reason` | `RejectBookingRequest` + SecurityHelper |
| `CancelBookingCommand` | `bookingId`, `userId`, `userRole`, `reason` | `CancelBookingRequest` + SecurityHelper |
| `StartBookingCommand` | `bookingId`, `driverId`, `pickupPhotos` (List<MultipartFile>) | multipart + SecurityHelper |
| `CompleteBookingCommand` | `bookingId`, `driverId`, `returnPhotos` (List<MultipartFile>) | multipart + SecurityHelper |
| `GetBookingCommand` | `bookingId`, `userId`, `userRole` | path var + SecurityHelper |
| `SearchVehiclesCommand` | `driverId`, `latitude`, `longitude`, `radiusKm`, `startTime`, `endTime`, `category`, `serviceType`, `minPrice`, `maxPrice`, `fuelType` | query params + SecurityHelper |

---

## Repository Queries planeados

### BookingRepository (`JpaRepository<Booking, Long>`)
```java
// Driver's bookings
List<Booking> findByDriverUserId(Long driverId);
List<Booking> findByDriverUserIdAndStatus(Long driverId, BookingStatus status);

// Owner's bookings (todos los vehiculos de este owner)
List<Booking> findByVehicleOwnerUserId(Long ownerId);
List<Booking> findByVehicleOwnerUserIdAndStatus(Long ownerId, BookingStatus status);
List<Booking> findByVehicleVehicleIdAndVehicleOwnerUserId(Long vehicleId, Long ownerId);

// Conflict check: bookings conflictivos para un vehiculo
@Query("SELECT b FROM Booking b WHERE b.vehicle.vehicleId = :vehicleId " +
       "AND b.status IN :activeStatuses " +
       "AND b.startTime < :endTime AND b.endTime > :startTime")
List<Booking> findConflictingBookings(...);

// Conflict check: bookings conflictivos para un driver
@Query("SELECT b FROM Booking b WHERE b.driver.userId = :driverId " +
       "AND b.status IN :activeStatuses " +
       "AND b.startTime < :endTime AND b.endTime > :startTime")
List<Booking> findDriverConflictingBookings(...);

// Admin: todos los bookings con filtros opcionales
List<Booking> findAll();
List<Booking> findByStatus(BookingStatus status);

// Auto-cancel: PENDING bookings con startTime pasado
@Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.startTime <= :now")
List<Booking> findPendingBookingsPastStartTime(@Param("now") LocalDateTime now);

// Auto-complete: IN_PROGRESS bookings con endTime + grace period pasado
@Query("SELECT b FROM Booking b WHERE b.status = 'IN_PROGRESS' AND b.endTime <= :graceCutoff")
List<Booking> findInProgressBookingsPastGracePeriod(@Param("graceCutoff") LocalDateTime graceCutoff);
```

### Query adicional en VehicleRepository
```java
@Query("SELECT v FROM Vehicle v WHERE v.isActive = true " +
       "AND v.status = 'APPROVED' " +
       "AND v.availableUntil > :now " +
       "AND (:category IS NULL OR v.category = :category) " +
       "AND (:serviceType IS NULL OR v.serviceType = :serviceType) " +
       "AND (:fuelType IS NULL OR v.fuelType = :fuelType) " +
       "AND (:minPrice IS NULL OR v.hourlyRate >= :minPrice) " +
       "AND (:maxPrice IS NULL OR v.hourlyRate <= :maxPrice)")
List<Vehicle> searchAvailableVehicles(...);
```

> **Nota:** Filtro de geo-radio se hace in-memory (Java) despues del query, usando formula Haversine en `LocationMaskService`. Esto evita extensiones espaciales de DB para el piloto.

---

## Data Initializer additions

Actualizar `com.turbo.config.DataInitializer` para seed de bookings de prueba:

| Booking | Driver | Vehicle | Status | Start | End |
|---------|--------|---------|--------|-------|-----|
| Booking #1 | driver@turbo.com | (primer vehiculo del owner) | COMPLETED | ayer 08:00 | ayer 16:00 |
| Booking #2 | driver@turbo.com | (primer vehiculo del owner) | CONFIRMED | manana 08:00 | manana 16:00 |
| Booking #3 | driver@turbo.com | (primer vehiculo del owner) | PENDING | pasado manana 08:00 | pasado manana 12:00 |

> Seed data depende de vehiculos existentes (Module 3). Si DataInitializer ya crea un vehiculo para `owner@turbo.com` con status APPROVED y isActive=true, estos bookings referencian ese vehiculo.
