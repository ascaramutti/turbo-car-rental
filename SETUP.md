# Local Development Setup

## Prerequisites

- **Docker Desktop** must be installed and running ([download here](https://www.docker.com/products/docker-desktop/))
- **Java 17+** (JDK)
- **Maven 3.8+**

## Step by step

### 1. Start infrastructure

From the project root:

```bash
cd TURBO/
docker compose up -d
```

This starts:
- **MySQL 8.0** on `localhost:3306` (database: `turbo_db`, user: `turbo_user`, password: `turbo_pass`)
- **MailHog** on `localhost:8025` (web UI to view verification emails)

### 2. Wait for MySQL to be ready

```bash
docker compose ps
```

Make sure both services show `Up` status. MySQL may take ~10 seconds on first run.

### 3. Start the backend

```bash
cd backend/
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

## Verify everything works

- **Backend API:** `POST http://localhost:8080/api/auth/login` (should return a validation error)
- **MailHog UI:** `http://localhost:8025` (should show an empty inbox)

## Shutting down

```bash
# Stop the backend: Ctrl+C in the terminal

# Stop infrastructure (from project root):
docker compose down
```

## Seed data

The backend automatically creates test users on startup:

| Email | Password | Role |
|-------|----------|------|
| admin@turbo.com | admin123 | ADMIN |
| driver@turbo.com | driver123 | DRIVER |
| owner@turbo.com | owner123 | CAR_OWNER |

## Troubleshooting

- **Port 3306 already in use:** Stop any local MySQL instance or change the port in `docker-compose.yml` and `backend/src/main/resources/application.properties`.
- **Port 8080 already in use:** Another app is using it. Kill the process or change `server.port` in `application.properties`.
- **MySQL connection refused:** Wait a few more seconds and retry. Check `docker compose logs mysql` for errors.
