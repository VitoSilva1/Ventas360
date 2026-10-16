import logging
import os
import time

import pika

from mail_carrier.config import load_config
from mail_carrier.controller import handle
from mail_carrier.models import EmailPayload, SaleEvent
from mail_carrier.service import send_email

log = logging.getLogger(__name__)

QUEUE = os.getenv("RABBITMQ_QUEUE", "ventas360.mail.queue")


def _build_email_payload(event: SaleEvent) -> EmailPayload:
    item_lines = "".join(
        f"<li>{item.productName} x{item.quantity} @ {item.unitPrice}</li>"
        for item in event.items
    )
    html = (
        f"<h1>Sale Confirmation</h1>"
        f"<p>Dear customer {event.customerId},</p>"
        f"<p>Your sale <strong>{event.saleId}</strong> has been registered.</p>"
        f"<ul>{item_lines}</ul>"
    )
    return EmailPayload(
        to=[event.customerEmail],
        subject=f"Sale Confirmation #{event.saleId}",
        html=html,
    )


def process_message(ch, method, properties, body):
    try:
        event = SaleEvent.model_validate_json(body)
        payload = _build_email_payload(event)
        handle(payload.model_dump_json(), lambda p: None)
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as e:
        log.error("Failed to process message: %s | body: %s", e, body)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def _make_send(cfg):
    def _send(p):
        send_email(p, cfg)

    return _send


def run():
    cfg = load_config(os.environ)

    def _on_message(ch, method, properties, body):
        try:
            event = SaleEvent.model_validate_json(body)
            payload = _build_email_payload(event)
            handle(payload.model_dump_json(), _make_send(cfg))
            ch.basic_ack(delivery_tag=method.delivery_tag)
        except Exception as e:
            log.error("Failed to process message: %s | body: %s", e, body)
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    while True:
        try:
            conn = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=os.getenv("RABBITMQ_HOST", "localhost"),
                    port=int(os.getenv("RABBITMQ_PORT", 5672)),
                    credentials=pika.PlainCredentials(
                        os.getenv("RABBITMQ_USERNAME", "guest"),
                        os.getenv("RABBITMQ_PASSWORD", "guest"),
                    ),
                )
            )
            ch = conn.channel()
            ch.queue_declare(queue=QUEUE, passive=True)
            ch.basic_consume(queue=QUEUE, on_message_callback=_on_message)
            log.info("Consuming from queue: %s", QUEUE)
            ch.start_consuming()
        except Exception as e:
            log.error("Connection lost: %s — retrying in 5s", e)
            time.sleep(5)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    run()
