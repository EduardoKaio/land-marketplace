# Build stage: Node only compiles the static files, it does not run in production
FROM node:24-alpine AS build
WORKDIR /build

# Dependencies first: this layer is rebuilt only when the lockfile changes
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Runtime stage: nginx serves the static files and forwards /api to the backend
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
