# ── Build stage ──────────────────────────────────────────────
FROM node:22 AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
# Vite는 빌드 시점에 VITE_* 환경변수를 번들에 주입한다.
# 데모 시에는 비워도 됨(지도 타일만 렌더링 안 됨).
ARG VITE_GOOGLE_MAPS_API_KEY=""
ENV VITE_GOOGLE_MAPS_API_KEY=$VITE_GOOGLE_MAPS_API_KEY
RUN npm run build

# ── Runtime stage (nginx) ────────────────────────────────────
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
