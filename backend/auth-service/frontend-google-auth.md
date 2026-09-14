# Integración de Google Sign-In en el frontend

Esta guía muestra la integración más simple con el auth-service. Google se encarga de seleccionar o crear la cuenta; el frontend envía el ID token de Google al backend.

## Ruta rápida

1. Creá un cliente OAuth de tipo **Web application** y agregá el origen del frontend.
2. Cargá Google Identity Services en la página.
3. Enviá el `credential` obtenido al backend.
4. Enviá ese mismo ID token como Bearer token a las rutas protegidas.
5. Al cerrar sesión, eliminá el token local y el estado de selección automática de Google.

## 1. Configurá el Client ID

Usá el Client ID OAuth generado en Google Cloud Console:

```text
1234567890-example.apps.googleusercontent.com
```

El Client ID es público. No pongas un client secret en el código del navegador.

Agregá el origen exacto del frontend en **Authorized JavaScript origins**, por ejemplo:

```text
http://localhost:5173
```

Un origen contiene sólo protocolo, host y puerto. No agregues `/login` ni otra ruta.

## 2. Ejemplo HTML mínimo

```html
<script src="https://accounts.google.com/gsi/client" async defer></script>

<div
  id="g_id_onload"
  data-client_id="TU_CLIENT_ID.apps.googleusercontent.com"
  data-callback="handleGoogleCredential"
  data-auto_prompt="false">
</div>

<div class="g_id_signin" data-type="standard"></div>

<script>
  let idToken = null;

  async function handleGoogleCredential(response) {
    // response.credential es un ID token OpenID Connect firmado por Google.
    const signInResponse = await fetch('http://localhost:8081/api/auth/google/sign-in', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ credential: response.credential })
    });

    if (!signInResponse.ok) {
      throw new Error('Falló el inicio de sesión con Google');
    }

    idToken = response.credential;
    const profile = await signInResponse.json();
    console.log('Sesión iniciada como:', profile.email);
  }

  async function callProtectedEndpoint() {
    const response = await fetch('http://localhost:8081/api/protected-resource', {
      headers: { Authorization: `Bearer ${idToken}` }
    });

    if (response.status === 401) {
      idToken = null;
      throw new Error('La sesión expiró o el token no es válido');
    }

    return response;
  }

  async function logout() {
    if (idToken) {
      await fetch('http://localhost:8081/api/auth/logout', {
        method: 'POST',
        headers: { Authorization: `Bearer ${idToken}` }
      });
    }

    idToken = null;
    google.accounts.id.disableAutoSelect();
  }
</script>
```

## 3. Contrato del backend

### Inicio de sesión

```http
POST /api/auth/google/sign-in
Content-Type: application/json
```

```json
{ "credential": "GOOGLE_ID_TOKEN" }
```

El backend verifica la firma, issuer, audience, expiración, `email_verified`, email y `sub` del
token. Nunca confíes en los claims de un token sin verificarlo criptográficamente.

### Solicitudes protegidas

```http
Authorization: Bearer GOOGLE_ID_TOKEN
```

### Cierre de sesión

```http
POST /api/auth/logout
Authorization: Bearer GOOGLE_ID_TOKEN
```

El endpoint devuelve `204`. Este logout es local/stateless: el frontend elimina el token y
desactiva la selección automática de cuenta. No elimina la cuenta Google ni revoca un ID token ya
emitido; el token sigue siendo utilizable hasta que expire.

## Checklist

- [ ] `GOOGLE_OAUTH_CLIENT_ID` está configurado en el backend.
- [ ] El origen del frontend está configurado en el cliente OAuth de Google.
- [ ] El frontend envía `response.credential` como `credential`.
- [ ] Las solicitudes protegidas incluyen `Authorization: Bearer <token>`.
- [ ] Logout elimina el token de la memoria del frontend.
- [ ] Ningún client secret ni token está commiteado en el repositorio.

