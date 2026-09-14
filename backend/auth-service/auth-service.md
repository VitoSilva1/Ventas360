# Auth Service

This service authenticates application users with Google OpenID Connect. It does not store
passwords, issue custom JWTs, use Identity Platform email/password, or keep a local user database.

## Flow

1. The frontend uses Google Identity Services. Google displays account selection and, if needed,
   account creation in its own UI.
2. The frontend sends the returned Google ID token as `credential` to `POST /api/auth/google/sign-in`.
3. The backend verifies the JWT signature using Google's public keys, accepted Google issuer,
   configured OAuth client ID audience, expiration, and not-before time.
4. The backend requires `email_verified=true` and uses `sub` as the stable identity. Email is
   returned as profile data, never used as a durable identifier.

The backend cannot create Google Accounts. Account creation and selection only happen in Google's
frontend UI.

## Endpoints

### `POST /api/auth/google/sign-in`

Public endpoint:

```json
{ "credential": "google-oidc-id-token" }
```

Returns the verified profile (`subject`, `email`, optional `name`, optional `picture`). Invalid,
expired, wrongly signed, wrongly issued, wrongly addressed, or unverified-email tokens return 401.

### `POST /api/auth/logout`

Requires `Authorization: Bearer <google-id-token>` and returns `204 No Content`.

Logout is stateless/local: the frontend clears its stored token and Google client session state.
It does not delete a Google Account or revoke a Google ID token server-side; ID tokens expire
naturally and all protected requests validate them cryptographically.

## Configuration

Set the OAuth Web Client ID, not a secret or API key:

```bash
export GOOGLE_OAUTH_CLIENT_ID="your-client-id.apps.googleusercontent.com"
```

Create this client in Google Cloud Console under **APIs & Services → Credentials → Create
Credentials → OAuth client ID → Web application**, then configure the authorized JavaScript origins
and redirect URIs for the frontend that uses Google Identity Services.
