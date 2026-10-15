## Descripción

- Este sistema debe tener la capacidad de conectarse a Gmail para enviar correos electrónicos. Así de simple.
- Implementación: módulo independiente en `backend/mail-carrier/` que envía un correo HTML por SMTP (Gmail) con la librería estándar `smtplib`.

## Instrucciones

- Este módulo debe acoplarse a la carpeta `backend` como un módulo completamente aparte, con su propia estructura.

**Tecnologías**

1. Python 3.12
2. `smtplib` / `email` (biblioteca estándar) para el envío por SMTP
3. Pydantic (`pydantic[email]`) para validar el payload

- El resto fue definido por el agente.

### Decisión: SMTP en lugar de la API de Gmail con httpx

- La versión inicial proponía `httpx` contra la API de Gmail. Se optó por SMTP con `smtplib` y una contraseña de aplicación de Google, porque evita OAuth y dependencias adicionales. La única dependencia de ejecución es `pydantic[email]`.
- Se requiere una cuenta de Google con verificación en dos pasos y una contraseña de aplicación.

## Arquitectura

- Como es un módulo sencillo, se eligió una arquitectura por capas para desacoplar la lógica y hacer legible el módulo.

1. Modelos (`models.py`): modelos de entrada (`EmailPayload`) y su error (`InvalidPayloadError`).
2. Lógica de negocio / servicio (`service.py`): construcción del mensaje y envío (`build_message`, `send_email`, `MailSendError`).
3. Controlador (`controller.py`): invoca la función de envío y se abstrae de la lógica de negocio (`handle`).

- `config.py` carga la configuración SMTP desde variables de entorno; `__main__.py` es la CLI y el único lugar que traduce excepciones a códigos de salida.

### Payload

JSON con `to` (lista de correos, mínimo 1), `subject` (sin saltos de línea), `html` (obligatorio) y `text` (opcional; si existe se envía multipart/alternative). Campos desconocidos (`cc`, `bcc`, `from`, etc.) se rechazan. El remitente siempre es `SMTP_USER`.

### Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `SMTP_HOST` | No | Por defecto `smtp.gmail.com` |
| `SMTP_PORT` | No | Por defecto `587` (STARTTLS) |
| `SMTP_USER` | Sí | Cuenta remitente |
| `SMTP_APP_PASSWORD` | Sí | Contraseña de aplicación de Google |
| `SMTP_FROM_NAME` | No | Nombre visible del remitente |

Use `.env.example` como plantilla. El archivo `.env` nunca se versiona ni se incluye en la imagen.

### Códigos de salida

| Código | Significado |
|---|---|
| 0 | Correo enviado |
| 1 | Fallo de envío (`MailSendError`) o configuración SMTP ausente/inválida (`ConfigError`) |
| 2 | Entrada inválida: payload inválido (`InvalidPayloadError`), JSON mal formado, o archivo inexistente/ilegible |

Los mensajes de error van a stderr y nunca incluyen la contraseña ni el HTML completo.

### Uso

```
cd backend/mail-carrier
pip install -e .[dev]
pytest                                   # SMTP siempre simulado
python -m mail_carrier samples/payload.json
cat samples/payload.json | python -m mail_carrier
```

### Docker

```
cd backend/mail-carrier
cp .env.example .env                     # completar credenciales
docker compose build
docker compose run --rm mail-carrier     # envía samples/payload.json
```

La imagen usa `python:3.12-slim` y se ejecuta con un usuario sin privilegios. El `docker-compose.yml` de la raíz del repositorio no se modifica.

### Trabajo futuro

- Controlador RabbitMQ: un consumidor que ejecute `load_config` al iniciar y llame a `handle(body, send)` por mensaje (ack en éxito, descarte a dead-letter en `InvalidPayloadError`, reintento en `MailSendError`). `handle` y el servicio no cambian.

## IMPORTANTE

- Prohibido elegir respuestas de riesgo sin previa consulta conmigo.
- Toda ambigüedad se debe resolver a través de chat, no de manera aleatoria.
