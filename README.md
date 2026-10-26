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

## Running with Docker

Requirements: Docker with Compose. Nothing else to install.

```bash
docker compose up --build
```

| Service | URL | Notes |
|---|---|---|
| web | http://localhost:3000 | nginx serves the app and forwards `/api` to the API |
| api | http://localhost:8080 | Health: `/actuator/health`. OpenAPI UI: `/swagger-ui.html` |
| db | localhost:15432 | PostGIS. Host port 15432 so it does not clash with a local PostgreSQL |

The API container runs with the `demo` Spring profile, which loads six sample lots around Natal
(RN), including two neighbors in Ponta Negra that share a border. To start with an empty database
instead, set the profile to an empty value: `SPRING_PROFILES_ACTIVE= docker compose up --build`.

Start over with a clean database: `docker compose down -v && docker compose up --build`.

Every setting has a default in `docker-compose.yml`; `.env.example` lists them.

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

Backend integration tests use Testcontainers, so Docker must be running. The backend build fails
when coverage drops below 80%, the frontend build below 85%.
