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

La comunicación entre los componentes se realiza mediante APIs HTTP/REST. El API Gateway expone las rutas principales:

- `/auth/`
- `/products/`
- `/sales/`

## Próximos pasos

- Integrar el frontend Angular con el API Gateway mediante HTTP/REST.
- Completar y validar los flujos de autenticación, productos y ventas.

## Ejecución con Docker Desktop

Con Docker Desktop iniciado, desde la raíz del proyecto:

`bash
docker compose up --build
`

La aplicación quedará disponible en:

- Frontend: `http://localhost:8087` (o mediante el proxy HTTPS del servidor)
- API Gateway: `http://localhost:8080`
- Eureka, PostgreSQL y los microservicios no publican puertos en el host.

Para detener los contenedores:

`bash
docker compose down
`

Los datos de PostgreSQL se conservan en el volumen `ventas360-postgres-data`. Para eliminar también ese volumen:

`bash
docker compose down -v
`

## Estructura del repositorio

```text
backend/
├── api-gateway/
├── auth-service/
├── product-service/
├── sales-service/
└── service-registry/

frontend/
└── Angular
```

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
