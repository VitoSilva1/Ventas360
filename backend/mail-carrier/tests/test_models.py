import pytest

from mail_carrier.models import EmailPayload, InvalidPayloadError

VALID = {"to": ["a@example.com"], "subject": "Hi", "html": "<p>Hi</p>"}


def test_valid_payload_accepted():
    p = EmailPayload.model_validate({**VALID, "text": "Hi"})
    assert (p.to, p.subject, p.html, p.text) == (["a@example.com"], "Hi", "<p>Hi</p>", "Hi")


def test_text_is_optional():
    assert EmailPayload.model_validate(VALID).text is None


@pytest.mark.parametrize(
    "override",
    [
        {"to": []},
        {"to": ["not-an-email"]},
        {"html": ""},
        {"subject": ""},
        {"subject": "Hello\r\nBcc: evil@x.com"},
        {"subject": "Hello\nWorld"},
        {"cc": ["b@example.com"]},
        {"bcc": ["b@example.com"]},
        {"from": "evil@example.com"},
    ],
)
def test_invalid_payloads_rejected(override):
    with pytest.raises(InvalidPayloadError) as exc:
        EmailPayload.parse({**VALID, **override})
    assert exc.value.__cause__ is not None


@pytest.mark.parametrize("name", ["html"])
def test_missing_required_field_rejected(name):
    data = {k: v for k, v in VALID.items() if k != name}
    with pytest.raises(InvalidPayloadError):
        EmailPayload.parse(data)


def test_multiple_recipients_and_plain_subject_accepted():
    p = EmailPayload.parse({**VALID, "to": ["a@example.com", "b@example.com"], "subject": "Hello World"})
    assert p.to == ["a@example.com", "b@example.com"]


def test_error_does_not_echo_html():
    html = "<p>" + "SECRET-BODY" * 20 + "</p>"
    with pytest.raises(InvalidPayloadError) as exc:
        EmailPayload.parse_json(f'{{"to": [], "subject": "x", "html": "{html}"}}')
    assert "SECRET-BODY" not in str(exc.value)


def test_malformed_json_is_invalid_payload():
    with pytest.raises(InvalidPayloadError):
        EmailPayload.parse_json("{not json")


def test_parse_json_accepts_bytes():
    assert EmailPayload.parse_json(b'{"to":["a@example.com"],"subject":"s","html":"h"}').subject == "s"
