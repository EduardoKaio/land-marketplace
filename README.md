# Land Marketplace

Map-based marketplace for land lots. Sellers draw the exact polygon of a lot on the map and
publish it; buyers draw a circle to find the lots inside the area they care about. The core rule
is spatial: two lots can never share area, but neighbors may share a border or a corner, as real
lots do.

- [Overview](#overview)
- [How it works](#how-it-works)
- [API reference](#api-reference)
- [Running with Docker](#running-with-docker)
- [Running without Docker](#running-without-docker)
- [Tests and coverage](#tests-and-coverage)
- [Known limitations](#known-limitations)

## Overview

### Features

Required scope:

- **Register a lot.** Navigate the map, draw the polygon vertex by vertex (snapping to the vertices
  and edges of existing lots) and fill in title, total price, description and contact. The area is
  shown live while drawing. A polygon whose area overlaps an existing lot is rejected and the
  invaded lots are highlighted in red; lots that only touch on a border or a vertex are accepted.
- **Search by circle.** Press, drag and release on the map to draw a circle, with the radius shown
  live while dragging. Only the lots that intersect the circle are rendered.
- **Lot details.** Clicking a lot opens a popup with price, area, price per m², description and
  contact links.

Additions on top of the required scope:

- **Accounts.** Anyone can browse and search; publishing needs an account. Only the owner of a lot
  can update or delete it, and the "My lots" panel lists the lots of the logged in account.
- **Filters.** Price range, area range, maximum price per m² and status (available or sold),
  applied to the map listing and to the circle search at the same time.
- **Validated contact.** Brazilian phone numbers with an input mask, `(84) 99999-8888`, and emails
  with a full domain, checked with the same rules on both sides.

Not covered: buying a lot. Buyers contact the seller through the phone or email shown in the popup,
and the owner marks the lot as sold.

### Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Hibernate Spatial, Flyway, Spring Security (JWT) |
| Database | PostgreSQL 16 with PostGIS 3.4 |
| Frontend | React 19, TypeScript, Vite, OpenLayers, React Query, react-hook-form, zod |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers, JaCoCo / Vitest, React Testing Library, user-event, MSW |
| Delivery | Docker Compose, nginx |

### Demo data

With Docker the API starts with the `demo` profile: six sample lots around Natal (RN), including two
neighbors in Ponta Negra that share a border and one sold lot. They belong to the demo account
**demo@example.com / demo1234**.

## How it works

### Architecture

```
Browser (React + OpenLayers, map in EPSG:3857)
   │   GeoJSON in EPSG:4326 over HTTP
   ▼
nginx (Docker) or Vite dev server   ── serves the app, forwards /api (same origin, no CORS)
   │
   ▼
Spring Boot API   ── validation, business rules, JWT
   │   JDBC, native spatial queries
   ▼
PostgreSQL + PostGIS   ── geometry storage, spatial predicates, GiST indexes
```

Coordinates are stored and exchanged in **EPSG:4326** (WGS84 longitude and latitude, the GeoJSON
standard). The map view uses **EPSG:3857** (Web Mercator), the projection of the map tiles. The
conversion happens in one place only, at the boundary between the map and the API client, the same
way an application stores UTC and converts to local time only on screen.

### Backend structure

The backend is organized **by feature**, with the same four layers inside each feature:

```
io.github.eduardokaio.landmarketplace
├── landlot/          land lots: create, update, delete, search, list
│   ├── api/              thin controllers, request and response DTOs (records), exception handler
│   ├── application/      services (use cases) and commands
│   ├── domain/           LandLot entity, Contact value object, filter, domain exceptions
│   └── infrastructure/   repository with native PostGIS queries, creation lock
├── auth/             accounts, login and JWT, same four layers
└── shared/
    ├── api/              global error handling, CORS, OpenAPI, shared validation patterns
    └── geo/              GeoJSON ↔ JTS mapping, precision grid, bounding box
```

Dependencies point inwards: `api` depends on `application`, which depends on `domain`;
`infrastructure` implements what the services need. A new feature is a new package instead of
files spread across global `controller` and `service` folders.

### Frontend structure

```
frontend/src
├── api/         typed HTTP client, Problem Details errors, token interceptor
├── map/         OpenLayers setup, layers, styles, projection conversion (hooks and pure functions)
├── features/    one folder per feature: land-lots (draw and publish), search, lot-popup,
│                filters, auth, my-lots, map-mode, map-workspace
└── shared/      formatting, phone mask, toast notifications
```

- **Each map mode is a component** that is mounted only while the mode is active. Mounting adds its
  OpenLayers interactions and layers in an effect; unmounting removes them in the cleanup, so
  switching modes can never leave two tools fighting for the same click.
- **Map logic lives in hooks and pure functions** (`useDrawLot`, `useCircleDraw`, `useLotPopup`,
  `toApiGeometry`, `getCircleSearchParams`, `maskPhone`), so it is tested without a browser.
- **Server state** goes through React Query; **forms** use react-hook-form with zod schemas that
  mirror the backend rules, while the backend stays the authority.
- Values that change on every pointer move (live area and radius) are written straight to the DOM
  instead of React state, to avoid dozens of re-renders per second.

### Data flow: publishing a lot

1. The seller draws the polygon on the map (EPSG:3857). The `Snap` interaction glues the pointer to
   the vertices and edges of neighboring lots.
2. The frontend converts the polygon to a GeoJSON geometry in EPSG:4326, **rounded to 7 decimal
   places** and with a counterclockwise exterior ring (RFC 7946), and sends it with the form.
3. Bean Validation checks the request fields; errors come back as a 400 listing every invalid field.
4. `GeoJsonGeometryMapper` checks the GeoJSON structure (type, closed rings, positions) and builds a
   JTS polygon on the same 7 decimal grid. Malformed geometry is a 422 with the reason.
5. `LandLotService.create`, in one transaction:
   1. takes a PostgreSQL advisory lock (see [Concurrency](#concurrency));
   2. rejects invalid shapes such as self-intersections with JTS, with the reason and location
      (422). Validity comes first because PostGIS predicates fail on invalid geometries;
   3. asks PostGIS for lots whose interior overlaps the new polygon (409 with their ids);
   4. creates the entity through its factory method, which enforces the domain invariants, and
      saves it with `saveAndFlush` so the area computed by the database is read back.
6. The response is a GeoJSON Feature (201 with a `Location` header). The frontend adds it to the map
   layer, or paints the conflicting lots red on a 409 and keeps the drawing for adjustments.

**Why the 7 decimal grid:** snapping copies a neighbor's vertex, but the round trip
4326 → 3857 → 4326 adds noise around the ninth decimal place, which would turn a shared border into
a microscopic overlap and a false 409. Rounding to the same grid (about 1 cm) on both sides brings
the vertex back identical.

### Spatial solutions

Every spatial predicate runs in PostGIS through native queries, never in Java loops.

| Problem | Solution | Why |
|---|---|---|
| Reject overlapping lots, allow shared borders | `ST_Intersects(a, b) AND NOT ST_Touches(a, b)` | True when the interiors cross: partial overlap, containment either way and identical shapes. `ST_Overlaps` was rejected because it is false when one polygon contains the other or both are equal, the worst cases |
| Search by circle | `ST_DWithin(geom::geography, point::geography, radius)` | Geodesic distance in meters and uses the index. Comparing `ST_Distance` scans every row, and a buffered circle is a 32-sided approximation that misses lots near the edge |
| List the lots of the visible map | `geom && ST_MakeEnvelope(...)`, largest first, `LIMIT max + 1` | The cheapest check the GiST index answers. The extra row tells whether the result was capped (`truncated: true`) without a `COUNT` query |
| Area in square meters | Generated column `ST_Area(geom::geography) STORED` | Computed by the database on the WGS84 spheroid, never stale, and sortable |
| Invalid shapes | `CHECK (ST_IsValid(geom))` on the column | Protects the table even from inserts that bypass the API |
| Fast predicates | GiST index on `geom` and an expression index on `(geom::geography)` | The circle query writes exactly `l.geom::geography`, the indexed expression, so the planner can use it (checked with `EXPLAIN`) |

The column type is `geometry(Polygon, 4326)`: fast planar predicates for overlap, with a cast to
`geography` whenever a measure in meters is needed. The circle radius drawn on the map is not sent
as is: Web Mercator stretches distances by 1 / cos(latitude), so the frontend measures the geodesic
distance between the center and a point of the border.

### Concurrency

Creating a lot is a check-then-insert. Under READ COMMITTED, two overlapping lots submitted at the
same moment would both pass the overlap check and both be saved, and there is no `UNIQUE`
constraint for "shapes that share area" (an exclusion constraint on `&&` compares bounding boxes
and would also block legitimate neighbors).

The first statement of the creation transaction is `pg_advisory_xact_lock` on a fixed key, so
creations run one at a time. The lock is released by the database on commit or rollback, and
unlike `synchronized` it works across several API instances. A test starts 8 threads at once with
the same polygon and asserts that exactly one lot is saved; with the lock removed, all 8 were
saved. The trade-off is a single queue for all creations, acceptable at the expected volume; a
grid of lock keys would scale it.

### Design decisions

- **Rich domain entity.** `LandLot` has no public setters. It is created through a factory method,
  `LandLot.create`, and changed through `update`, both validating every invariant first, so an
  invalid lot cannot exist in memory. `Contact` is an embedded value object.
- **Validation in three places, each with its own job:** Bean Validation on DTOs for detailed 400
  answers, the domain for invariants, and database constraints against anything that bypasses the
  API.
- **Patterns in use:** Repository (Spring Data with native spatial queries), DTO and Command
  (`CreateLandLotRequest` → `CreateLandLotCommand`, so services never see HTTP or JSON), Factory
  Method (`LandLot.create`), Assembler (`LandLotGeoJsonAssembler` builds the GeoJSON responses),
  Adapter (`GeoJsonGeometryMapper` between GeoJSON and JTS), and separate command and query
  services (`LandLotService`, `LandLotQueryService` with read-only transactions).
- **SOLID where it pays off:** single responsibility per class (controller, service, mapper,
  assembler, lock), dependency injection through constructors only, and API contracts documented
  in interfaces (`LandLotApi`, `AuthApi`) so controllers stay short.
- **A pattern that was removed on purpose.** The two creation checks (valid shape, no overlap) were
  first a Strategy: an interface, two implementations and an `@Order`. With two rules it spread the
  logic over three files and hid the order in an annotation, so they became two private methods
  called in sequence. The Strategy would come back if rules grew or varied by configuration.

### Errors

Every error is an RFC 9457 Problem Details document (`application/problem+json`).

| Status | When |
|---|---|
| 400 | Invalid fields (with an `errors` list), invalid bounding box, radius above the maximum, inverted filter range |
| 401 | Missing, expired or forged token; wrong credentials; account no longer exists |
| 403 | Logged in but not the owner of the lot |
| 404 | Lot not found |
| 409 | The polygon overlaps existing lots (`conflictingLotIds`), or the email is already registered |
| 422 | Well formed request, but the geometry cannot be accepted (for example a self-intersection) |
| 500 | Unexpected error, with a generic message; details only in the server log |

Values that cannot even be converted (a word where a number is expected) answer
"has an invalid value" without echoing the input or naming internal classes. Client errors are
logged as one line with the exception class only, so user input cannot forge log lines.

### Authentication

- `POST /api/auth/register` and `POST /api/auth/login` return a JWT access token, signed with HS256
  by Spring Security and valid for 2 hours (`JWT_EXPIRATION`). Clients send it as
  `Authorization: Bearer <token>`.
- The API is stateless. CSRF protection is off because no cookie is involved: the token travels in
  a header that only the app's own code sets. Every route not explicitly opened requires a token.
- Passwords are BCrypt hashes (8 to 64 characters, with a letter and a digit). Unknown emails are
  compared against a dummy hash, so response times do not reveal which emails have an account.
- The owner of a lot comes from the token, never from the request body. The polygon of a published
  lot cannot be changed; the owner deletes and publishes it again.
- The default `JWT_SECRET` is for development only; any deployment must set its own (at least 32
  bytes), otherwise anyone who reads the repository can forge tokens.

### Filters

The map listing and the circle search take the same optional filters: `minPrice`, `maxPrice`,
`minArea`, `maxArea` (square meters), `maxPricePerSquareMeter` and `status` (`AVAILABLE` or `SOLD`).
Bounds are inclusive and a minimum above its maximum is a 400. One SQL fragment is shared by both
queries, the spatial index still selects the candidates first, and the result limit applies after
the filters. The price per m² is compared rounded to cents, exactly as it is displayed. In the app,
the Filters button applies them to the map and to the current search at once.

## API reference

Interactive documentation with examples: http://localhost:8080/swagger-ui.html (raw OpenAPI at
`/v3/api-docs`).

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | none | Create an account; returns a token (201) |
| `POST` | `/api/auth/login` | none | Log in; returns a token |
| `GET` | `/api/land-lots?bbox=minLon,minLat,maxLon,maxLat` | none | Lots in the visible area, largest first, capped (`truncated`); accepts the filters |
| `GET` | `/api/land-lots/search?lon=&lat=&radius=` | none | Lots intersecting a circle (radius in meters, up to 50 km); accepts the filters |
| `GET` | `/api/land-lots/{id}` | none | One lot |
| `POST` | `/api/land-lots` | token | Publish a lot (201, 400, 409, 422) |
| `PUT` | `/api/land-lots/{id}` | owner | Update the details and status of a lot |
| `DELETE` | `/api/land-lots/{id}` | owner | Delete a lot (204) |
| `GET` | `/api/me/land-lots` | token | Lots of the logged in account, newest first |

Lots are returned as GeoJSON Features and FeatureCollections, with coordinates in EPSG:4326.

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

Open http://localhost:3000 and log in with **demo@example.com / demo1234**, or create an account.
The services start in order: the API waits for a healthy database and the web server for a healthy
API.

- Start with an empty database instead of the demo data:
  `SPRING_PROFILES_ACTIVE= docker compose up --build`
- Start over with a clean database: `docker compose down -v && docker compose up --build`
- Every setting has a default in `docker-compose.yml`; `.env.example` lists them, including the
  host ports.

## Running without Docker

Requirements: Java 21, Node.js 22 or newer, and PostgreSQL 16 with the PostGIS 3.4 extension
installed.

### 1. Database

Create the user and the database, and enable PostGIS as a superuser (creating the extension
requires superuser rights; the first migration then finds it already there). On Linux the
superuser is usually `postgres`; elsewhere, run the same SQL in `psql` as any superuser:

```bash
sudo -u postgres psql -c "CREATE USER landmarket WITH PASSWORD 'landmarket';"
sudo -u postgres psql -c "CREATE DATABASE landmarket OWNER landmarket;"
sudo -u postgres psql -d landmarket -c "CREATE EXTENSION IF NOT EXISTS postgis;"
```

Flyway creates the tables when the API starts.

### 2. Backend

The API defaults to `localhost:15432` (the Docker database), so point it at the local server:

```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/landmarket
export DB_USERNAME=landmarket
export DB_PASSWORD=landmarket
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Leave out `-Dspring-boot.run.profiles=demo` to start without the sample lots. The API listens on
http://localhost:8080 (health: `/actuator/health`, OpenAPI UI: `/swagger-ui.html`).

### 3. Frontend

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

The app opens on http://localhost:5173. The Vite dev server forwards `/api` to
http://localhost:8080 (override with `API_PROXY_TARGET`), so the browser only talks to one origin.
The initial map view can be changed with `VITE_MAP_CENTER` ("lon,lat") and `VITE_MAP_ZOOM`.

Shortcut: to skip installing PostgreSQL, run only the database container with
`docker compose up -d db` and start the backend without the `DB_*` variables; the defaults already
point at it.

## Tests and coverage

```bash
cd backend && ./mvnw verify            # tests + coverage report and check
cd frontend && npm run test:coverage   # tests + coverage report and check
```

Docker must be running for the backend tests: integration tests start a real PostGIS container
with Testcontainers (`postgis/postgis:16-3.4`). An in-memory database would not have the spatial
functions that matter here.

### Coverage reports

| Side | Tool | HTML report | Minimum enforced |
|---|---|---|---|
| Backend | JaCoCo | `backend/target/site/jacoco/index.html` | 80% of instructions, lines and branches |
| Frontend | Vitest (v8) | `frontend/coverage/index.html` | 85% of statements, branches, functions and lines |

Both builds fail when coverage drops below the minimum. The frontend also prints a summary table in
the terminal. Results on the current code:

| Side | Tests | Lines | Branches | Other |
|---|---|---|---|---|
| Backend | 244 | 99.8% | 97.2% | Instructions 99.9% |
| Frontend | 267 | 99.7% | 97.6% | Statements 99.7%, functions 97.8% |

### What the tests cover

- **Backend:** unit tests of the domain and pure classes; services with Mockito; web slices
  (`@WebMvcTest`) for validation and CORS; and tests against real PostGIS for every spatial rule:
  rejected overlaps (partial, containment, identical shape), accepted neighbors (shared border,
  single shared vertex, lot inside the hole of another), the database `CHECK` on invalid shapes,
  distance in meters, bounding box limits and every filter bound. Full application tests cover
  authentication, ownership rules, the demo seed and the concurrent creation of overlapping lots
  with 8 threads.
- **Frontend:** pure functions (projection conversion, rounding, circle radius, formatting, phone
  mask, schemas, error mapping); hooks rendered inside a real OpenLayers map (interaction order,
  cleanup on unmount); components with React Testing Library and user-event against the real HTTP
  client, with the API mocked by MSW.
- **Mutation checks by hand:** coverage only shows that lines ran, so the key tests were verified by
  breaking the code on purpose (removing the advisory lock, inverting the exception handler order,
  removing the stale response check of the filtered map layer) and watching the tests fail.

## Known limitations

- The creation lock is global: every new lot waits in one queue. A grid of lock keys would scale it.
- The circle search caps the radius (50 km) but not the number of results; the map listing caps both.
- Maps crossing the antimeridian (±180°) are not supported.
- The access token is kept in `localStorage`, readable by any script on the page if an XSS flaw
  existed. There are no refresh tokens, no token revocation and no login rate limiting yet.
- Updating a lot is available in the API but has no screen yet, and the polygon cannot be edited.
- Only Brazilian phone numbers are accepted.
- The frontend area shown while drawing is an estimate on a sphere (about 0.4% from the PostGIS
  value on the spheroid); the saved value comes from the database.
- The frontend bundle is above 500 kB, mostly OpenLayers; code splitting the map would reduce it.
- No automated end-to-end test goes through nginx yet.
