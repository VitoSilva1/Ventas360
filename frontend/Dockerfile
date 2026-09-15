FROM node:20-alpine AS build

# Google OAuth Web Client ID — se pasa desde docker-compose como build-arg.
# El valor se inyecta en environment.ts antes de compilar Angular.
ARG GOOGLE_CLIENT_ID=TU_CLIENT_ID.apps.googleusercontent.com

WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .

# Reemplaza el placeholder del environment.ts con el Client ID real.
RUN sed -i "s|TU_CLIENT_ID.apps.googleusercontent.com|${GOOGLE_CLIENT_ID}|g" \
    src/environments/environment.ts src/environments/environment.prod.ts

RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
