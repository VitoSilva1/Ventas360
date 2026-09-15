# Frontend — Ventas 360

Aplicación Angular 21 que consume los microservicios del backend de Ventas 360
(API Gateway, Auth Service, Product Service, Sales Service).

---

## Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Node.js | 20 LTS o superior |
| npm | 11 o superior (incluido con Node) |

Verifica tu instalación:

```bash
node -v
npm -v
```

---

## 1. Configurar el Google Client ID

La autenticación usa **Google Identity Services** (OAuth 2.0).
Antes de poder iniciar sesión necesitás un **OAuth 2.0 Web Client ID**.

### Obtener el Client ID

1. Ve a [Google Cloud Console → APIs & Services → Credentials](https://console.cloud.google.com/apis/credentials).
2. Clic en **Create Credentials → OAuth client ID**.
3. Tipo de aplicación: **Web application**.
4. Agrega el origen del frontend en **Authorized JavaScript origins**:
   - Desarrollo local: `http://localhost:4200`
   - Docker local: `http://localhost` (si corresponde)
5. **No** agregues redirect URIs — Google Identity Services no las necesita.
6. Copia el **Client ID** generado (termina en `.apps.googleusercontent.com`).

> **Importante:** Solo el Client ID es público y va en el frontend.
> El Client Secret **nunca** va en el código del navegador.

### Configurar el Client ID en el frontend

Edita el archivo `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  googleClientId: 'TU_CLIENT_ID.apps.googleusercontent.com', // ← Reemplazar aquí
};
```

El mismo Client ID debe configurarse en el backend con la variable `GOOGLE_OAUTH_CLIENT_ID`.

---

## 2. Instalar dependencias

```bash
npm install
```

---

## 3. Levantar el backend

El frontend requiere que los siguientes servicios estén corriendo:

| Servicio | Puerto | Descripción |
|---|---|---|
| `service-registry` | 8761 | Eureka — registro de servicios |
| `auth-service` | (via gateway) | Autenticación Google |
| `product-service` | (via gateway) | Catálogo de productos |
| `sales-service` | (via gateway) | Registro de ventas |
| `api-gateway` | **8080** | Punto de entrada único del frontend |

### Con Docker Compose (recomendado)

Desde la raíz del repositorio:

```powershell
# Variables de entorno requeridas
$env:GOOGLE_OAUTH_CLIENT_ID = "tu-client-id.apps.googleusercontent.com"
$env:DB_USERNAME = "ventas360"
$env:DB_PASSWORD = "ventas360"

# Levantar todos los servicios
docker-compose up -d
```

El API Gateway quedará disponible en `http://localhost:8080`.

### Manual (sin Docker)

Levanta cada servicio en el orden indicado:

```powershell
# 1. Service Registry (Eureka)
cd backend/service-registry; ./mvnw spring-boot:run

# 2. Auth Service
cd backend/auth-service
$env:GOOGLE_OAUTH_CLIENT_ID = "tu-client-id.apps.googleusercontent.com"
./mvnw spring-boot:run

# 3. Product Service
cd backend/product-service; ./mvnw spring-boot:run

# 4. Sales Service
cd backend/sales-service; ./mvnw spring-boot:run

# 5. API Gateway
cd backend/api-gateway; ./mvnw spring-boot:run
```

---

## 4. Levantar el frontend

```bash
npm start
```

El servidor de desarrollo arranca en `http://localhost:4200` con **proxy habilitado**:
todas las peticiones a `/api/*` se reenvían automáticamente a `http://localhost:8080`.

Configuración del proxy en `proxy.conf.json`:

```
/api  →  http://localhost:8080  (reescribe el prefijo /api)
```

---

## 5. Probar el flujo de autenticación

1. Abre `http://localhost:4200` en el navegador.
2. Serás redirigido automáticamente a `/login`.
3. Haz clic en el botón **"Iniciar sesión con Google"**.
4. Selecciona tu cuenta Google.
5. Si el backend acepta el token, serás redirigido al dashboard.
6. El token se guarda en `localStorage` bajo la clave `ventas360_id_token`.
7. Al recargar la página, la sesión se restaura automáticamente.
8. El botón **"Salir"** en el topbar cierra la sesión y redirige a `/login`.

### Verificar que el interceptor funciona

Abre las DevTools → pestaña Network y comprueba que las peticiones a `/api/products`
y `/api/sales` incluyen el header:

```
Authorization: Bearer <google-id-token>
```

---

## 6. Estructura del proyecto

```
src/
├── app/
│   ├── guards/
│   │   └── auth.guard.ts          # Protege rutas que requieren sesión
│   ├── interceptors/
│   │   └── auth.interceptor.ts    # Agrega Bearer token a cada petición HTTP
│   ├── layout/                    # ← Fase 2: componentes de layout compartido
│   │   ├── sidebar.ts             # Sidebar con routerLink/routerLinkActive
│   │   ├── sidebar.html
│   │   ├── sidebar.css
│   │   ├── topbar.ts              # Topbar con fecha, usuario y logout
│   │   ├── topbar.html
│   │   └── topbar.css
│   ├── pages/
│   │   ├── login/
│   │   │   ├── login.ts           # Página de login con Google Sign-In
│   │   │   ├── login.html
│   │   │   └── login.css
│   │   ├── products/              # ← Fase 2 (placeholder) / Fase 3 (CRUD real)
│   │   │   ├── product-list.ts    # Lista de productos
│   │   │   ├── product-list.html
│   │   │   └── product-list.css
│   │   └── sales/                 # ← Fase 2 (placeholder) / Fase 4 (real)
│   │       ├── sale-list.ts       # Lista de ventas
│   │       ├── sale-list.html
│   │       └── sale-list.css
│   ├── services/
│   │   ├── auth.service.ts        # Sesión, token, perfil de usuario
│   │   ├── product.service.ts     # Consume /api/products
│   │   └── sale.service.ts        # Consume /api/sales
│   ├── shell.ts                   # Componente raíz — solo contiene <router-outlet>
│   ├── app.ts                     # Dashboard (componente del panel principal)
│   ├── app.html
│   ├── app.css
│   ├── app.routes.ts              # Rutas: /, /login, /dashboard, /products, /sales
│   └── app.config.ts              # Providers: router, http + interceptor
├── environments/
│   ├── environment.ts             # ← Configurar googleClientId aquí
│   └── environment.prod.ts
└── index.html                     # Carga el SDK de Google Identity Services
```

---

## 7. Rutas disponibles

| Ruta | Acceso | Descripción | Estado |
|---|---|---|---|
| `/` | Pública | Redirige a `/dashboard` | ✅ |
| `/login` | Pública | Página de login con Google | ✅ |
| `/dashboard` | 🔒 Protegida | Panel principal: métricas, productos destacados, nueva venta, últimas ventas | ✅ |
| `/products` | 🔒 Protegida | Tabla completa de productos con stock y estado | ✅ |
| `/products/new` | 🔒 Protegida | Formulario para crear nuevo producto | ✅ |
| `/products/:id/edit` | 🔒 Protegida | Formulario para editar producto existente | ✅ |
| `/sales` | 🔒 Protegida | Historial de ventas con filtro por estado | ✅ |
| `/sales/:id` | 🔒 Protegida | Detalle completo de una venta (ítems, subtotales, total) | ✅ |

Las rutas protegidas sin sesión activa redirigen a `/login`.


---

## 8. Build de producción

```bash
npm run build
```

Los artefactos se generan en `dist/frontend/browser/`.
El archivo `nginx.conf` incluye la configuración para servir la SPA correctamente
(todas las rutas desconocidas devuelven `index.html`).

---

## 9. Errores comunes

| Error | Causa probable | Solución |
|---|---|---|
| Botón de Google no aparece | Client ID incorrecto o SDK no cargado | Verifica `environment.ts` y la consola del navegador |
| `401` al hacer login | `GOOGLE_OAUTH_CLIENT_ID` no configurado en el backend | Reiniciar `auth-service` con la variable correcta |
| `CORS error` | Frontend en puerto distinto al configurado | Verifica `allowedOrigins` en `application.yaml` del gateway |
| No conecta a `/api/...` | Backend no está corriendo | Levantar el API Gateway en el puerto 8080 |
| Token no se restaura al recargar | Token de Google expirado (~1h) | Iniciar sesión nuevamente |
