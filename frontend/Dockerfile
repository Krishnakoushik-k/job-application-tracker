# ---- Stage 1: build the static site -----------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
# Uses the lock file when it exists (recommended: commit package-lock.json).
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY . .
# Baked into the bundle at build time. "/api" is proxied to the backend by nginx.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---- Stage 2: serve with nginx --------------------------------------------------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
