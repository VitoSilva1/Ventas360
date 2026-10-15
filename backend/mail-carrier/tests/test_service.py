import logging
import smtplib
from email import message_from_bytes
from unittest.mock import MagicMock

import pytest

from mail_carrier.config import SmtpConfig
from mail_carrier.models import EmailPayload
from mail_carrier.service import MailSendError, build_message, send_email

PASSWORD = "s3cret-pass"
HTML = "<p>Hola ñandú, ¡cuánto tiempo!</p>"


def cfg(**kw):
    base = dict(host="smtp.test", port=587, user="ventas@gmail.com", app_password=PASSWORD, timeout=7.0)
    return SmtpConfig(**{**base, **kw})


def payload(**kw):
    return EmailPayload.parse({"to": ["a@example.com"], "subject": "Hi", "html": HTML, **kw})


def fake_smtp():
    smtp = MagicMock()
    smtp.__enter__.return_value = smtp
    factory = MagicMock(return_value=smtp)
    return factory, smtp


# --- build_message ---

def test_multipart_alternative_with_text():
    msg = build_message(payload(text="plain"), cfg())
    assert msg.get_content_type() == "multipart/alternative"
    assert msg.get_body(("plain",)).get_content().strip() == "plain"
    assert msg.get_body(("html",)).get_content().strip() == HTML


def test_html_only_without_text():
    msg = build_message(payload(), cfg())
    assert msg.get_content_type() == "text/html"
    assert msg.get_body(("plain",)) is None


def test_utf8_round_trip():
    parsed = message_from_bytes(build_message(payload(), cfg()).as_bytes())
    assert parsed.get_payload(decode=True).decode("utf-8").strip() == HTML


def test_from_is_bare_address_without_name():
    assert build_message(payload(), cfg())["From"] == "ventas@gmail.com"


def test_from_has_display_name():
    assert build_message(payload(), cfg(from_name="Ventas360"))["From"] == "Ventas360 <ventas@gmail.com>"


def test_all_recipients_in_to_and_subject_set():
    msg = build_message(payload(to=["a@example.com", "b@example.com"]), cfg())
    assert msg["To"] == "a@example.com, b@example.com"
    assert msg["Subject"] == "Hi"


# --- send_email ---

def test_send_sequence_and_timeout():
    factory, smtp = fake_smtp()
    send_email(payload(), cfg(), smtp_factory=factory)
    factory.assert_called_once_with("smtp.test", 587, timeout=7.0)
    names = [c[0] for c in smtp.method_calls]
    assert names.index("starttls") < names.index("login") < names.index("send_message")
    smtp.login.assert_called_once_with("ventas@gmail.com", PASSWORD)
    smtp.send_message.assert_called_once()
    assert smtp.send_message.call_args.args[0]["To"] == "a@example.com"


@pytest.mark.parametrize(
    "where,exc",
    [
        ("login", smtplib.SMTPAuthenticationError(535, b"bad credentials")),
        ("starttls", TimeoutError("timed out")),
        ("send_message", smtplib.SMTPRecipientsRefused({"a@example.com": (550, b"no")})),
        ("send_message", smtplib.SMTPServerDisconnected("gone")),
    ],
)
def test_failures_map_to_mail_send_error_with_cause(where, exc):
    factory, smtp = fake_smtp()
    getattr(smtp, where).side_effect = exc
    with pytest.raises(MailSendError) as e:
        send_email(payload(), cfg(), smtp_factory=factory)
    assert e.value.__cause__ is exc
    assert PASSWORD not in str(e.value)


def test_connection_refused_maps_to_mail_send_error():
    factory = MagicMock(side_effect=ConnectionRefusedError("refused"))
    with pytest.raises(MailSendError) as e:
        send_email(payload(), cfg(), smtp_factory=factory)
    assert isinstance(e.value.__cause__, ConnectionRefusedError)


def test_error_message_hides_password_even_if_server_echoes_it():
    factory, smtp = fake_smtp()
    smtp.login.side_effect = smtplib.SMTPAuthenticationError(535, PASSWORD.encode())
    with pytest.raises(MailSendError) as e:
        send_email(payload(), cfg(), smtp_factory=factory)
    assert PASSWORD not in str(e.value)


def test_logs_exclude_password_and_html(caplog):
    caplog.set_level(logging.DEBUG)
    factory, smtp = fake_smtp()
    send_email(payload(), cfg(), smtp_factory=factory)
    smtp.login.side_effect = smtplib.SMTPAuthenticationError(535, b"x")
    with pytest.raises(MailSendError):
        send_email(payload(), cfg(), smtp_factory=factory)
    assert caplog.records
    assert PASSWORD not in caplog.text and HTML not in caplog.text


def test_starttls_uses_verifying_context():
    import ssl

    factory, smtp = fake_smtp()
    send_email(payload(), cfg(), smtp_factory=factory)
    ctx = smtp.starttls.call_args.kwargs["context"]
    assert ctx.verify_mode == ssl.CERT_REQUIRED and ctx.check_hostname
