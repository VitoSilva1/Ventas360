import json
from unittest.mock import MagicMock, patch

import pytest

from mail_carrier.amqp_consumer import process_message

VALID_BODY = json.dumps({
    "sale_id": "550e8400-e29b-41d4-a716-446655440000",
    "customer_id": 42,
    "customer_email": "customer@example.com",
    "sale_date": "2025-10-15T14:30:00",
    "items": [
        {
            "product_id": 1,
            "product_name": "Product A",
            "quantity": 2,
            "unit_price": "19.99"
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
