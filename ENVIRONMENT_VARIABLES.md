# Variables de entorno por microservicio

Este archivo enumera los **nombres** de las variables usadas por `docker-compose.yml`. No contiene valores reales ni secretos.

## Resumen rápido

| Servicio | Variables |
|---|---|
| `postgres` | `DB_USERNAME`, `DB_PASSWORD` |
| `service-registry` | `SERVER_PORT`, `EUREKA_URL` |
| `auth-service` | `SERVER_PORT`, `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS`, `EUREKA_INSTANCE_HOSTNAME`, `GOOGLE_OAUTH_CLIENT_ID` |
| `product-service` | `DB_USERNAME`, `DB_PASSWORD`, `SPRING_DATASOURCE_URL`, `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS`, `SERVER_PORT`, `EUREKA_INSTANCE_HOSTNAME`, `KAFKA_BOOTSTRAP_SERVERS` |
| `sales-service` | `DB_USERNAME`, `DB_PASSWORD`, `SPRING_DATASOURCE_URL`, `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS`, `SERVER_PORT`, `EUREKA_INSTANCE_HOSTNAME`, `KAFKA_BOOTSTRAP_SERVERS`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` |
| `analytics-service` | `DB_USERNAME`, `DB_PASSWORD`, `SPRING_DATASOURCE_URL`, `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS`, `SERVER_PORT`, `EUREKA_INSTANCE_HOSTNAME`, `KAFKA_BOOTSTRAP_SERVERS` |
| `kafka-admin` | `SERVER_PORT`, `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS`, `EUREKA_INSTANCE_HOSTNAME`, `KAFKA_BOOTSTRAP_SERVERS` |
| `kafka-1..3`, `zookeeper-1..3`, `kafka-ui`, `kafka-topics-init` (`docker-compose.kafka.yml`) | Sin variables externas: la configuración está fija en el archivo |
| `api-gateway` | `SERVER_PORT`, `EUREKA_URL`, `GOOGLE_OAUTH_CLIENT_ID` |
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
- `GOOGLE_OAUTH_CLIENT_ID` (mismo Client ID que auth-service y frontend)

### `frontend`

No tiene variables de entorno declaradas actualmente en `docker-compose.yml`.

## Antes de levantar Compose

Definí al menos la variable requerida por el auth-service:

```bash
export GOOGLE_OAUTH_CLIENT_ID="tu-client-id.apps.googleusercontent.com"
```

Las variables de PostgreSQL pueden definirse con `DB_USERNAME` y `DB_PASSWORD`; Compose tiene
valores predeterminados para desarrollo local.

### Kafka

`KAFKA_BOOTSTRAP_SERVERS` lo fija Compose a `kafka-1:29092,kafka-2:29092,kafka-3:29092` (listeners
internos de la red `backend`), así que no hace falta definirlo en `.env`. Al ejecutar un servicio
fuera de Docker, su valor por defecto es `localhost:9092,localhost:9093,localhost:9094` (listeners
externos que publica `docker-compose.kafka.yml`).

