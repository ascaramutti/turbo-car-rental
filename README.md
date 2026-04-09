# TURBO — Peer-to-Peer Car Rental Platform

TURBO is a peer-to-peer car rental platform that connects **Car Owners** with **Drivers**, with administrative oversight, integrated payments via Stripe, and document verification.

This README explains how to run the entire stack with a single command using **Docker Compose**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA |
| Frontend | React 19, Vite, Tailwind CSS, React Router v7 |
| Database | MySQL 8.0 |
| Email (dev) | MailHog |
| Payments | Stripe (test mode) |
| Maps | Google Maps JavaScript API |
| Auth | JWT + OTP via email |
| Mapping | MapStruct |
| Testing | JUnit, Vitest, Playwright (E2E) |

---

## Prerequisites

You only need **one** thing installed on your machine:

- **Docker Desktop** — https://www.docker.com/products/docker-desktop/
  - macOS / Windows / Linux all supported
  - Make sure Docker Desktop is **running** before starting (the whale icon should be visible in your menu bar / system tray)

You do **not** need Java, Node, Maven, MySQL or any other tool installed locally. Everything runs inside containers.

---

## Quick Start (one command)

```bash
git clone https://github.com/ascaramutti/turbo-car-rental.git
cd turbo-car-rental
git checkout develop
docker compose up --build
```

The first build takes **5–8 minutes** (downloads base images, compiles the backend, builds the frontend bundle). Subsequent runs are nearly instant thanks to Docker layer caching.

When you see these three lines in the logs, the stack is ready:

```
turbo-mysql      | ... ready for connections
turbo-backend    | ... Started TurboCarRentalApplication in X seconds
turbo-frontend   | ... Configuration complete; ready for start up
```

---

## URLs

Once the stack is up, open these in your browser:

| Service | URL | Purpose |
|---|---|---|
| **Frontend (TURBO app)** | http://localhost:5173 | Main user interface |
| **Backend API** | http://localhost:8080 | REST API (Spring Boot) |
| **MailHog UI** | http://localhost:8025 | View OTP and notification emails |
| **MySQL** | localhost:3306 | Database (only needed for direct queries) |

---

## Seed Credentials

The backend automatically seeds three users on first startup. Use them to log in and explore the platform:

| Role | Email | Password |
|---|---|---|
| **Admin** | `admin@turbo.com` | `admin123` |

---

## Suggested Demo Flow

1. **Log in as Driver** → search for available vehicles → open one → create a booking for tomorrow.
2. **Log in as Car Owner** → see the booking request → confirm it.
3. **Log in as Driver again** → pay the booking using a Stripe test card:
   - **Card number:** `4242 4242 4242 4242`
   - **Expiration:** any future date (e.g. `12/30`)
   - **CVC:** any 3 digits (e.g. `123`)
   - **ZIP:** any (e.g. `V6B 1A1`)
4. **Check MailHog** at http://localhost:8025 to see the confirmation email.
5. **Log in as Admin** → review users, vehicles and bookings.

For more failure scenarios (declined card, insufficient funds), see the [Stripe testing docs](https://stripe.com/docs/testing).

---

## Useful Commands

| Action | Command |
|---|---|
| Start the stack | `docker compose up` |
| Start in background | `docker compose up -d` |
| Stop the stack | `docker compose down` |
| Stop and **wipe the database** | `docker compose down -v` |
| Rebuild after code changes | `docker compose up --build` |
| Tail backend logs | `docker compose logs -f backend` |
| Tail frontend logs | `docker compose logs -f frontend` |
| Restart only the backend | `docker compose restart backend` |

---

## Troubleshooting

### "Port is already allocated"
Another process is using one of the ports (5173, 8080, 3306, 8025, 1025). Find and stop it:
```bash
lsof -i :8080            # macOS / Linux — replace with the conflicting port
docker ps                # check if a previous container is still running
docker rm -f <name>      # remove a stuck container
```

### "Container name already in use"
A previous run left a container with the same name. Force-remove it:
```bash
docker rm -f turbo-mysql turbo-mailhog turbo-backend turbo-frontend
docker compose up --build
```

### Backend can't connect to MySQL
MySQL takes ~15 seconds to become healthy on the first run. Compose's `depends_on` waits for the healthcheck, but if it still fails:
```bash
docker compose down -v   # wipe the volume
docker compose up --build
```

### "Payment processing is not configured"
The Stripe keys are baked into the image at build time. If you cloned and ran without `--build`, force a rebuild:
```bash
docker compose down
docker compose up --build
```

### "Start time must be in the future" when creating a booking
The container is already configured for `America/Vancouver` timezone. If you're running in a very different timezone and still see this, pick a time several hours later in the day.

### Reset everything from scratch
```bash
docker compose down -v
docker system prune -f
docker compose up --build
```

---

## Project Structure

```
turbo-car-rental/
├── backend/                # Spring Boot application
│   ├── src/main/java/      # Java source (controller / service / repository / model)
│   ├── src/main/resources/ # application.properties, templates
│   ├── src/test/           # JUnit unit and integration tests
│   ├── docs/               # OpenAPI 3.0 contracts (auth, vehicles, booking, payment, reviews)
│   ├── pom.xml             # Maven build descriptor
│   └── Dockerfile          # Multi-stage: Maven build → JRE runtime
├── frontend/               # React + Vite SPA
│   ├── src/                # React source (modules / components / pages)
│   ├── e2e/                # Playwright end-to-end tests + manual checklist
│   ├── package.json
│   ├── vite.config.js
│   ├── nginx.conf          # Nginx config for serving the built bundle
│   └── Dockerfile          # Multi-stage: Vite build → Nginx
├── docker-compose.yml      # Orchestrates mysql + mailhog + backend + frontend
└── README.md               # This file
```

---

## Architecture Overview

TURBO follows a pragmatic combination of established patterns:

- **Client–Server, Modular Monolith** — single deployable backend, decoupled SPA frontend.
- **Decoupled MVC** — React handles the View, Spring REST controllers handle Controller + Model.
- **Layered Architecture with Service Layer** — `controller → service → repository → entity`, with explicit DTOs and MapStruct mappers between layers.
- **Modular by feature** — each business module (`auth`, `vehicles`, `booking`, `documents`, `payments`, `reviews`, `admin`) contains its own controllers, services, repositories, DTOs and tests.
- **REST + Stateless Authentication (JWT)** — every request is self-contained; the backend keeps no session state.
- **API contracts** — every module is documented with an **OpenAPI 3.0 specification** under `backend/docs/` as the single source of truth between frontend and backend.

---

## Testing

| Type | Tool | Location |
|---|---|---|
| Backend unit tests | JUnit + Mockito | `backend/src/test/` |
| Frontend unit tests | Vitest + Testing Library | `frontend/src/**/tests/` |
| End-to-end tests | Playwright | `frontend/e2e/` |

To run the Playwright E2E suite locally (requires Node and Playwright browsers installed):
```bash
cd frontend
npm install
npx playwright install
npm run test:e2e
```

