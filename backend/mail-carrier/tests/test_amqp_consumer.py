import json
from unittest.mock import MagicMock, patch

import pytest

from mail_carrier.amqp_consumer import _build_email_payload, process_message
from mail_carrier.models import SaleEvent

VALID_BODY = json.dumps({
    "saleId": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": 42,
    "customerEmail": "customer@example.com",
    "saleDate": "2025-10-15T14:30:00",
    "items": [
        {
            "productId": 1,
            "productName": "Product A",
            "quantity": 2,
            "unitPrice": "19.99"
        }
    ]
}).encode()


def _make_channel():
    ch = MagicMock()
    ch.basic_ack = MagicMock()
    ch.basic_nack = MagicMock()
    return ch


def _make_method():
    method = MagicMock()
    method.delivery_tag = 1
    return method


def test_build_email_payload_creates_branded_html_and_plain_text():
    event = SaleEvent.model_validate({
        "saleId": "550e8400-e29b-41d4-a716-446655440000",
        "customerEmail": "customer@example.com",
        "saleDate": "2025-10-15T14:30:00",
        "items": [
            {
                "productId": 1,
                "productName": "Café <Premium>",
                "quantity": 2,
                "unitPrice": "1234.50",
            }
        ],
    })

    payload = _build_email_payload(event)

    assert payload.subject == "Confirmación de compra #550e8400-e29b-41d4-a716-446655440000"
    assert payload.to == ["customer@example.com"]
    assert "<!doctype html>" in payload.html
    assert "Café &lt;Premium&gt;" in payload.html
    assert "Café <Premium>" not in payload.html
    assert "$2.469" in payload.html
    assert "Total de la compra" in payload.html
    assert payload.text is not None
    assert "Venta #550e8400-e29b-41d4-a716-446655440000" in payload.text
    assert "Total: $2.469" in payload.text


def test_process_message_calls_handle_and_acks():
    ch = _make_channel()
    method = _make_method()
    props = MagicMock()

    with patch("mail_carrier.amqp_consumer.handle") as mock_handle:
        process_message(ch, method, props, VALID_BODY)

    mock_handle.assert_called_once()
    ch.basic_ack.assert_called_once_with(delivery_tag=method.delivery_tag)


def test_process_message_malformed_nacks_and_logs_error():
    ch = _make_channel()
    method = _make_method()
    props = MagicMock()

    malformed_body = b"{not valid json at all}"

    with patch("mail_carrier.amqp_consumer.handle") as mock_handle:
        process_message(ch, method, props, malformed_body)

    mock_handle.assert_not_called()
    ch.basic_nack.assert_called_once_with(delivery_tag=method.delivery_tag, requeue=False)
    ch.basic_ack.assert_not_called()
