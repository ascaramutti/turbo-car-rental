# Module 6 — Reviews

## Decisiones de diseno

### Scope del modulo
Este modulo maneja las **reviews mutuas** entre drivers y car owners despues de bookings completados.
Cada booking puede tener hasta 2 reviews: una del driver al owner (DRIVER_TO_OWNER) y una del owner al driver (OWNER_TO_DRIVER).

**Justificacion (del Progress Report 2 — Assumptions 7-9, 14):**
- Assumption 7: Each Booking can have up to two Reviews (one from Driver, one from CarOwner)
- Assumption 8: Each Driver can write many Reviews for completed bookings (partial participation)
- Assumption 9: Each CarOwner can write many Reviews for completed bookings (partial participation)
- Assumption 14: Only Drivers and CarOwners can write Reviews. Admin cannot write Reviews.

### Domain name
- **Nombre:** `review`
- **Package:** `com.turbo.review`
- **Frontend module:** `src/modules/reviews`

---

## Enums

### ReviewType
**Package:** `com.turbo.review.model.enums`

| Valor | Significado |
|-------|-------------|
| `DRIVER_TO_OWNER` | Driver evalua al car owner y su vehiculo |
| `OWNER_TO_DRIVER` | Car owner evalua al driver |

---

## Error Codes

### ReviewErrorCode
**Package:** `com.turbo.exception.error`

| Codigo | Nombre | HTTP Status | Descripcion |
|--------|--------|-------------|-------------|
| REV-001 | BOOKING_NOT_FOUND | 404 | Booking no existe |
| REV-002 | BOOKING_NOT_COMPLETED | 400 | Solo bookings COMPLETED pueden recibir reviews |
| REV-003 | UNAUTHORIZED_REVIEW | 403 | El usuario no es driver ni owner del booking |
| REV-004 | REVIEW_ALREADY_SUBMITTED | 409 | El usuario ya envio review para este booking |
| REV-005 | REVIEW_NOT_FOUND | 404 | Review no encontrada |

---

## Entidades del modulo

### Review (nueva)
**Table name:** `reviews`
**Unique constraint:** `(booking_id, reviewer_id)` — maximo 1 review por usuario por booking

| # | Campo | Java Type | Columna | Nullable | Constraints |
|---|-------|-----------|---------|----------|-------------|
| 1 | `reviewId` | `Long` | `review_id` PK | No | `@Id`, `@GeneratedValue(IDENTITY)` |
| 2 | `booking` | `Booking` | `booking_id` FK | No | `@ManyToOne(LAZY)` |
| 3 | `reviewer` | `User` | `reviewer_id` FK | No | `@ManyToOne(LAZY)` — quien escribe la review |
| 4 | `reviewee` | `User` | `reviewee_id` FK | No | `@ManyToOne(LAZY)` — quien recibe la review |
| 5 | `vehicle` | `Vehicle` | `vehicle_id` FK | No | `@ManyToOne(LAZY)` |
| 6 | `reviewType` | `ReviewType` | `review_type` | No | `@Enumerated(STRING)` |
| 7 | `rating` | `Integer` | `rating` | No | 1-5 |
| 8 | `comment` | `String` | `comment` length=500 | Yes | `@Pattern(regexp = "^[a-zA-Z0-9 .,!?'\\-()]*$")` |
| 9 | `createdAt` | `LocalDateTime` | `created_at` | No | `@PrePersist`, `updatable=false` |

---

## Flujo completo

### Flujo 1: Crear review

```
Driver o CarOwner (autenticado con JWT, booking COMPLETED)
  |
  └─ POST /api/bookings/{bookingId}/reviews
     Body: { "bookingId": 5, "rating": 4, "comment": "Great experience" }

     → Valida:
       1. Booking existe (si no → REV-001)
       2. Booking.status == COMPLETED (si no → REV-002)
       3. Usuario es driver O owner del booking (si no → REV-003)
       4. Usuario no ha enviado review para este booking (si no → REV-004)
     → Determina automaticamente:
       - reviewer = usuario autenticado
       - reviewee = la otra parte
       - reviewType = DRIVER_TO_OWNER o OWNER_TO_DRIVER
       - vehicle = booking.vehicle
     → Respuesta: ReviewResponse
```

### Flujo 2: Ver reviews de un booking

```
GET /api/bookings/{bookingId}/reviews
→ Lista 0-2 reviews del booking (driver y/o owner)
→ Cualquier usuario autenticado puede ver
```

### Flujo 3: Ver reviews de un usuario

```
GET /api/users/{userId}/reviews
→ Lista de reviews RECIBIDAS por el usuario (ordenadas por fecha desc)
→ Usada en la pagina "My Reviews" del owner
```

---

## Frontend

### Componentes
- `StarRating.jsx` — Rating interactivo de 1-5 estrellas (reutilizable)
- `ReviewCard.jsx` — Card de review individual (reviewer, rating, comment, fecha)
- `ReviewModal.jsx` — Modal para enviar review (rating + comment con validacion onBlur)

### Paginas
- `MyReviewsPage.jsx` — Pagina `/owner/reviews` con resumen y lista de reviews recibidas

### Integracion en DriverBookingDetailPage
- Boton "Leave a Review" visible solo para bookings COMPLETED donde el driver aun no ha hecho review
- Abre ReviewModal → submit → refetch booking reviews

### Constantes
- `reviewConstants.js` — REVIEW_TYPE, REVIEW_TYPE_LABELS, REVIEW_CONSTRAINTS, REVIEW_ERROR_MESSAGES
