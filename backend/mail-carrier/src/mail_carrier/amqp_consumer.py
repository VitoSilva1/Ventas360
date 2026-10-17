import logging
import os
import time
from decimal import Decimal, ROUND_HALF_UP
from html import escape

import pika

from mail_carrier.config import load_config
from mail_carrier.controller import handle
from mail_carrier.models import EmailPayload, SaleEvent
from mail_carrier.service import send_email

log = logging.getLogger(__name__)

QUEUE = os.getenv("RABBITMQ_QUEUE", "ventas360.mail.queue")


def _format_clp(amount: Decimal) -> str:
    rounded = amount.quantize(Decimal("1"), rounding=ROUND_HALF_UP)
    return f"${rounded:,.0f}".replace(",", ".")


def _build_email_payload(event: SaleEvent) -> EmailPayload:
    rows = "".join(
        f"""
        <tr>
          <td style="padding:14px 12px;border-bottom:1px solid #edf0f6;color:#17213a">
            {escape(item.productName)}
          </td>
          <td align="center" style="padding:14px 12px;border-bottom:1px solid #edf0f6;color:#546078">
            {item.quantity}
          </td>
          <td align="right" style="padding:14px 12px;border-bottom:1px solid #edf0f6;color:#546078;white-space:nowrap">
            {_format_clp(item.unitPrice)}
          </td>
          <td align="right" style="padding:14px 12px;border-bottom:1px solid #edf0f6;color:#17213a;font-weight:700;white-space:nowrap">
            {_format_clp(item.unitPrice * item.quantity)}
          </td>
        </tr>
        """
        for item in event.items
    )
    total = sum(
        (item.unitPrice * item.quantity for item in event.items),
        start=Decimal("0"),
    )
    sale_date = event.saleDate.strftime("%d/%m/%Y %H:%M")
    text_items = "\n".join(
        f"- {item.productName} | {item.quantity} x {_format_clp(item.unitPrice)}"
        f" = {_format_clp(item.unitPrice * item.quantity)}"
        for item in event.items
    )
    text = (
        f"Hola, tu compra fue registrada correctamente.\n\n"
        f"Venta #{event.saleId}\n"
        f"Fecha: {sale_date}\n\n"
        f"Detalle de tu compra:\n{text_items}\n\n"
        f"Total: {_format_clp(total)}\n\n"
        "Gracias por preferir Ventas 360."
    )
    html = (
        f"""<!doctype html>
<html lang="es">
  <body style="margin:0;padding:0;background-color:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#17213a">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0"
      style="background-color:#f5f7fb;padding:32px 12px">
      <tr>
        <td align="center">
          <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0"
            style="width:100%;max-width:600px;background-color:#ffffff;border:1px solid #e7ebf3;border-radius:16px">
            <tr>
              <td style="padding:32px 36px 24px;border-bottom:1px solid #f0f3f8">
                <p style="margin:0 0 8px;color:#315cf5;font-size:13px;font-weight:bold;letter-spacing:1px">
                  VENTAS 360
                </p>
                <h1 style="margin:0 0 12px;font-size:25px;line-height:1.3;color:#17213a">
                  ¡Gracias por tu compra!
                </h1>
                <p style="margin:0;color:#546078;font-size:15px;line-height:1.6">
                  Tu venta <strong>#{event.saleId}</strong> fue registrada correctamente.
                </p>
              </td>
            </tr>
            <tr>
              <td style="padding:24px 36px 8px">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                  <tr>
                    <td style="color:#8994aa;font-size:12px;text-transform:uppercase;letter-spacing:.06em">
                      Fecha de compra
                    </td>
                    <td align="right" style="color:#17213a;font-size:13px;font-weight:bold">
                      {sale_date}
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr>
              <td style="padding:16px 24px 8px">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0"
                  style="border:1px solid #edf0f6;border-radius:10px;border-collapse:separate">
                  <thead>
                    <tr style="background-color:#f8fafd">
                      <th align="left" style="padding:12px;color:#8994aa;font-size:10px;text-transform:uppercase">Producto</th>
                      <th align="center" style="padding:12px;color:#8994aa;font-size:10px;text-transform:uppercase">Cant.</th>
                      <th align="right" style="padding:12px;color:#8994aa;font-size:10px;text-transform:uppercase">Precio</th>
                      <th align="right" style="padding:12px;color:#8994aa;font-size:10px;text-transform:uppercase">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>{rows}</tbody>
                </table>
              </td>
            </tr>
            <tr>
              <td style="padding:16px 36px 28px">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0"
                  style="border-top:2px solid #edf0f6">
                  <tr>
                    <td style="padding-top:18px;color:#546078;font-size:14px;font-weight:bold">Total de la compra</td>
                    <td align="right" style="padding-top:18px;color:#17213a;font-size:22px;font-weight:bold;white-space:nowrap">
                      {_format_clp(total)}
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr>
              <td align="center" style="padding:18px 24px;background-color:#f8fafd;border-radius:0 0 16px 16px">
                <p style="margin:0;color:#8994aa;font-size:12px;line-height:1.6">
                  Este correo confirma el registro de tu compra.<br>
                  Gracias por preferir Ventas 360.
                </p>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>"""
    )
    return EmailPayload(
        to=[event.customerEmail],
        subject=f"Confirmación de compra #{event.saleId}",
        html=html,
        text=text,
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
