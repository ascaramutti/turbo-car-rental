# TURBO — Module Tracking

## Overview

| # | Module | Branch | Backend | Frontend | Tests | Status |
|---|--------|--------|---------|----------|-------|--------|
| 1 | Auth & Security | `module-1/frontend` | Done | Done | 148 BE / 56 FE | Merged to `predevelop` |
| 2 | Document Verification | `module-2` | - | - | - | In progress |
| 3 | Vehicle Catalog | `module-3` | Done | Done | 296 BE / 216 FE / 22 E2E | In review |
| 4 | Booking Orchestrator | - | - | - | - | Not started |
| 5 | Transaction Processor | - | - | - | - | Not started |
| 6 | Reviews | - | - | - | - | Not started |

---

## Module 1 — Auth & Security
**Scope:** Registration, login, OTP verification, JWT, address fields (BC/Canada pilot)
**Key commits:**
- `d5aabe2` feat(ui): implement auth module frontend
- `519718d` feat(api): add address fields to user registration flow
- `bd3fc8d` feat(ui): add address fields to sign up form with BC/Canada defaults

**Entities:** User, Driver, CarOwner, Admin, Address (@Embeddable)
**Endpoints:** POST /api/auth/register, /login, /verify-otp, /resend-otp

---

## Module 2 — Document Verification (Driver Documents)
**Scope:** Driver personal document upload (license + study permit), admin review/approval
**Endpoints:** POST/GET/PUT /api/driver/documents/* (upload, my, reupload, file), GET/PUT /api/admin/documents/* (pending, review, user, file)
**Entities:** Document (with user_id, vehicle_id=null)
**Key flows:**
- Driver uploads DRIVERS_LICENSE + STUDY_PERMIT → status=PENDING → Admin reviews
- Admin approves with license class (CLASS_4 or CLASS_5) or rejects with reason
- Driver.isVerified=true ONLY when ALL required docs are APPROVED
- Rejected docs can be re-uploaded → status resets to PENDING
**Constraints:**
- File format: PDF/JPG/JPEG/PNG only
- Max file size: 5MB
- Upload validation < 10 seconds
- License classification: Class 4 → TAXI + DELIVERY, Class 5 → DELIVERY only
**Note:** Vehicle documents (INSURANCE, VEHICLE_REGISTRATION, INSPECTION_REPORT) are in Module 3

---

## Module 3 — Vehicle Catalog
**Branch:** `module-3`
**Scope:** Vehicle registration, vehicle documents (insurance/registration/inspection), admin vehicle approval with service type, activate/deactivate with availableUntil
**Endpoints:**
- `POST /api/owner/vehicles` — Register a new vehicle
- `GET /api/owner/vehicles` — List my vehicles
- `GET /api/owner/vehicles/{id}` — Get vehicle details
- `PUT /api/owner/vehicles/{id}` — Update vehicle
- `PUT /api/owner/vehicles/{vehicleId}/activate` — Activate vehicle with availableUntil
- `PUT /api/owner/vehicles/{vehicleId}/deactivate` — Remove vehicle from rent
- `POST /api/owner/vehicles/{vehicleId}/documents/upload` — Upload vehicle document
- `GET /api/owner/vehicles/{vehicleId}/documents` — List vehicle documents
- `GET /api/owner/vehicles/{vehicleId}/documents/{docId}/view` — View document inline
- `GET /api/owner/vehicles/{vehicleId}/documents/{docId}/download` — Download document
- `PUT /api/owner/vehicles/{vehicleId}/documents/{docId}/reupload` — Re-upload document
- `PUT /api/admin/vehicles/{id}/approve` — Admin approves vehicle with service type (no vehicle rejection; document rejection is handled by Module 2)
**Entities:** Vehicle, Document (with vehicle_id)
**Key flows:**
- CarOwner registers vehicle (VIN, make, model, year, plate, price, description)
- CarOwner uploads vehicle docs: INSURANCE, VEHICLE_REGISTRATION, INSPECTION_REPORT (per vehicle)
- Admin reviews vehicle docs via Module 2 endpoint
- Admin approves vehicle with service type (TAXI_AND_DELIVERY or DELIVERY_ONLY)
- CarOwner activates vehicle with availableUntil (lists for rent) — only after admin approval
- CarOwner deactivates vehicle (removes from rent)
- Vehicle is available when isActive=true AND availableUntil > now()
- Vehicle validation: <= 9 years old, unique VIN, unique plate
- Location masking: 1km radius until 2hrs before booking

---

## Module 4 — Booking Orchestrator
**Scope:** Shift booking (4h min), conflict resolution, pickup/return documentation
**Entities:** Booking
**Key flows:**
- Driver searches available vehicles (filters: zone, time, category, price)
- Booking creation with conflict check
- Pickup/return photo documentation
- Status lifecycle: PENDING → CONFIRMED → IN_PROGRESS → COMPLETED/CANCELLED

---

## Module 5 — Transaction Processor
**Scope:** Stripe payments (test mode), 80/20 commission split, security deposits
**Entities:** Payment
**Key flows:**
- Payment on booking confirmation ($50/4h, $100/8h, etc.)
- Commission: 80% owner payout, 20% platform fee
- BigDecimal precision (4 decimals)
- Security deposit handling

---

## Module 6 — Reviews
**Scope:** Mutual rating system (Driver ↔ CarOwner), post-booking reviews
**Entities:** Review
**Key flows:**
- Driver rates CarOwner after completed booking
- CarOwner rates Driver after completed booking
- Rating aggregation on profiles
- Only participants of a booking can review
