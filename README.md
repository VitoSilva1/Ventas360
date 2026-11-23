# Ventas 360

Plataforma de gestión de ventas basada en una arquitectura de microservicios.

## Estado del proyecto

Actualmente se encuentra implementada la base del backend y se incorporó un frontend inicial con Angular. La integración completa entre ambas capas continúa en desarrollo.

## Arquitectura actual

El backend contempla los siguientes componentes:

- **API Gateway**: punto de entrada para las peticiones de los usuarios y enrutamiento hacia los microservicios.
- **Service Registry (Eureka)**: registro y descubrimiento de servicios.
- **auth-service**: gestión de autenticación y usuarios, con su base de datos `db-auth`.
- **sales-service**: gestión de ventas, con su base de datos `db-service`.
- **product-service**: gestión de productos, con su base de datos `db-product`.
- **Resilience4j**: tolerancia a fallos mediante el patrón Circuit Breaker en la comunicación entre servicios.

- **analytics-service**: consume los tópicos de Kafka y expone métricas de ventas (`/analytics/summary`).
- **kafka-admin**: administración del clúster Kafka (tópicos, configuraciones y consumer groups).
- **RabbitMQ**: mensajería de comandos y notificaciones (registro de ventas y correo de confirmación).
- **Kafka**: streaming de eventos de negocio para analítica e inventario (ver sección *Streaming con Kafka*).

La comunicación síncrona se realiza mediante APIs HTTP/REST. El API Gateway expone las rutas principales:

- `/auth/`
- `/products/`
- `/sales/`
- `/analytics/`
- `/kafka-admin/`

## Próximos pasos

- Integrar el frontend Angular con el API Gateway mediante HTTP/REST.
- Completar y validar los flujos de autenticación, productos y ventas.

## Ejecución con Docker Desktop

Con Docker Desktop iniciado, desde la raíz del proyecto:

```bash
make up
# equivalente a:
docker compose -f docker-compose.yml -f docker-compose.kafka.yml up --build -d
```

`docker-compose.kafka.yml` levanta la plataforma Kafka y comparte la red `backend` con el resto
del stack, por eso ambos archivos se usan juntos. La plataforma Kafka también puede levantarse sola
con `make kafka-up`.

La aplicación quedará disponible en:

- Frontend: `http://localhost:4200` (o mediante el proxy HTTPS del servidor)
- API Gateway: `http://localhost:8080`
- Consola de RabbitMQ: `http://localhost:15672` (usuario/contraseña por defecto: `guest`/`guest`)
- Kafka-UI: `http://localhost:8090`
- Brokers Kafka para clientes locales: `localhost:9092`, `localhost:9093`, `localhost:9094`
- Eureka, PostgreSQL y los microservicios no publican puertos en el host.

Para detener los contenedores:

```bash
make down
```

Los datos de PostgreSQL se conservan en el volumen `ventas360-postgres-data`. Para eliminar también ese volumen:

`bash
docker compose down -v
`

## Estructura del repositorio

```text
backend/
├── analytics-service/   # consumidor Kafka (analytics-group) + métricas
├── api-gateway/
├── auth-service/
├── kafka-admin/         # administración de Kafka (AdminClient)
├── mail-carrier/        # consumidor RabbitMQ de correos (Python)
├── product-service/     # productor product-events + consumidor inventory-group
├── rabbit-mq/           # productor y administración de RabbitMQ
├── sales-service/       # consumidor RabbitMQ + productor sales-completed
└── service-registry/

docker/kafka/create-topics.sh   # creación idempotente de tópicos
docker-compose.kafka.yml        # Zookeeper, 3 brokers, Kafka-UI

frontend/
└── Angular
```

## Streaming con Kafka

Kafka **complementa** a RabbitMQ sin reemplazarlo. RabbitMQ sigue transportando el comando
"registrar venta" y la notificación por correo. Kafka guarda el **registro de eventos de negocio**
que alimenta la analítica y el inventario. Si Kafka no está disponible, la venta igual se registra
por RabbitMQ: el error se deja en el log y el mensaje de RabbitMQ no se rechaza.

```text
Frontend ──POST /sales/events──► rabbit-mq ──fanout──► sales.queue ──► sales-service ──┐
                                                └────► mail.queue  ──► mail-carrier     │ SaleCompletedEvent
                                                                                       ▼
product-service ──ProductChangedEvent──► [ventas360.product.events]    [ventas360.sales.completed]
       ▲                                         │                        │            │
       │                                         ▼                        ▼            ▼
       └──── inventory-group (descuenta stock) ◄─┼────────────────────────┘     analytics-group
                                                 └──────────────────────────────► analytics-service
```

### Infraestructura (`docker-compose.kafka.yml`)

- 1 Zookeeper y 3 brokers (imágenes Confluent 7.7, Kafka 3.7). Kafka 4.x ya no soporta Zookeeper.
- Cada broker tiene un listener interno `kafka-N:29092` (red Docker) y uno externo `localhost:909N`.
- Valores por defecto del clúster: factor de replicación 3, `min.insync.replicas=2` y creación
  automática de tópicos desactivada.
- `kafka-topics-init` es un job que corre una sola vez y ejecuta `docker/kafka/create-topics.sh`.
- Kafka-UI en `http://localhost:8090` para administrar brokers, tópicos y consumer groups.

### Tópicos

| Tópico | Productor | Clave | Particiones / RF | Configuración | Motivo |
|---|---|---|---|---|---|
| `ventas360.sales.completed` | sales-service | `saleId` | 3 / 3 | `cleanup.policy=delete`, `retention.ms=7 días` | Flujo de eventos que se puede reprocesar durante una semana |
| `ventas360.product.events` | product-service | `productId` | 3 / 3 | `cleanup.policy=compact`, `min.compaction.lag.ms=60000` | Guarda el último estado de cada producto; un consumidor nuevo reconstruye el catálogo |
| `*.DLT` | error handlers | misma clave | 3 / 3 | `delete`, 14 días | Mensajes que fallaron, para inspeccionarlos y reprocesarlos |

`create-topics.sh` usa `--if-not-exists`: volver a ejecutarlo no tiene efectos, pero tampoco
cambia la configuración de un tópico que ya existe. Para modificarla se usa `kafka-admin`
o Kafka-UI.

### Productores y consumidores

- **Clientes:** usan `JacksonJsonSerializer` sin type headers. Cada servicio deserializa a su propio
  DTO. Los productores usan `acks=all`, idempotencia, compresión `lz4` y `max.block.ms=5000`.
- **Consumer groups:** `analytics-group` (analytics-service) e `inventory-group` (product-service)
  leen el mismo tópico de ventas. Cada grupo recibe todos los eventos, y dentro de un grupo las
  3 particiones se reparten entre las instancias (`concurrency=3`, `CooperativeStickyAssignor`).
- **Errores:**
  - `ErrorHandlingDeserializer` atrapa los JSON inválidos y la validación Jakarta rechaza los payloads incompletos.
  - Los fallos transitorios se reintentan con backoff exponencial (0.5 s, 1 s, 2 s).
  - Lo que sigue fallando se registra en el log y se publica en `<tópico>.DLT`, en la misma partición, sin bloquear los mensajes siguientes.
- **Idempotencia:** analytics usa el `saleId` como clave primaria, e inventario guarda una marca de
  venta procesada en la misma transacción que descuenta el stock, con un `UPDATE` atómico.
- **Logs:** formato ECS (JSON). Mientras se procesa un mensaje, cada línea incluye `kafka.topic`,
  `kafka.partition`, `kafka.offset` y `kafka.key`.

### Administración (`/kafka-admin`, requiere JWT)

| Operación | Endpoint |
|---|---|
| Estado del clúster | `GET /kafka-admin/cluster` |
| Listar / detallar tópicos | `GET /kafka-admin/topics`, `GET /kafka-admin/topics/{name}` |
| Crear tópico | `POST /kafka-admin/topics` `{"name","partitions","replicationFactor","configs":{}}` |
| Modificar configuración | `PATCH /kafka-admin/topics/{name}/configs` `{"set":{},"delete":[]}` |
| Aumentar particiones | `POST /kafka-admin/topics/{name}/partitions` `{"totalCount"}` |
| Eliminar tópico | `DELETE /kafka-admin/topics/{name}` |
| Consumer groups y lag | `GET /kafka-admin/consumer-groups`, `GET /kafka-admin/consumer-groups/{id}` |
| Resetear offsets (grupo inactivo) | `POST /kafka-admin/consumer-groups/{id}/offsets/reset` `{"topic","strategy":"EARLIEST\|LATEST"}` |
| Eliminar consumer group | `DELETE /kafka-admin/consumer-groups/{id}` |

Los tópicos internos (`__*`) no se pueden crear, modificar, reparticionar ni eliminar.

Comandos útiles: `make kafka-topics` (describe los tópicos y su configuración) y `make logs`.

## Autenticación y acceso a los servicios

El API Gateway valida el ID token JWT de Google enviado en `Authorization: Bearer <token>`.
Comprueba firma, vigencia, emisor, audiencia (`GOOGLE_OAUTH_CLIENT_ID`) y perfil con correo verificado.
Todas las rutas requieren token excepto `POST /auth/google/sign-in`, `GET /actuator/health`,
`GET /actuator/info` y solicitudes `OPTIONS`. Auth-service conserva su validación del mismo token.

En Compose, productos, ventas, autenticación, Eureka y PostgreSQL solo están conectados a la
red backend y no publican puertos. El frontend usa la red edge y accede al backend a través
del Gateway. La red backend permite salida a Internet para obtener las claves de Google.
Esto impide acceso externo directo por los puertos del host; el host Docker y los contenedores
con acceso a la red backend deben ser de confianza. Ejecutar los servicios fuera de Compose
requiere configurar por separado su firewall y acceso de red.

Define `GOOGLE_OAUTH_CLIENT_ID` antes de iniciar. Para aplicar cambios en un despliegue Compose:

```bash
docker compose up -d --build
```

El cambio solo entra en vigor al recrear los contenedores. Los permisos por usuario o rol
no se implementan con esta validación: cualquier usuario con un token aceptado puede acceder
a las rutas protegidas, sujeto a las reglas de negocio de cada servicio.
