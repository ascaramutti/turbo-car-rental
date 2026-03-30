# Module 3 — Pending Improvements

List of improvements identified during development that were deferred for future implementation.

---

## 1. Google Maps Integration for Vehicle Location

**Current state:** The owner enters general location, latitude, and longitude manually as text/number inputs when activating a vehicle for rent.

**Improvement:** Replace manual inputs with an interactive map (Google Maps API or Leaflet + OpenStreetMap). The owner clicks on the map to set the vehicle location. The system auto-fills latitude, longitude, and general location from the selected point.

**Why deferred:** Requires external API key (Google) or additional library (Leaflet). The current manual input works for development and testing. The map integration is a UI/UX improvement, not a logic change.

**Impact:** Frontend only. No backend changes needed.

---

## 2. License Plate Change Resets Vehicle Approval

**Current state:** The owner can edit the vehicle description after approval. License plate editing is allowed but does not reset the vehicle status or invalidate documents.

**Improvement:** When the owner changes the license plate on an approved vehicle:
- Vehicle status resets from APPROVED to PENDING
- Vehicle is deactivated (isActive = false, availableUntil = null)
- VEHICLE_REGISTRATION document is invalidated (status reset to REJECTED with reason "License plate changed, please upload new registration")
- Owner must upload new registration document
- Admin must re-approve the vehicle

**Why deferred:** Complex flow that touches multiple modules (vehicle status, document status, admin review). Requires careful handling of edge cases (what if vehicle has active bookings?).

**Impact:** Backend service logic, document module integration, frontend UI updates.

---

## 3. Admin Portal for Vehicle Management

**Current state:** The admin can approve vehicles via API endpoint (`PUT /api/admin/vehicles/{id}/approve`), but there is no dedicated admin UI page for managing vehicles. The admin approval is only testable via curl/Postman.

**Improvement:** Create an admin page similar to the document verification portal (Module 2). The page would show:
- List of vehicles with PENDING status
- Vehicle details, documents, and owner information
- Approve button with service type selector (TAXI_AND_DELIVERY / DELIVERY_ONLY)
- Age-based restriction warning for vehicles older than 9 years

**Why deferred:** The admin portal was not part of the initial Module 3 scope. The backend endpoints exist and work. The frontend admin page can be built independently.

**Impact:** Frontend only. No backend changes needed.

---

## 4. Owner Dashboard

**Current state:** After login, the car owner is redirected to `/owner/dashboard` which is an empty page. The owner must manually navigate to `/owner/vehicles`.

**Improvement:** Create an owner dashboard showing:
- Summary of registered vehicles (total, active, pending)
- Quick links to vehicle management
- Future: booking requests, earnings summary (Modules 4-5)

**Why deferred:** Dashboard content depends on features from Modules 4 (Booking) and 5 (Payments). A basic version could be built now, but a complete dashboard makes more sense after those modules are implemented.

**Impact:** Frontend only. May need new backend endpoints for summary data.

---

## 5. Vehicle Edit Restrictions After Approval

**Current state:** All vehicle fields except VIN are editable after approval. Changes do not affect the approval status.

**Improvement:** After a vehicle is approved, restrict editable fields to only:
- Description (informational, does not affect documents)
- License plate (with approval reset — see item 2)

All other fields (make, model, year, category, fuel type) should be read-only since they represent fixed vehicle characteristics verified during the approval process.

**Why deferred:** Requires frontend form field disabling logic based on vehicle status, and backend validation to reject changes to restricted fields on approved vehicles.

**Impact:** Frontend (field disabling) + Backend (validation in UpdateVehicleCommand).

---

## 6. Improved Vehicle Activation (Slot Creation)

**Current state:** When the owner activates a vehicle, they enter `availableUntil`, `generalLocation`, `latitude`, `longitude`, and `hourlyRate` in simple text/number inputs on the vehicle detail page. There is no structured concept of a "slot" — the vehicle simply has a single availability window.

**Improvement:** Replace the current flat activation fields with a proper slot-based system:
- Allow the owner to create multiple availability slots (e.g., Monday 8am–6pm, Wednesday 10am–4pm)
- Each slot has its own start/end time, location, and hourly rate
- Calendar UI for visualizing and managing slots
- Recurring slots (weekly patterns)
- Conflict detection (no overlapping slots)

**Why deferred:** The current single-window approach works for MVP. A slot system requires a new `VehicleSlot` entity, dedicated endpoints (CRUD for slots), calendar UI components, and conflict validation logic. This is a significant feature that should be designed as its own module or sub-module.

**Impact:** Backend (new entity, repository, service, controller) + Frontend (calendar UI, slot management page).

---

## 7. Path Traversal Vulnerability in FileStorageServiceImpl (SECURITY — Technical Debt)

**Current state:** `FileStorageServiceImpl.load(String filePath)` in `com.turbo.document.service.impl` accepts arbitrary file paths without validating that the resolved path stays within the configured `uploadDir`. An attacker could craft a request with `../../etc/passwd` to read files outside the uploads directory.

**Fix needed:** Add path bounds validation before loading:
```java
Path resolved = Paths.get(uploadDir).resolve(filePath).normalize();
if (!resolved.startsWith(Paths.get(uploadDir).normalize())) {
    throw new BusinessException(DocumentErrorCode.INVALID_FILE_FORMAT);
}
```

**Why deferred:** Discovered during Module 4 security audit (Gemini review). The vulnerable code belongs to Module 2 (`document` package). Fixing it in the Module 4 branch could break existing document functionality. Should be addressed in a dedicated security fix PR.

**Impact:** Backend only — `FileStorageServiceImpl.java`. Low risk in dev (Docker-only deploys), higher risk if deployed to production.

**Priority:** HIGH — fix before any production deployment.
