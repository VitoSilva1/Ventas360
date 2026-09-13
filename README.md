# Ventas 360

Plataforma de gestión de ventas basada en una arquitectura de microservicios.

## Estado del proyecto

Actualmente se ha definido el diagrama de arquitectura del backend. El frontend todavía no está implementado; será incorporado posteriormente utilizando Angular.

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

- Implementar el frontend con Angular.
- Integrar el frontend con el API Gateway mediante HTTP/REST.
- Completar y validar los flujos de autenticación, productos y ventas.

## Estructura del repositorio

```text
backend/
├── api-gateway/
├── auth-service/
├── product-service/
├── sales-service/
└── service-registry/
```
