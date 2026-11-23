# Feature: Kafka streaming and analytics (EP5 DSY1107)

Objective: add Kafka (Zookeeper + 3 brokers + Kafka-UI) as an analytics event stream that complements RabbitMQ without changing it. Branch: `feature/kafka-streaming` (builds on a teammate's commit 0e48e2d). Delivery: `ask-on-risk`, chained PRs (~4 slices). Test runner: `./mvnw test` per service, run inside `maven:3.9-eclipse-temurin-21` via Docker (no local JDK).

## Specs

Requirements quoted verbatim from `EP5_DSY1107_Estudiante_encargo.pdf`:

- **S1 Infra**: "El proyecto debe incluir un archivo docker-compose funcional para levantar una arquitectura Kafka con Zookeeper y brokers." and "El proyecto debe incluir un archivo docker-compose funcional para levantar Kafka-ui y permitir la administración de brokers y tópicos". Design: `docker-compose.kafka.yml` with 1 Zookeeper, 3 brokers, Kafka-UI and a topic-init job. It runs together with `docker-compose.yml` on the shared `backend` network.
- **S2 Client config**: "configuración completa del cliente Kafka, con conexión a los brokers levantados en Docker Compose y propiedades que permiten publicar y consumir mensajes desde los tópicos con sus particiones y réplicas definidas", including "bootstrap servers serializers consumer groups y propiedades avanzadas".
- **S3 Two topics + DTOs**: "productores y consumidores que interactúan con los dos tópicos configurados, utilizando estructuras tipadas como DTO". Topics:
  - `ventas360.sales.completed`: `delete`, 7-day retention, 3 partitions, RF 3, key `saleId`.
  - `ventas360.product.events`: `compact`, 3 partitions, RF 3, key `productId`.
- **S4 Producers**: "productores Kafka correctamente implementados, enviando eventos a los tópicos configurados con uso de DTO bien estructurados y serializers adecuados".
  - sales-service publishes `SaleCompletedEvent` after it persists a sale.
  - product-service publishes `ProductChangedEvent` on create, update and delete.
- **S5 Consumers**: "consumidores Kafka que procesan eventos de forma coherente con los flujos de negocio, utilizando @KafkaListener u otra API equivalente y manteniendo la lógica de procesamiento separada de la infraestructura de mensajería".
  - The new `analytics-service` consumes both topics.
  - product-service consumes sales to decrement stock idempotently.
- **S6 Error handling**: "manejo adecuado de errores durante el procesamiento de eventos, utilizando estrategias como logging, validación de payloads o control de reintentos que aseguran que los mensajes defectuosos o inesperados no interrumpan el funcionamiento del sistema". Design: `DefaultErrorHandler` with backoff, then `DeadLetterPublishingRecoverer` to `<topic>.DLT`, with contextual logs (topic, partition, offset, key, eventId).
- **S7 Topic admin params**: "parámetros de administración de tópicos tales como niveles de retención, cleanup policy, número de particiones y factor de replicación ajustados a los requerimientos de los distintos flujos de datos".
- **S8 Consumer groups**: "consumer groups adecuados para balancear carga o asegurar procesamiento resiliente permitiendo que distintos microservicios compartan o dividan responsabilidades según el caso de uso". Groups: `analytics-group` and `inventory-group`.
- **S9 Admin microservice**: "El microservicio administrador de Kafka debe permitir la creación, modificación y eliminación de tópicos y configuraciones." plus "operaciones para crear, actualizar o inspeccionar tópicos y grupos de consumidores". Design: new `kafka-admin` service using `AdminClient`, routed through the gateway (JWT protected).
- **S10 Logging**: structured ECS logs with Kafka context (MDC) in the services that touch Kafka. This is agreed design, not quoted from the PDF.
- **S11 Frontend**: analytics summary card on the Angular dashboard that consumes `GET /analytics/summary`. This is agreed design.
- **S12 Docs**: README, `ENVIRONMENT_VARIABLES.md` and `.env.example` updated for Kafka.
- **Constraint**: "Kafka complementa la arquitectura existente sin afectar las integraciones previas con RabbitMQ". The RabbitMQ flow must keep working unchanged.

## Tasks

| ID | Specs | Route | Status | Commit / evidence |
|---|---|---|---|---|
| T1 | S1, S7 | inline | done | fdc35f6. Verified: compose config OK; 3 brokers + ZK healthy; 4 topics with RF3 and expected configs; Kafka-UI online (3 brokers, 12 partitions). RDD: high, consent declined by user |
| T2 | S2, S3, S4, S10 (producers) | inline | done | see T2 commit. sales-service 18/18, product-service 6/6 (incl. EmbeddedKafka round-trip). RED not observed separately: tests and code compiled in the same first run |
| T3 | S5, S6, S8, S10 (analytics-service) | inline | done | see T3 commit. analytics-service 8/8 (domain + EmbeddedKafka: both topics consumed, malformed JSON and invalid payload land in .DLT without blocking later events); api-gateway 10/10 after adding /analytics/** route. RED observed: DLT tests failed first (Spring Kafka 4 default "-dlt" suffix), fixed with explicit resolver |
| T4 | S5, S6, S8 (inventory consumer) | inline | done | see T4 commit. product-service 13/13 (InventoryServiceTest 5: decrement, idempotent, clamp at 0, unknown product skipped, UPDATED event; EmbeddedKafka: stock decremented via inventory-group, invalid payload -> .DLT); sales-service 18/18 after max.block.ms change |
| T5 | S9 (kafka-admin) | inline | todo | |
| T6 | S10 (ECS logging across Kafka services) | inline | todo | |
| T7 | S11 | inline | todo | |
| T8 | S12 | inline | todo | |

Risk notes:
- T1 changes delivery infra (high-risk item 5).
- T4 mutates stock (stored data).
- T5 exposes destructive operations, so it must be behind JWT (item 2).

## Log

- **L1 (2026-11-23, user)**: "Si, entregado a tus recomendaciones. Mis compas me abandonaron y de kafka se lo justo. Te sigo. Te aviso cuando Docker Desktop este instalado"
  - Recommendations accepted: a new analytics-service; two separate ODD cycles (Kafka here, RabbitMQ leftovers later); ECS contextual logging; no ELK stack.
- **L2**: Found the teammate branch `origin/feature/kafka-streaming` (commit 0e48e2d). It has a single KRaft broker and no Zookeeper, which does not meet S1. Work continues on that branch.
- **L3**: The environment has no JDK and Docker is not installed yet. Maven verification will run in a Docker `maven:3.9-eclipse-temurin-21` container once Docker Desktop is available.
- **L4 (T1 decisions)**:
  - Confluent `cp-zookeeper`/`cp-kafka` 7.7.1 (Kafka 3.7) are used because Kafka 4.x dropped Zookeeper.
  - Brokers use an INTERNAL listener on 29092 (Docker network) and EXTERNAL listeners on localhost:9092, 9093 and 9094.
  - Cluster defaults: RF 3, `min.insync.replicas=2`, auto topic creation off.
  - Topics are created by the `kafka-topics-init` one-shot job from `docker/kafka/create-topics.sh`. DLT topics keep 3 partitions because `DeadLetterPublishingRecoverer` preserves the source partition.
  - `.gitattributes` forces LF on `*.sh` (`core.autocrlf=true` on this machine).
  - Kafka-UI is on 127.0.0.1:8090.
  - The Makefile `up`/`down`/`logs` targets now include both compose files; new targets: `kafka-up`, `kafka-down`, `kafka-topics`.
- **L5 (resolved, from Spring Kafka 4.1 docs via context7)**:
  - Use the Jackson 3 classes: `JacksonJsonSerializer`/`JacksonJsonDeserializer` replace the deprecated `JsonSerializer`/`JsonDeserializer`, and `DefaultJacksonJavaTypeMapper` replaces `DefaultJackson2JavaTypeMapper`.
  - DTOs live in different packages per service. Producers will use `noTypeInfo()`; consumers will use `ignoreTypeHeaders()` with a fixed target type, wrapped in `ErrorHandlingDeserializer` so a poison payload goes to the DLT instead of blocking the partition.
  - Compile in Docker before committing.
- **L6 (T2 decisions)**:
  - sales-service publishes `SaleCompletedEvent` (key = sale DB id) synchronously from the RabbitMQ listener. A Kafka failure propagates, RabbitMQ retries, and the duplicate branch re-publishes the stored sale (at-least-once without an outbox).
  - `POST /sales` publishes asynchronously and logs failures, so a committed sale never becomes a 500.
  - product-service raises a `ProductChangedEvent` domain event, relayed by `@TransactionalEventListener(AFTER_COMMIT)`. Key = productId; CREATED/UPDATED/DELETED.
  - Kafka client config lives in `kafka.yaml`, imported by the main and test `application.yaml`, because the test `application.yaml` shadows the main one.
  - `rocksdbjni` (~72 MB, Kafka Streams only) is excluded from `spring-boot-starter-kafka-test`.
  - The network here is ~65 KB/s, so Maven runs use the `ventas360-m2` volume plus `-Daether.connector.http.retryHandler.count=5`.
  - Pending follow-up (pre-existing): `SaleEventConsumerService` sets `saleEventId` in a second save after `create()` commits, so it is not atomic.
- **L7 (T3 decisions)**:
  - The new `analytics-service` (port 8085) persists its read model in Postgres (`analytics_sales`, `analytics_sale_lines`, `analytics_catalog_products`). With shared storage, instances in `analytics-group` split partitions and still return a complete summary.
  - Top products are a `GROUP BY` over sale lines (no counters, so no lost updates). Idempotency comes from the PK = sales-service sale id.
  - Two `@KafkaListener` adapters, each with concurrency 3 (one consumer per partition), validate payloads with Jakarta Validation and delegate to `AnalyticsService`, which has no Kafka types.
  - Consumer config: `ErrorHandlingDeserializer` wrapping `JacksonJsonDeserializer` (no type headers; a per-listener default type), `CooperativeStickyAssignor`, manual commits (`ack-mode: record`), `earliest`.
  - Error handling: `DefaultErrorHandler` with exponential backoff (0.5s x2, 3 attempts) and retry logs. `InvalidEventException` and `DeserializationException` are not retried. The recoverer logs the error and publishes to `<topic>.DLT` on the same partition, using a `DelegatingByTypeSerializer` (`byte[]` or JSON).
  - **Gotcha**: Spring Kafka 4's `DeadLetterPublishingRecoverer` defaults to the `-dlt` suffix, so an explicit resolver is required (saved to Engram).
  - Gateway route `/analytics/**` → `lb://analytics-service` (JWT protected like the rest). Compose service `analytics-service` was added.
- **L8 (T4 decisions)**:
  - product-service consumes `ventas360.sales.completed` in `inventory-group` (concurrency 3). With `analytics-group` reading the same topic, each group receives every event.
  - Stock is decremented with an atomic JPQL `UPDATE ... CASE` (clamped at 0), so concurrent sales from different partitions cannot lose updates.
  - Idempotency: the `inventory_processed_sales` row (PK saleId) is written in the same transaction as the decrements.
  - Each decrement raises `ProductChangedEvent` UPDATED, which the AFTER_COMMIT relay sends to Kafka, so analytics sees low stock.
  - Error handling mirrors analytics (`.DLT` resolver). The DLT producer is private to the config so it does not replace the auto-configured `KafkaTemplate` used by the relay.
  - Resilience fix to T2 producers: `max.block.ms=5000` (the default of 60s would block HTTP or RabbitMQ threads while Kafka is down).
  - The test yaml disables listener auto-startup; listener tests re-enable it with an embedded broker.
