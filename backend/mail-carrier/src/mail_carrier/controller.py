from collections.abc import Callable

from mail_carrier.models import EmailPayload


def handle(raw: str | bytes, send: Callable[[EmailPayload], None]) -> None:
    send(EmailPayload.parse_json(raw))
