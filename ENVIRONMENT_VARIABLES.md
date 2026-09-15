# Variables de entorno por microservicio

Este archivo enumera los **nombres** de las variables usadas por `docker-compose.yml`. No contiene valores reales ni secretos.

## Resumen rápido

| Servicio | Variables |
|---|---|
| `postgres` | `DB_USERNAME`, `DB_PASSWORD` |
| `service-registry` | `SERVER_PORT`, `EUREKA_URL` |
| `auth-service` | `SERVER_PORT`, `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS`, `EUREKA_INSTANCE_HOSTNAME`, `GOOGLE_OAUTH_CLIENT_ID` |
| `product-service` | `DB_USERNAME`, `DB_PASSWORD`, `SPRING_DATASOURCE_URL`, `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS`, `SERVER_PORT`, `EUREKA_INSTANCE_HOSTNAME` |
| `sales-service` | `DB_USERNAME`, `DB_PASSWORD`, `SPRING_DATASOURCE_URL`, `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS`, `SERVER_PORT`, `EUREKA_INSTANCE_HOSTNAME` |
| `api-gateway` | `SERVER_PORT`, `EUREKA_URL` |
| `frontend` | Ninguna variable declarada en Compose actualmente |

## Detalle por servicio

### `postgres`

- `DB_USERNAME`
- `DB_PASSWORD`

### `service-registry`

- `SERVER_PORT`
- `EUREKA_URL`

### `auth-service`

- `SERVER_PORT`
- `EUREKA_URL`
- `EUREKA_INSTANCE_PREFER_IP_ADDRESS`
- `EUREKA_INSTANCE_HOSTNAME`
- `GOOGLE_OAUTH_CLIENT_ID`

`GOOGLE_OAUTH_CLIENT_ID` es el Client ID OAuth de tipo Web application. No se debe agregar un
client secret al frontend ni al repositorio.

### `product-service`

- `DB_USERNAME`
- `DB_PASSWORD`
- `SPRING_DATASOURCE_URL`
- `EUREKA_URL`
- `EUREKA_INSTANCE_PREFER_IP_ADDRESS`
- `SERVER_PORT`
- `EUREKA_INSTANCE_HOSTNAME`

### `sales-service`

- `DB_USERNAME`
- `DB_PASSWORD`
- `SPRING_DATASOURCE_URL`
- `EUREKA_URL`
- `EUREKA_INSTANCE_PREFER_IP_ADDRESS`
- `SERVER_PORT`
- `EUREKA_INSTANCE_HOSTNAME`

### `api-gateway`

- `SERVER_PORT`
- `EUREKA_URL`

### `frontend`

No tiene variables de entorno declaradas actualmente en `docker-compose.yml`.

## Antes de levantar Compose

Definí al menos la variable requerida por el auth-service:

```bash
export GOOGLE_OAUTH_CLIENT_ID="tu-client-id.apps.googleusercontent.com"
```

Las variables de PostgreSQL pueden definirse con `DB_USERNAME` y `DB_PASSWORD`; Compose tiene
valores predeterminados para desarrollo local.

