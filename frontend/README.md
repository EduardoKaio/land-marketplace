# Land Marketplace web

React (Vite + TypeScript) client. OpenLayers renders and handles the map.

## Commands

```bash
npm run dev             # dev server on http://localhost:5173, /api proxied to localhost:8080
npm run build           # type check and production build
npm test                # unit and component tests
npm run test:coverage   # tests with coverage; fails below 85%
npm run lint            # ESLint
npm run format          # Prettier
```

## Configuration

Optional variables in `frontend/.env.local`:

- `VITE_MAP_CENTER`: start center as `lon,lat` (default Natal: `-35.2094,-5.7945`)
- `VITE_MAP_ZOOM`: start zoom (default `12`)

`API_PROXY_TARGET` (shell variable) changes where the dev server forwards `/api`.

## Layout

- `src/api`: typed HTTP client and the GeoJSON types of the API contract (EPSG:4326)
- `src/map`: OpenLayers setup, layers and map hooks (view in EPSG:3857)
- `src/features/<feature>`: feature components and hooks
- `src/shared`: shared utilities and UI
