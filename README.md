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
(RN), including two neighbors in Ponta Negra that share a border. They belong to the demo account
**demo@example.com / demo1234**. To start with an empty database
instead, set the profile to an empty value: `SPRING_PROFILES_ACTIVE= docker compose up --build`.

Start over with a clean database: `docker compose down -v && docker compose up --build`.

Every setting has a default in `docker-compose.yml`; `.env.example` lists them.

## Filters

The map listing (`GET /api/land-lots?bbox=...`) and the circle search (`GET /api/land-lots/search`)
take the same optional filters: `minPrice`, `maxPrice`, `minArea`, `maxArea` (square meters),
`maxPricePerSquareMeter` and `status` (`AVAILABLE` or `SOLD`). Bounds are inclusive, a minimum
above its maximum is a 400, and the price per m² is compared rounded to cents, as it is shown. In
the app, the Filters button applies them to the map and to the current search at once.

## Accounts and access

Anyone can browse and search lots. Publishing needs an account: register or log in from the app
(or `POST /api/auth/register` and `POST /api/auth/login`), and send the returned token as
`Authorization: Bearer <token>`.

- Only the owner of a lot can update it (`PUT /api/land-lots/{id}`, details only: the polygon of a
  published lot cannot change) or delete it (`DELETE /api/land-lots/{id}`). Others get 403.
- `GET /api/me/land-lots` lists the lots of the logged in account.
- Tokens are JWTs signed with HS256 by Spring Security, valid for `JWT_EXPIRATION` (2 hours by
  default). The default `JWT_SECRET` is for development only; any deployment must set its own.
- Passwords are stored as BCrypt hashes and need 8 to 64 characters with a letter and a digit.

Not covered yet: refresh tokens, revoking tokens before they expire and rate limiting of the login.

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
