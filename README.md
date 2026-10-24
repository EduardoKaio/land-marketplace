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

Requirements: Docker with Compose, Java 21, Node.js 22.

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

Run the frontend (another terminal):

```bash
cd frontend && npm install && npm run dev
```

The app opens on http://localhost:5173. The Vite dev server forwards `/api` to the API on port
8080, so the browser only talks to one origin.

All settings have defaults. To override them, copy `.env.example` to `.env`.

## Tests

```bash
cd backend && ./mvnw verify              # report: backend/target/site/jacoco/index.html
cd frontend && npm run test:coverage     # report: frontend/coverage/index.html
```

Backend integration tests use Testcontainers, so Docker must be running. Both builds fail when
coverage drops below 80%.
