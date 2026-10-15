import logging
import smtplib
import ssl
from email.message import EmailMessage
from email.utils import formataddr

from mail_carrier.config import SmtpConfig
from mail_carrier.models import EmailPayload

log = logging.getLogger(__name__)


class MailSendError(Exception):
    """Any send-time failure. The original exception is chained as __cause__."""


def build_message(p: EmailPayload, cfg: SmtpConfig) -> EmailMessage:
    msg = EmailMessage()
    msg["From"] = formataddr((cfg.from_name, cfg.user)) if cfg.from_name else cfg.user
    msg["To"] = ", ".join(p.to)
    msg["Subject"] = p.subject
    if p.text is not None:
        msg.set_content(p.text)
        msg.add_alternative(p.html, subtype="html")
    else:
        msg.set_content(p.html, subtype="html")
    return msg


def send_email(p: EmailPayload, cfg: SmtpConfig, *, smtp_factory=smtplib.SMTP) -> None:
    msg = build_message(p, cfg)
    try:
        with smtp_factory(cfg.host, cfg.port, timeout=cfg.timeout) as smtp:
            smtp.starttls(context=ssl.create_default_context())
            smtp.login(cfg.user, cfg.app_password)
            smtp.send_message(msg)
    except (smtplib.SMTPException, OSError) as e:
        # Only the exception class is reported: server replies could echo credentials.
        log.error("SMTP send failed: %s", type(e).__name__)
        raise MailSendError(f"SMTP send failed: {type(e).__name__}") from e
