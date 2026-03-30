# Module 4 — Booking Orchestrator (Technical Brief)

> **Status:** Not started
> **Branch:** `module-4/booking-orchestrator`
> **Domain name:** `booking` (package: `com.turbo.booking`)
> **Depends on:** Module 1 (Auth), Module 2 (Document Verification), Module 3 (Vehicle Catalog)

---

## 1. Module Summary and Purpose

The Booking Orchestrator is the core transactional module of TURBO. It enables verified Drivers to search available vehicles, create shift-based bookings (minimum 4 hours), and follow a complete booking lifecycle from request through completion. Car Owners receive booking requests and can confirm or reject them. The module also handles pickup/return photo documentation and location reveal logic (exact coordinates disclosed 2 hours before a confirmed booking).

This module connects the "supply" side (vehicles registered and activated by Car Owners in Module 3) with the "demand" side (verified Drivers from Module 2) to form the marketplace transaction.

---

## 2. Entities

### 2.1 Booking (new entity)

**Table name:** `bookings`

| Field | Java Type | Column | Nullable | Constraints | Description |
|-------|-----------|--------|----------|-------------|-------------|
| `bookingId` | `Long` | `booking_id` PK | No | `@GeneratedValue(IDENTITY)` | Auto-generated primary key |
| `driver` | `Driver` | `driver_id` FK -> `users` | No | `@ManyToOne(LAZY)` | The driver who requested the booking |
| `vehicle` | `Vehicle` | `vehicle_id` FK -> `vehicles` | No | `@ManyToOne(LAZY)` | The vehicle being booked |
| `status` | `BookingStatus` | `status` | No | `@Enumerated(STRING)`, default `PENDING` | Current booking state |
| `startTime` | `LocalDateTime` | `start_time` | No | Must be in the future at creation | Shift start datetime |
| `endTime` | `LocalDateTime` | `end_time` | No | Must be >= startTime + 4 hours | Shift end datetime |
| `totalHours` | `Integer` | `total_hours` | No | Derived: `Duration.between(startTime, endTime).toHours()` | Total booked hours |
| `totalPrice` | `BigDecimal` | `total_price` precision=10, scale=2 | No | `totalHours * vehicle.hourlyRate` | Computed booking cost |
| `pickupLocation` | `String` | `pickup_location` length=200 | Yes | Set at activation step | Human-readable pickup location |
| `pickupLatitude` | `Double` | `pickup_latitude` | Yes | Revealed 2h before start | Exact latitude for pickup |
| `pickupLongitude` | `Double` | `pickup_longitude` | Yes | Revealed 2h before start | Exact longitude for pickup |
| `cancellationReason` | `String` | `cancellation_reason` length=500 | Yes | Required when status transitions to CANCELLED | Reason for cancellation |
| `cancelledBy` | `String` | `cancelled_by` | Yes | `DRIVER` or `OWNER` | Who initiated the cancellation |
| `confirmedAt` | `LocalDateTime` | `confirmed_at` | Yes | Set when owner confirms | Timestamp of owner confirmation |
| `startedAt` | `LocalDateTime` | `started_at` | Yes | Set when driver starts the shift | Timestamp of shift start |
| `completedAt` | `LocalDateTime` | `completed_at` | Yes | Set on completion | Timestamp of shift completion |
| `cancelledAt` | `LocalDateTime` | `cancelled_at` | Yes | Set on cancellation | Timestamp of cancellation |
| `createdAt` | `LocalDateTime` | `created_at` | No | `@PrePersist`, `updatable = false` | Record creation timestamp |
| `updatedAt` | `LocalDateTime` | `updated_at` | No | `@PrePersist` + `@PreUpdate` | Last modification timestamp |

### 2.2 BookingStatus (new enum)

**Package:** `com.turbo.booking.model.enums`

| Value | Meaning | Transitions from |
|-------|---------|-----------------|
| `PENDING` | Booking requested by driver, awaiting owner decision | (initial state) |
| `CONFIRMED` | Owner accepted the booking | PENDING |
| `IN_PROGRESS` | Driver has started the shift (picked up vehicle) | CONFIRMED |
| `COMPLETED` | Shift finished, vehicle returned | IN_PROGRESS |
| `CANCELLED` | Cancelled by either party | PENDING, CONFIRMED |
| `REJECTED` | Owner rejected the booking request | PENDING |

**State machine diagram:**
```
PENDING ----[owner confirms]----> CONFIRMED ----[driver starts shift]----> IN_PROGRESS ----[shift ends]----> COMPLETED
  |                                   |
  |---[owner rejects]----> REJECTED   |---[either cancels]----> CANCELLED
  |
  |---[either cancels]----> CANCELLED
```

---

## 3. Complete Endpoint List

### 3.1 Driver Endpoints (`/api/driver/bookings`)

Secured by: `hasAuthority(DRIVER)` (via SecurityConfig `/api/driver/**` rule)

| # | Method | Path | Description | Request Body | Success Response | Auth |
|---|--------|------|-------------|-------------|-----------------|------|
| 1 | `GET` | `/api/driver/bookings/vehicles/search` | Search available vehicles | Query params (see below) | `List<VehicleSearchResponse>` | DRIVER |
| 2 | `GET` | `/api/driver/bookings/vehicles/{vehicleId}` | View vehicle details for booking | - | `VehicleBookingDetailResponse` | DRIVER |
| 3 | `POST` | `/api/driver/bookings` | Create a new booking | `CreateBookingRequest` | `BookingResponse` | DRIVER |
| 4 | `GET` | `/api/driver/bookings` | List my bookings (as driver) | Query: `?status=` (optional) | `List<BookingResponse>` | DRIVER |
| 5 | `GET` | `/api/driver/bookings/{bookingId}` | Get booking details | - | `BookingDetailResponse` | DRIVER |
| 6 | `PUT` | `/api/driver/bookings/{bookingId}/cancel` | Cancel a pending/confirmed booking | `CancelBookingRequest` | `BookingResponse` | DRIVER |
| 7 | `PUT` | `/api/driver/bookings/{bookingId}/start` | Start shift (pickup) | `multipart/form-data` (1-10 pickup photos) | `BookingResponse` | DRIVER |
| 8 | `PUT` | `/api/driver/bookings/{bookingId}/complete` | Complete shift (return) | `multipart/form-data` (1-10 return photos) | `BookingResponse` | DRIVER |
| 9 | `GET` | `/api/driver/bookings/{bookingId}/vehicle-location` | Get vehicle location (masked or exact) | - | `VehicleLocationResponse` | DRIVER |

### 3.2 Car Owner Endpoints (`/api/owner/bookings`)

Secured by: `hasAuthority(CAR_OWNER)` (via SecurityConfig `/api/owner/**` rule)

| # | Method | Path | Description | Request Body | Success Response | Auth |
|---|--------|------|-------------|-------------|-----------------|------|
| 10 | `GET` | `/api/owner/bookings` | List bookings for my vehicles | Query: `?status=`, `?vehicleId=` (optional) | `List<BookingResponse>` | CAR_OWNER |
| 11 | `GET` | `/api/owner/bookings/{bookingId}` | Get booking details | - | `BookingDetailResponse` | CAR_OWNER |
| 12 | `PUT` | `/api/owner/bookings/{bookingId}/confirm` | Confirm a pending booking | - | `BookingResponse` | CAR_OWNER |
| 13 | `PUT` | `/api/owner/bookings/{bookingId}/reject` | Reject a pending booking | `RejectBookingRequest` | `BookingResponse` | CAR_OWNER |
| 14 | `PUT` | `/api/owner/bookings/{bookingId}/cancel` | Cancel a confirmed booking | `CancelBookingRequest` | `BookingResponse` | CAR_OWNER |

### 3.3 Admin Endpoints (`/api/admin/bookings`)

Secured by: `hasAuthority(ADMIN)` (via SecurityConfig `/api/admin/**` rule)

| # | Method | Path | Description | Request Body | Success Response | Auth |
|---|--------|------|-------------|-------------|-----------------|------|
| 15 | `GET` | `/api/admin/bookings` | List all bookings (platform overview) | Query: `?status=`, `?driverId=`, `?vehicleId=` | `List<AdminBookingResponse>` | ADMIN |
| 16 | `GET` | `/api/admin/bookings/{bookingId}` | Get full booking details (with photos) | - | `AdminBookingDetailResponse` | ADMIN |

---

## 4. Request and Response DTOs

### 4.1 Request DTOs

#### `CreateBookingRequest`

| Field | Java Type | Required | Constraints | Description |
|-------|-----------|----------|-------------|-------------|
| `vehicleId` | `Long` | Yes | `@NotNull` | ID of the vehicle to book |
| `startTime` | `LocalDateTime` | Yes | `@NotNull`, must be future, must be within vehicle's `availableUntil` | Shift start datetime |
| `endTime` | `LocalDateTime` | Yes | `@NotNull`, must be >= `startTime + 4h` | Shift end datetime |

#### `CancelBookingRequest`

| Field | Java Type | Required | Constraints | Description |
|-------|-----------|----------|-------------|-------------|
| `reason` | `String` | Yes | `@NotBlank`, `@Size(max=500)` | Reason for cancellation |

#### `RejectBookingRequest`

| Field | Java Type | Required | Constraints | Description |
|-------|-----------|----------|-------------|-------------|
| `reason` | `String` | Yes | `@NotBlank`, `@Size(max=500)` | Reason for rejection |

#### Vehicle Search Query Parameters

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `latitude` | `Double` | No | - | Driver's current latitude for proximity search |
| `longitude` | `Double` | No | - | Driver's current longitude for proximity search |
| `radiusKm` | `Double` | No | `10.0` | Search radius in kilometers |
| `startTime` | `LocalDateTime` | No | - | Desired shift start time (filters out conflicting bookings) |
| `endTime` | `LocalDateTime` | No | - | Desired shift end time (filters out conflicting bookings) |
| `category` | `String` | No | - | Vehicle category filter (SEDAN, SUV, VAN, TRUCK, COMPACT) |
| `serviceType` | `String` | No | - | Service type filter (TAXI_AND_DELIVERY, DELIVERY_ONLY) |
| `minPrice` | `BigDecimal` | No | - | Minimum hourly rate |
| `maxPrice` | `BigDecimal` | No | - | Maximum hourly rate |
| `fuelType` | `String` | No | - | Fuel type filter |

### 4.2 Response DTOs

#### `BookingResponse`

| Field | Java Type | Description |
|-------|-----------|-------------|
| `bookingId` | `Long` | Booking ID |
| `driverId` | `Long` | Driver user ID |
| `driverFullName` | `String` | Driver's first + last name |
| `vehicleId` | `Long` | Vehicle ID |
| `vehicleSummary` | `String` | `"year make model"` (e.g., `"2022 Toyota Corolla"`) |
| `status` | `String` | Current booking status |
| `startTime` | `String` | ISO datetime string |
| `endTime` | `String` | ISO datetime string |
| `totalHours` | `Integer` | Total booked hours |
| `totalPrice` | `BigDecimal` | Computed total cost |
| `createdAt` | `String` | ISO datetime string |
| `updatedAt` | `String` | ISO datetime string |

#### `BookingDetailResponse` (extends BookingResponse)

| Field | Java Type | Description |
|-------|-----------|-------------|
| (all fields from `BookingResponse`) | | |
| `ownerId` | `Long` | Vehicle owner ID |
| `ownerFullName` | `String` | Owner's first + last name |
| `vehicleMake` | `String` | Vehicle make |
| `vehicleModel` | `String` | Vehicle model |
| `vehicleYear` | `Integer` | Vehicle year |
| `vehicleLicensePlate` | `String` | License plate |
| `vehicleCategory` | `String` | Vehicle category |
| `vehicleServiceType` | `String` | Service type |
| `vehicleHourlyRate` | `BigDecimal` | Hourly rate |
| `pickupLocation` | `String` | Pickup location (masked or exact depending on time) |
| `pickupPhotoUrls` | `List<String>` | URLs for pickup condition photos (empty list until shift starts) |
| `returnPhotoUrls` | `List<String>` | URLs for return condition photos (empty list until shift completes) |
| `cancellationReason` | `String` | Reason for cancellation (nullable) |
| `cancelledBy` | `String` | Who cancelled (nullable) |
| `confirmedAt` | `String` | ISO datetime string (nullable) |
| `startedAt` | `String` | ISO datetime string (nullable) |
| `completedAt` | `String` | ISO datetime string (nullable) |
| `cancelledAt` | `String` | ISO datetime string (nullable) |

#### `VehicleSearchResponse`

| Field | Java Type | Description |
|-------|-----------|-------------|
| `vehicleId` | `Long` | Vehicle ID |
| `make` | `String` | Vehicle make |
| `model` | `String` | Vehicle model |
| `year` | `Integer` | Vehicle year |
| `category` | `String` | Vehicle category |
| `fuelType` | `String` | Fuel type |
| `serviceType` | `String` | Service type classification |
| `hourlyRate` | `BigDecimal` | Hourly rental rate |
| `description` | `String` | Vehicle description |
| `generalLocation` | `String` | Masked general location (e.g., "Downtown Vancouver") |
| `maskedLatitude` | `Double` | Latitude rounded to ~1km radius |
| `maskedLongitude` | `Double` | Longitude rounded to ~1km radius |
| `availableUntil` | `String` | ISO datetime string |
| `ownerFullName` | `String` | Owner's display name |
| `ownerRating` | `Float` | Owner's average rating |
| `effectiveServiceType` | `String` | Effective service type based on driver's license class + vehicle classification |
| `serviceTypeWarning` | `String` | Informational warning if driver's license restricts usage (nullable, null = no warning) |

#### `VehicleBookingDetailResponse`

| Field | Java Type | Description |
|-------|-----------|-------------|
| (all fields from `VehicleSearchResponse`) | | |
| `licensePlate` | `String` | License plate (visible only for specific views) |
| `ownerId` | `Long` | Owner user ID |
| `vin` | `String` | VIN (omitted in search, included in detail) |

#### `VehicleLocationResponse`

| Field | Java Type | Description |
|-------|-----------|-------------|
| `vehicleId` | `Long` | Vehicle ID |
| `isExactLocation` | `Boolean` | Whether exact coordinates are revealed |
| `generalLocation` | `String` | Human-readable area name |
| `latitude` | `Double` | Masked or exact latitude |
| `longitude` | `Double` | Masked or exact longitude |
| `message` | `String` | e.g., "Exact location will be available 2 hours before your booking" |

#### `AdminBookingResponse` (extends BookingResponse)

| Field | Java Type | Description |
|-------|-----------|-------------|
| (all fields from `BookingResponse`) | | |
| `ownerId` | `Long` | Vehicle owner ID |
| `ownerFullName` | `String` | Owner's full name |
| `vehicleLicensePlate` | `String` | Vehicle plate |

---

## 5. Service Layer Commands

Following the existing pattern (e.g., `RegisterVehicleCommand`, `ActivateVehicleCommand`), each service operation uses a command object:

| Command | Fields | Maps from |
|---------|--------|-----------|
| `CreateBookingCommand` | `driverId`, `vehicleId`, `startTime`, `endTime` | `CreateBookingRequest` + SecurityHelper |
| `ConfirmBookingCommand` | `bookingId`, `ownerId` | path var + SecurityHelper |
| `RejectBookingCommand` | `bookingId`, `ownerId`, `reason` | `RejectBookingRequest` + SecurityHelper |
| `CancelBookingCommand` | `bookingId`, `userId`, `userRole`, `reason` | `CancelBookingRequest` + SecurityHelper |
| `StartBookingCommand` | `bookingId`, `driverId`, `pickupPhotos` (List<MultipartFile>) | multipart + SecurityHelper |
| `CompleteBookingCommand` | `bookingId`, `driverId`, `returnPhotos` (List<MultipartFile>) | multipart + SecurityHelper |
| `GetBookingCommand` | `bookingId`, `userId`, `userRole` | path var + SecurityHelper |
| `SearchVehiclesCommand` | `driverId`, `latitude`, `longitude`, `radiusKm`, `startTime`, `endTime`, `category`, `serviceType`, `minPrice`, `maxPrice`, `fuelType` | query params + SecurityHelper |

---

## 6. User Flows by Role

### 6.1 Driver Flow

```
Driver (authenticated, isVerified=true)
  |
  |-- Step 1: Search available vehicles
  |   GET /api/driver/bookings/vehicles/search?lat=49.28&lng=-123.12&radiusKm=10
  |   -> Returns list of available vehicles with masked locations
  |   -> Only vehicles where isActive=true AND availableUntil > now() AND status=APPROVED
  |   -> Coordinates masked to ~1km radius
  |
  |-- Step 2: View vehicle details
  |   GET /api/driver/bookings/vehicles/{vehicleId}
  |   -> Full vehicle info (still masked location)
  |   -> Shows owner name and rating
  |
  |-- Step 3: Create booking
  |   POST /api/driver/bookings
  |   Body: { vehicleId: 5, startTime: "2026-03-20T08:00:00", endTime: "2026-03-20T16:00:00" }
  |   -> Validates: driver is verified, vehicle is available, no time conflict, min 4h
  |   -> Booking.status = PENDING
  |   -> totalPrice = 8h * vehicle.hourlyRate
  |   -> Response: BookingResponse
  |
  |-- Step 4: Wait for owner decision (PENDING)
  |   GET /api/driver/bookings
  |   -> Dashboard shows booking with status PENDING
  |
  |-- Step 5a: Booking confirmed by owner
  |   -> Status changes to CONFIRMED
  |   -> 2h before startTime: exact location revealed
  |   GET /api/driver/bookings/{id}/vehicle-location
  |   -> If <= 2h before start: exact lat/lng
  |   -> If > 2h before start: masked lat/lng + message
  |
  |-- Step 5b: Booking rejected by owner
  |   -> Status changes to REJECTED
  |   -> Driver sees rejection reason in booking detail
  |
  |-- Step 6: Start shift (pickup)
  |   PUT /api/driver/bookings/{id}/start
  |   Body: multipart/form-data with 1-10 pickup photos
  |   -> Validates: status must be CONFIRMED, current time >= startTime (or within 15min early)
  |   -> Booking.status = IN_PROGRESS
  |   -> pickupPhotoUrls saved in booking_photos table
  |
  |-- Step 7: Complete shift (return)
  |   PUT /api/driver/bookings/{id}/complete
  |   Body: multipart/form-data with 1-10 return photos
  |   -> Validates: status must be IN_PROGRESS
  |   -> Booking.status = COMPLETED
  |   -> returnPhotoUrls saved in booking_photos table
  |   -> completedAt = now()
  |
  |-- Alternative: Cancel booking
  |   PUT /api/driver/bookings/{id}/cancel
  |   Body: { reason: "Schedule conflict" }
  |   -> Validates: status must be PENDING or CONFIRMED
  |   -> Booking.status = CANCELLED, cancelledBy = DRIVER
```

### 6.2 Car Owner Flow

```
CarOwner (authenticated, email verified)
  |
  |-- Step 1: View incoming booking requests
  |   GET /api/owner/bookings?status=PENDING
  |   -> Lists all PENDING bookings for owner's vehicles
  |   -> Shows driver name, vehicle, dates, total price
  |
  |-- Step 2a: Confirm booking
  |   PUT /api/owner/bookings/{id}/confirm
  |   -> Validates: status must be PENDING, vehicle is still active
  |   -> Booking.status = CONFIRMED
  |   -> confirmedAt = now()
  |
  |-- Step 2b: Reject booking
  |   PUT /api/owner/bookings/{id}/reject
  |   Body: { reason: "Vehicle is unavailable for those dates" }
  |   -> Validates: status must be PENDING
  |   -> Booking.status = REJECTED
  |
  |-- Step 3: Monitor active bookings
  |   GET /api/owner/bookings?status=IN_PROGRESS
  |   -> See which vehicles are currently in use
  |
  |-- Step 4: View completed bookings
  |   GET /api/owner/bookings?status=COMPLETED
  |   -> See booking history with pickup/return photos
  |
  |-- Alternative: Cancel confirmed booking
  |   PUT /api/owner/bookings/{id}/cancel
  |   Body: { reason: "Emergency - vehicle needs repairs" }
  |   -> Validates: status must be CONFIRMED (cannot cancel IN_PROGRESS)
  |   -> Booking.status = CANCELLED, cancelledBy = OWNER
```

### 6.3 Admin Flow

```
Admin (authenticated)
  |
  |-- Step 1: View all bookings (platform overview)
  |   GET /api/admin/bookings
  |   -> Lists all bookings across the platform
  |   -> Filterable by status, driverId, vehicleId
  |
  |-- Step 2: View booking details
  |   GET /api/admin/bookings/{id}
  |   -> Full details including pickup/return photos
  |   -> Driver info, owner info, vehicle info
  |   -> Used for dispute resolution
```

---

## 7. Dependencies with Completed Modules

### 7.1 Module 1 — Auth & Security

| What | Where | How |
|------|-------|-----|
| JWT authentication | `com.turbo.config.JwtAuthFilter` | All endpoints require valid JWT token |
| User resolution | `com.turbo.config.SecurityHelper.getCurrentUser()` | Resolves `User` entity from SecurityContext in every controller |
| Role-based access | `com.turbo.config.SecurityConfig` | `/api/driver/**` -> DRIVER, `/api/owner/**` -> CAR_OWNER, `/api/admin/**` -> ADMIN |
| Error codes | `com.turbo.exception.error.AuthErrorCode.USER_NOT_FOUND` | When user referenced in booking no longer exists |

### 7.2 Module 2 — Document Verification

| What | Where | How |
|------|-------|-----|
| Driver verification | `com.turbo.user.model.User.isVerified` | Booking creation requires `driver.getIsVerified() == true` |
| Driver entity | `com.turbo.user.model.Driver` | FK in Booking. Fields: `licenseClass`, `isWorkEligible`, `rating` |
| File storage pattern | `com.turbo.document.service.FileStorageService` | Reuse for pickup/return photo storage |
| File response helper | `com.turbo.document.util.FileResponseHelper` | Reuse for serving photo files |

### 7.3 Module 3 — Vehicle Catalog

| What | Where | How |
|------|-------|-----|
| Vehicle entity | `com.turbo.vehicle.model.Vehicle` | FK in Booking. Fields: `hourlyRate`, `isActive`, `availableUntil`, `status`, `serviceType`, `generalLocation`, `latitude`, `longitude` |
| Vehicle repository | `com.turbo.vehicle.repository.VehicleRepository` | Query available vehicles for search |
| Vehicle enums | `com.turbo.vehicle.model.enums.*` | `VehicleStatus.APPROVED`, `VehicleCategory`, `FuelType`, `ServiceType` for filters |
| CarOwner entity | `com.turbo.user.model.CarOwner` | Accessed via `vehicle.getOwner()` for owner info in responses |
| Location masking logic | (new) `com.turbo.booking.service.LocationMaskService` | Masks vehicle coordinates in search results |

---

## 8. Business Constraints and Validations

### 8.1 Booking Creation

| # | Rule | Error if violated |
|---|------|------------------|
| 1 | Driver must be verified (`isVerified == true`) | `BOOK-001` |
| 2 | Vehicle must exist | `VEH-004` (reuse) |
| 3 | Vehicle must be approved (`status == APPROVED`) | `BOOK-002` |
| 4 | Vehicle must be active (`isActive == true`) | `BOOK-003` |
| 5 | Vehicle `availableUntil` must be >= booking `endTime` | `BOOK-004` |
| 6 | `startTime` must be in the future | `BOOK-005` |
| 7 | `endTime` must be after `startTime` | `BOOK-006` |
| 8 | Minimum shift duration: 4 hours (`endTime - startTime >= 4h`) | `BOOK-007` |
| 8b | Maximum shift duration: 24 hours (`endTime - startTime <= 24h`) — study permit restriction | `BOOK-025` |
| 9 | No overlapping PENDING/CONFIRMED/IN_PROGRESS bookings for the same vehicle in the same time range | `BOOK-008` |
| 10 | Driver cannot book their own vehicle | `BOOK-009` |
| 11 | Driver cannot have overlapping active bookings (across any vehicle) | `BOOK-010` |

### 8.2 State Transitions

| Transition | Who can trigger | Additional rules |
|-----------|----------------|-----------------|
| PENDING -> CONFIRMED | CAR_OWNER (vehicle owner) | Vehicle must still be active |
| PENDING -> REJECTED | CAR_OWNER (vehicle owner) | Rejection reason required |
| PENDING -> CANCELLED | DRIVER or CAR_OWNER | Cancellation reason required |
| CONFIRMED -> IN_PROGRESS | DRIVER | Must be within booking time window (startTime - 15min to startTime + 1h), at least one pickup photo required (max 10) |
| CONFIRMED -> CANCELLED | DRIVER or CAR_OWNER | Cancellation reason required |
| IN_PROGRESS -> COMPLETED | DRIVER | At least one return photo required (max 10) |
| CANCELLED/REJECTED/COMPLETED | (terminal) | No further transitions |

### 8.3 Location Masking

| Condition | Latitude/Longitude returned |
|-----------|---------------------------|
| Vehicle search (any time) | Masked (~1km radius): `Math.round(lat * 100) / 100.0` |
| Booking detail, > 2h before startTime | Masked (~1km radius) |
| Booking detail, <= 2h before startTime AND status is CONFIRMED/IN_PROGRESS | Exact coordinates |
| Booking status is COMPLETED | Exact coordinates (for reference) |

### 8.4 Pickup/Return Photos

| Rule | Description |
|------|-------------|
| Quantity | Min 1 photo, Max 10 photos per event (pickup or return) |
| Format | JPG, JPEG, PNG (photos only) |
| Max size | 5MB per photo |
| Storage | Docker volume, same pattern as `FileStorageService` in Module 2 |
| Subdirectory pickup | `bookings/{bookingId}/pickup/` |
| Subdirectory return | `bookings/{bookingId}/return/` |

### 8.5 Price Calculation

```
totalPrice = totalHours * vehicle.hourlyRate
totalHours = Duration.between(startTime, endTime).toHours()
```

- `BigDecimal` precision: `(10, 2)` matching `Vehicle.hourlyRate`
- Price is computed and stored at booking creation; changes to `hourlyRate` after booking do not affect existing bookings

---

## 9. Planned Error Codes

**Prefix:** `BOOK-XXX`

| Code | HTTP | Message | When |
|------|------|---------|------|
| `BOOK-001` | 403 | Driver must be verified before creating bookings | `driver.getIsVerified() == false` |
| `BOOK-002` | 400 | Vehicle is not approved for rentals | `vehicle.getStatus() != APPROVED` |
| `BOOK-003` | 400 | Vehicle is not currently available for rent | `vehicle.getIsActive() == false` |
| `BOOK-004` | 400 | Booking end time exceeds vehicle availability window | `endTime > vehicle.getAvailableUntil()` |
| `BOOK-005` | 400 | Booking start time must be in the future | `startTime <= now()` |
| `BOOK-006` | 400 | Booking end time must be after start time | `endTime <= startTime` |
| `BOOK-007` | 400 | Minimum booking duration is 4 hours | `endTime - startTime < 4h` |
| `BOOK-008` | 409 | Vehicle is already booked for the requested time slot | Overlapping booking exists for vehicle |
| `BOOK-009` | 400 | You cannot book your own vehicle | `vehicle.getOwner().getUserId().equals(driverId)` |
| `BOOK-010` | 409 | You already have an active booking during this time slot | Driver has overlapping booking |
| `BOOK-011` | 404 | Booking not found | `bookingId` does not exist |
| `BOOK-012` | 403 | You do not have permission to access this booking | Booking doesn't belong to requesting user |
| `BOOK-013` | 400 | Only pending bookings can be confirmed | `status != PENDING` on confirm |
| `BOOK-014` | 400 | Only pending bookings can be rejected | `status != PENDING` on reject |
| `BOOK-015` | 400 | Only pending or confirmed bookings can be cancelled | `status != PENDING && status != CONFIRMED` on cancel |
| `BOOK-016` | 400 | Only confirmed bookings can be started | `status != CONFIRMED` on start |
| `BOOK-017` | 400 | Booking cannot be started yet (too early) | More than 15 minutes before `startTime` |
| `BOOK-018` | 400 | Booking start window has expired | More than 1 hour after `startTime` without starting |
| `BOOK-019` | 400 | Only in-progress bookings can be completed | `status != IN_PROGRESS` on complete |
| `BOOK-020` | 400 | At least one pickup photo is required to start the shift | Missing photo on start |
| `BOOK-021` | 400 | At least one return photo is required to complete the shift | Missing photo on complete |
| `BOOK-022` | 400 | Cancellation reason is required | Missing reason on cancel |
| `BOOK-023` | 400 | Rejection reason is required | Missing reason on reject |
| `BOOK-025` | 400 | Maximum booking duration is 24 hours | `endTime - startTime > 24h` |
| `BOOK-026` | 400 | Maximum 10 photos allowed per upload | More than 10 files in start/complete |

**Error codes reused from other modules:**
- `VEH-004` — Vehicle not found (from `com.turbo.exception.error.VehicleErrorCode`)
- `AUTH-002` — User not found (from `com.turbo.exception.error.AuthErrorCode`)
- `DOC-001` — Invalid file format (from `com.turbo.exception.error.DocumentErrorCode`)
- `DOC-002` — File too large (from `com.turbo.exception.error.DocumentErrorCode`)

---

## 10. Backend File Structure

Following the existing pattern observed in modules 1-3:

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
  |   |-- LocationMaskService.java               # Interface for location masking logic
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

## 11. Frontend File Structure

Following the existing pattern observed in modules 1-3:

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
  |   |-- BookingCard.jsx                        # Booking summary card (reused in lists)
  |   |-- BookingStatusBadge.jsx                 # Colored status badge
  |   |-- VehicleSearchCard.jsx                  # Vehicle card in search results
  |   |-- BookingForm.jsx                        # Date/time picker for creating a booking
  |   |-- PhotoUpload.jsx                        # Pickup/return photo upload component
  |   |-- LocationMap.jsx                        # Map component showing masked/exact location
  |
  |-- pages/
  |   |-- DriverSearchPage.jsx                   # Vehicle search page for drivers
  |   |-- DriverBookingsPage.jsx                 # Driver's booking list/dashboard
  |   |-- DriverBookingDetailPage.jsx            # Single booking detail (driver view)
  |   |-- OwnerBookingsPage.jsx                  # Owner's incoming bookings list
  |   |-- OwnerBookingDetailPage.jsx             # Single booking detail (owner view)
  |
  |-- utils/
  |   |-- bookingValidation.js                   # Frontend validation (min 4h, future dates, etc.)
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

## 12. Repository Queries

### `BookingRepository` (`JpaRepository<Booking, Long>`)

```java
// Driver's bookings
List<Booking> findByDriverUserId(Long driverId);
List<Booking> findByDriverUserIdAndStatus(Long driverId, BookingStatus status);

// Owner's bookings (all vehicles owned by this user)
List<Booking> findByVehicleOwnerUserId(Long ownerId);
List<Booking> findByVehicleOwnerUserIdAndStatus(Long ownerId, BookingStatus status);
List<Booking> findByVehicleVehicleIdAndVehicleOwnerUserId(Long vehicleId, Long ownerId);

// Conflict check: overlapping bookings for a vehicle
@Query("SELECT b FROM Booking b WHERE b.vehicle.vehicleId = :vehicleId " +
       "AND b.status IN :activeStatuses " +
       "AND b.startTime < :endTime AND b.endTime > :startTime")
List<Booking> findConflictingBookings(
    @Param("vehicleId") Long vehicleId,
    @Param("activeStatuses") List<BookingStatus> activeStatuses,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime
);

// Conflict check: overlapping bookings for a driver
@Query("SELECT b FROM Booking b WHERE b.driver.userId = :driverId " +
       "AND b.status IN :activeStatuses " +
       "AND b.startTime < :endTime AND b.endTime > :startTime")
List<Booking> findDriverConflictingBookings(
    @Param("driverId") Long driverId,
    @Param("activeStatuses") List<BookingStatus> activeStatuses,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime
);

// Admin: all bookings with optional filters
List<Booking> findAll();
List<Booking> findByStatus(BookingStatus status);
```

### Additional query needed in `VehicleRepository`

```java
// Search available vehicles: active, approved, available, within radius
@Query("SELECT v FROM Vehicle v WHERE v.isActive = true " +
       "AND v.status = 'APPROVED' " +
       "AND v.availableUntil > :now " +
       "AND (:category IS NULL OR v.category = :category) " +
       "AND (:serviceType IS NULL OR v.serviceType = :serviceType) " +
       "AND (:fuelType IS NULL OR v.fuelType = :fuelType) " +
       "AND (:minPrice IS NULL OR v.hourlyRate >= :minPrice) " +
       "AND (:maxPrice IS NULL OR v.hourlyRate <= :maxPrice)")
List<Vehicle> searchAvailableVehicles(
    @Param("now") LocalDateTime now,
    @Param("category") VehicleCategory category,
    @Param("serviceType") ServiceType serviceType,
    @Param("fuelType") FuelType fuelType,
    @Param("minPrice") BigDecimal minPrice,
    @Param("maxPrice") BigDecimal maxPrice
);
```

> **Note:** Geo-radius filtering will be done in-memory (Java) after the query, using the Haversine formula in `LocationMaskService`. This avoids needing spatial DB extensions for the pilot.

---

## 13. SecurityConfig Changes

The existing `SecurityConfig` already handles the URL patterns needed:

```java
.requestMatchers("/api/driver/**").hasAuthority("DRIVER")
.requestMatchers("/api/owner/**").hasAuthority("CAR_OWNER")
.requestMatchers("/api/admin/**").hasAuthority("ADMIN")
```

All new endpoints fall under these existing patterns, so **no changes to SecurityConfig are needed**.

---

## 14. Frontend Routes (additions to App.jsx)

| Path | Component | Role | Description |
|------|-----------|------|-------------|
| `/driver/search` | `DriverSearchPage` | DRIVER | Vehicle search and browsing |
| `/driver/bookings` | `DriverBookingsPage` | DRIVER | Driver's booking dashboard |
| `/driver/bookings/:id` | `DriverBookingDetailPage` | DRIVER | Booking detail with actions |
| `/owner/bookings` | `OwnerBookingsPage` | CAR_OWNER | Owner's booking management |
| `/owner/bookings/:id` | `OwnerBookingDetailPage` | CAR_OWNER | Booking detail with confirm/reject |
| `/admin/bookings` | (future Module 8 scope) | ADMIN | Admin booking overview |

---

## 15. Resolved Design Decisions (formerly Open Questions)

| # | Decision | Rationale | Implementation |
|---|----------|-----------|---------------|
| 1 | **Max booking duration: 24 hours** | Study permit restricts drivers to 24 work hours. Drivers can create consecutive bookings for longer needs. | Add validation `BOOK-025`: `endTime - startTime > 24h` → error "Maximum booking duration is 24 hours" |
| 2 | **Auto-cancel PENDING when `startTime` arrives** | If the owner doesn't respond by the booking's `startTime` (LocalDateTime, includes hour), the booking is useless. Also covers the `availableUntil` case since `startTime < availableUntil` is enforced at creation. | `@Scheduled` task checks every 5 minutes for PENDING bookings where `startTime <= now()` → auto-cancel with reason "Auto-cancelled: owner did not respond before booking start time" |
| 3 | **Auto-complete IN_PROGRESS 2h after `endTime`** | Prevents bookings stuck in IN_PROGRESS forever. Flagged for admin review. | `@Scheduled` task: IN_PROGRESS bookings where `endTime + 2h <= now()` → auto-complete with flag |
| 4 | **Cancellation penalties deferred to Module 5** | Module 4 tracks the cancellation; Module 5 (Payments) applies financial consequences. | `cancelledBy` and `cancelledAt` fields stored for Module 5 to reference |
| 5 | **Multiple PENDING allowed, one CONFIRMED/IN_PROGRESS per slot** | Driver can "apply" to multiple vehicles but only one booking per time slot can be active. | Allow overlapping PENDING. On confirm → auto-reject other PENDING bookings for same driver overlapping the confirmed slot |
| 6 | **Email notifications on status changes** | Reuse `EmailService` from Module 1. | Notifications: booking created (→ owner), confirmed/rejected (→ driver), cancelled (→ both parties) |
| 7 | **Service type matching: informational, NOT a hard block** | Any driver can book any vehicle. The system informs them of the effective service type based on their license class + vehicle classification. CLASS_4 driver + DELIVERY_ONLY vehicle → warning "this vehicle is delivery only". CLASS_5 driver + TAXI_AND_DELIVERY vehicle → warning "your license restricts you to delivery only". Search can filter by vehicle classification. | Add `effectiveServiceType` and `serviceTypeWarning` fields to `VehicleSearchResponse` and `BookingDetailResponse`. No BOOK-024 error needed — it's not a block. |
| 8 | **Pickup/return photos: required** | Photos serve as condition evidence for dispute resolution. | Keep `BOOK-020` and `BOOK-021` error codes |

---

## 16. Data Initializer Additions

Update `com.turbo.config.DataInitializer` to seed sample bookings for testing:

| Booking | Driver | Vehicle | Status | Start | End |
|---------|--------|---------|--------|-------|-----|
| Booking #1 | driver@turbo.com | (first owner vehicle) | COMPLETED | yesterday 08:00 | yesterday 16:00 |
| Booking #2 | driver@turbo.com | (first owner vehicle) | CONFIRMED | tomorrow 08:00 | tomorrow 16:00 |
| Booking #3 | driver@turbo.com | (first owner vehicle) | PENDING | day after tomorrow 08:00 | day after tomorrow 12:00 |

> **Note:** Seed data depends on vehicles being seeded first (Module 3). If DataInitializer already seeds a vehicle for `owner@turbo.com` with status APPROVED and isActive=true, these bookings reference that vehicle.

---

## 17. Test Plan Outline

### Backend Tests (JUnit 5 + Mockito)

| Area | Tests |
|------|-------|
| `BookingServiceImpl` — creation | Happy path, driver not verified, vehicle not available, time conflict, min duration, own vehicle, driver conflict |
| `BookingServiceImpl` — confirm/reject | Happy path, wrong status, wrong owner |
| `BookingServiceImpl` — cancel | By driver, by owner, wrong status, missing reason |
| `BookingServiceImpl` — start/complete | Happy path, wrong status, too early, missing photo |
| `BookingServiceImpl` — search | Filters, location masking, availability window |
| `DriverBookingController` | All endpoints via MockMvc, auth checks |
| `OwnerBookingController` | All endpoints via MockMvc, auth checks |
| `AdminBookingController` | All endpoints via MockMvc, auth checks |
| `LocationMaskService` | Masking logic, 2h reveal logic |
| `BookingRepository` | Conflict queries, filter queries |

### Frontend Tests (Vitest + React Testing Library)

| Area | Tests |
|------|-------|
| `BookingCard` | Renders all statuses correctly, shows correct actions per status |
| `BookingForm` | Validation (min 4h, future dates), error display |
| `DriverSearchPage` | Renders search results, filters, empty state |
| `DriverBookingsPage` | Lists bookings, status filter, cancel action |
| `OwnerBookingsPage` | Lists bookings, confirm/reject actions |
| `bookingValidation.js` | All validation rules |

### E2E Tests (Playwright)

| Flow | Steps |
|------|-------|
| Driver creates booking | Login as driver -> search -> select vehicle -> fill dates -> submit -> verify PENDING |
| Owner confirms booking | Login as owner -> view pending -> confirm -> verify CONFIRMED |
| Owner rejects booking | Login as owner -> view pending -> reject with reason -> verify REJECTED |
| Driver starts and completes shift | Login as driver -> view confirmed booking -> start with photo -> complete with photo -> verify COMPLETED |
| Driver cancels booking | Login as driver -> view pending -> cancel with reason -> verify CANCELLED |
