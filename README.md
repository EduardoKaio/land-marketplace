# Land Marketplace

Map-based marketplace for land lots. Sellers draw the exact polygon of a lot on the map and
publish it; buyers draw a circle to find lots that intersect the searched area.

## Stack

- Backend: Java 21, Spring Boot, Spring Data JPA, Hibernate Spatial, Flyway
- Database: PostgreSQL with PostGIS
- Frontend: React, Vite, TypeScript, OpenLayers

## Repository layout

```
backend/    Spring Boot API
frontend/   React application
docs/       Project documentation
```

## Running locally

Requirements: Docker with Compose, Java 21.

Start the database:

```bash
docker compose up -d db
```

Run the API against it:

```bash
cd backend && ./mvnw spring-boot:run
```

The API listens on http://localhost:8080. Health check: http://localhost:8080/actuator/health.
OpenAPI UI: http://localhost:8080/swagger-ui.html.

All settings have defaults. To override them, copy `.env.example` to `.env`.

## Tests

```bash
cd backend && ./mvnw verify
```

Integration tests use Testcontainers, so Docker must be running.
