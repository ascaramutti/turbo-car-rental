# TURBO - Development Plan

## Overview

TURBO is a peer-to-peer car rental platform that connects Car Owners with Drivers (international students doing gig work in Vancouver). This document describes the modular development plan, the workflow we will follow, and the itinerary for each module.

## Repository Strategy

We will create a **new repository** with a clean git history. The existing codebase serves as reference. Each module will be implemented from scratch, reviewed for quality, and merged step by step.

**Why a new repo?**
- Clean commit history that tells a clear story
- Each module gets its own branch and pull request
- Easier for team members to contribute per module
- Quality-first approach: no legacy code carried over without review

## Workflow (Option A - Vertical)

Each module is implemented **end-to-end** (database to frontend) before moving to the next one. This means every merge leaves a fully functional feature in the codebase.

### Branch Strategy

```
main (stable, always working)
  |
  |-- module-1/auth-security
  |-- module-2/user-entity-model
  |-- module-3/vehicle-management
  |-- module-4/document-management
  |-- module-5/booking-system
  |-- module-6/payments-earnings
  |-- module-7/reviews
  |-- module-8/admin-panel
  |-- module-9/public-pages-layout
  |-- module-10/infrastructure
```

### Steps Per Module

1. Create branch from `main`
2. Implement entities / database layer
3. Implement repositories
4. Implement services (business logic)
5. Implement controllers + DTOs
6. Implement frontend (pages + components)
7. Manual testing
8. Code review
9. Commit + merge to `main`

---

## Modules

### Module 1 - Auth & Security
**Branch:** `module-1/auth-security`

| Layer | Files |
|-------|-------|
| Config | SecurityConfig, JwtUtil, JwtAuthFilter, CorsConfig |
| Service | AuthService, CustomUserDetailsService, EmailService |
| Controller | AuthController |
| DTOs | RegisterRequest, LoginRequest, AuthResponse |
| Frontend | AuthContext, axios.js, ProtectedRoute, LoginPage, SignUpPage, VerifyOtpPage |

**Features:**
- User registration (Driver or Car Owner)
- Login with email/password, returns JWT token
- OTP email verification (6-digit code, 10-minute expiry)
- OTP resend functionality
- Route protection by role (DRIVER, CAR_OWNER, ADMIN)
- JWT interceptor on every authenticated request
- CORS configuration for frontend

---

### Module 2 - User & Entity Model
**Branch:** `module-2/user-entity-model`

| Layer | Files |
|-------|-------|
| Entities | User, Driver, CarOwner, Admin |
| Enums | UserRole, BookingStatus, DocumentStatus, PaymentStatus, ReviewType |
| Repos | UserRepository, DriverRepository, CarOwnerRepository, AdminRepository |
| Config | DataInitializer (seed data) |

**Features:**
- User inheritance model (User base with JOINED strategy)
- Driver: license number, study permit, work eligibility
- CarOwner: bank account, rating, vehicle list
- Admin: department
- Seed data with 3 test users (admin, driver, owner)
- All enums for status management across the platform

---

### Module 3 - Vehicle Management
**Branch:** `module-3/vehicle-management`

| Layer | Files |
|-------|-------|
| Entities | Vehicle, AvailabilitySlot |
| Repos | VehicleRepository, AvailabilitySlotRepository |
| Service | VehicleService |
| Controller | VehicleController |
| DTOs | VehicleRequest, VehicleResponse |
| Frontend | OwnerVehicles, SelectCarPage |

**Features:**
- Owner creates vehicle (make, model, year, price/day, location)
- Owner edits and deletes vehicles
- Owner toggles vehicle availability
- Vehicle requires 3 approved documents to activate (Registration, Insurance, Inspection)
- Driver browses available vehicles catalog
- Driver views vehicle details
- Availability slots by date range

---

### Module 4 - Document Management
**Branch:** `module-4/document-management`

| Layer | Files |
|-------|-------|
| Entity | Document |
| Repo | DocumentRepository |
| Service | DocumentService, FileStorageService |
| Controller | DocumentController |
| Frontend | DocumentsPage |

**Features:**
- Upload documents (JPG, PNG, PDF - max 5MB)
- Documents linked to vehicle or user
- Audit trail: old documents are never deleted, new versions are created
- Checklist of 3 required documents per vehicle
- `getLatestDocsPerType()` to determine current approval status
- Document download and preview

---

### Module 5 - Booking System
**Branch:** `module-5/booking-system`

| Layer | Files |
|-------|-------|
| Entity | Booking |
| Repo | BookingRepository |
| Service | BookingService |
| Controller | BookingController |
| DTOs | BookingRequest, BookingResponse |
| Frontend | OwnerBookings, DriverDashboard |

**Features:**
- Driver creates booking (select vehicle + dates)
- Owner confirms or rejects booking
- Either party can cancel
- Status flow: PENDING -> CONFIRMED -> ACTIVE -> COMPLETED / CANCELLED
- Booking list filtered by role (driver sees own, owner sees vehicle bookings)

---

### Module 6 - Payments & Earnings
**Branch:** `module-6/payments-earnings`

| Layer | Files |
|-------|-------|
| Entity | Payment |
| Repo | PaymentRepository |
| Service | PaymentService |
| Controller | OwnerDashboardController |
| DTOs | EarningsResponse, TransactionResponse |
| Frontend | OwnerDashboard, OwnerEarnings |

**Features:**
- Payment linked to each booking
- Payment split: platform fee + owner payout + security deposit
- Payment status: PENDING -> COMPLETED / REFUNDED / FAILED
- Owner dashboard: total earnings and monthly breakdown
- Transaction history

---

### Module 7 - Reviews
**Branch:** `module-7/reviews`

| Layer | Files |
|-------|-------|
| Entity | Review |
| Repo | ReviewRepository |
| Service | ReviewService |
| Controller | ReviewController |
| DTOs | ReviewRequest, ReviewResponse |
| Frontend | OwnerReviews, StarRating |

**Features:**
- Driver rates Owner after completing a booking
- Owner rates Driver after completing a booking
- Star rating (1-5) with text comment
- Review list by owner
- Average rating displayed on profile

---

### Module 8 - Admin Panel
**Branch:** `module-8/admin-panel`

| Layer | Files |
|-------|-------|
| Controller | AdminController |
| Frontend | AdminDashboard, AdminUsers |

**Features:**
- View pending documents for review
- Modal with zoom to inspect documents
- Approve or reject documents
- View all documents (full history)
- User list with roles and account status

---

### Module 9 - Public Pages & Layout
**Branch:** `module-9/public-pages-layout`

| Layer | Files |
|-------|-------|
| Frontend | HomePage, PricingPage, AboutPage |
| Components | Navbar, Footer, Sidebar |
| Router | App.jsx (27 routes + 404 fallback) |

**Features:**
- Home page with hero section, features, and call-to-action
- Pricing page with plans and rates
- About page with mission and team info
- Dynamic navbar based on role and auth status
- Sidebar for dashboard navigation
- Footer component
- Full routing with role-based access and 404 fallback

---

### Module 10 - Infrastructure
**Branch:** `module-10/infrastructure`

| Layer | Files |
|-------|-------|
| Docker | docker-compose.yml |
| Backend | Dockerfile (multi-stage Maven build) |
| Frontend | Dockerfile (multi-stage Node build) |
| Nginx | nginx.conf |

**Features:**
- Docker Compose with 4 containers (MySQL, Backend, Nginx, MailHog)
- Multi-stage backend build (Maven -> JDK 17 runtime)
- Multi-stage frontend build (Node -> Nginx Alpine)
- Nginx reverse proxy: `/api/*` -> backend, static files with cache
- MailHog captures emails in development (UI on port 8025)
- Persistent volumes for MySQL data and file uploads
- Health checks for service dependencies

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Spring Boot 3.2.3 + Java 17 + Maven |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Database | MySQL 8.0 + Spring Data JPA + Hibernate |
| Frontend | React 19 + Vite + Tailwind CSS v4 |
| Routing | React Router v7 |
| HTTP Client | Axios |
| Email (Dev) | MailHog |
| Containers | Docker Compose |
| Proxy | Nginx 1.25 |

## Test Users

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@turbo.com | admin123 |
| Driver | driver@turbo.com | driver123 |
| Owner | owner@turbo.com | owner123 |

All test users have `emailVerified = true` for development convenience.

## Team Contribution Guide

1. Pull the latest `main` branch
2. Check which module is currently in progress
3. Create your changes on the active module branch
4. Push your commits to the module branch
5. Request a code review before merging
6. After approval, merge to `main` and move to the next module
