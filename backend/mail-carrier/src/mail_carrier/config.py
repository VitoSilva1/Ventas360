from collections.abc import Mapping
from dataclasses import dataclass, field


class ConfigError(Exception):
    """Missing or invalid SMTP configuration. Messages never include values."""


@dataclass(frozen=True)
class SmtpConfig:
    host: str
    port: int
    user: str
    app_password: str = field(repr=False)
    from_name: str | None = None
    timeout: float = 10.0


def _required(env: Mapping[str, str], name: str) -> str:
    value = env.get(name, "").strip()
    if not value:
        raise ConfigError(f"Missing required environment variable: {name}")
    return value


def load_config(env: Mapping[str, str]) -> SmtpConfig:
    try:
        port = int(env.get("SMTP_PORT") or 587)
    except ValueError:
        raise ConfigError("SMTP_PORT must be an integer") from None
    return SmtpConfig(
        host=env.get("SMTP_HOST") or "smtp.gmail.com",
        port=port,
        user=_required(env, "SMTP_USER"),
        app_password=_required(env, "SMTP_APP_PASSWORD"),
        from_name=env.get("SMTP_FROM_NAME") or None,
    )
