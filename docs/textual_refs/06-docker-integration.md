# Openpress — Docker Integration

> **Prerequisites:** Docker and Docker Compose installed on your machine
> **Status:** 🚧 Planned — Neither `Dockerfile` nor `docker-compose.yml` exist in the project root yet. This document describes the target Docker setup and should be used as an implementation guide.

---

## 1. Dockerfile

Create `Dockerfile` in the project root:

```dockerfile
# ── Stage 1: Build ───────────────────────────────────
FROM gradle:8.13-jdk21 AS build

WORKDIR /app

# Copy Gradle wrapper and config first (cache dependencies)
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradlew gradlew.bat ./

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the fat JAR
RUN ./gradlew buildFatJar --no-daemon

# ── Stage 2: Runtime ────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/openpress-*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
```

> **Note:** The `buildFatJar` task comes from the Ktor plugin (`io.ktor.plugin`). It bundles all dependencies into a single executable JAR.

---

## 2. Docker Compose

Create `docker-compose.yml` in the project root:

```yaml
version: "3.9"

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - POSTGRES_URL=jdbc:postgresql://db:5432/openpress
      - POSTGRES_USER=openpress
      - POSTGRES_PASSWORD=changeme
      - JWT_SECRET=change-this-in-production
      - STRIPE_SECRET_KEY=${STRIPE_SECRET_KEY}
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

  db:
    image: postgres:16-alpine
    volumes:
      - pgdata:/var/lib/postgresql/data
    environment:
      - POSTGRES_DB=openpress
      - POSTGRES_USER=openpress
      - POSTGRES_PASSWORD=changeme
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U openpress"]
      interval: 5s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  pgdata:
```

---

## 3. Update `application.yaml` for Docker

```yaml
ktor:
  deployment:
    port: 8080
  application:
    modules:
      - org.nexus.openpress.HttpKt.configureHttp
      - org.nexus.openpress.MonitoringKt.configureMonitoring
      - org.nexus.openpress.SerializationKt.configureSerialization
      - org.nexus.openpress.SecurityKt.configureSecurity
      - org.nexus.openpress.PostgresKt.configurePostgres
      - org.nexus.openpress.RoutingKt.configureRouting

postgres:
  url: ${POSTGRES_URL}
  user: ${POSTGRES_USER}
  password: ${POSTGRES_PASSWORD}
```

---

## 4. Build & Run

```bash
# Build and start all services
docker compose up --build

# Run in detached mode (background)
docker compose up -d --build

# View logs
docker compose logs -f app

# Stop services
docker compose down

# Stop and remove volumes (wipes database data)
docker compose down -v
```

---

## 5. Verify It Works

```bash
# Health check
curl http://localhost:8080/
# Expected: "Hello, World!"

# Check running containers
docker compose ps
```

---

## 6. Multi-Environment Setup

For different environments, create separate Compose files:

```yaml
# docker-compose.override.yml  (local dev — replaces production defaults)
services:
  app:
    ports:
      - "8080:8080"
    environment:
      - JWT_SECRET=dev-secret
```

```bash
# Development
docker compose -f docker-compose.yml -f docker-compose.override.yml up

# Production
docker compose -f docker-compose.yml up
```

---

## 7. Useful Docker Commands

```bash
# Rebuild without cache
docker compose build --no-cache

# Execute inside a running container
docker compose exec app sh

# Check resource usage
docker stats

# Copy a file from container
docker compose cp app:/app/app.jar .
```
