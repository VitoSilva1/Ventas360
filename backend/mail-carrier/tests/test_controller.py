import pytest

from mail_carrier.controller import handle
from mail_carrier.models import InvalidPayloadError
from mail_carrier.service import MailSendError

RAW = '{"to": ["a@example.com"], "subject": "Hi", "html": "<p>Hi</p>"}'


@pytest.mark.parametrize("raw", [RAW, RAW.encode()])
def test_valid_input_calls_send_once_with_payload(raw):
    sent = []
    handle(raw, sent.append)
    assert [p.to for p in sent] == [["a@example.com"]]


@pytest.mark.parametrize("raw", ["{broken", '{"to": [], "subject": "Hi", "html": "x"}'])
def test_invalid_input_raises_and_send_not_called(raw):
    sent = []
    with pytest.raises(InvalidPayloadError):
        handle(raw, sent.append)
    assert sent == []


def test_send_error_propagates_without_system_exit():
    def boom(_):
        raise MailSendError("x")

    with pytest.raises(MailSendError):
        handle(RAW, boom)
