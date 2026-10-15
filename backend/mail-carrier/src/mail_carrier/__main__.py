import logging
import os
import sys
from pathlib import Path
from collections.abc import Mapping, Sequence
from typing import TextIO

from mail_carrier.config import ConfigError, load_config
from mail_carrier.controller import handle
from mail_carrier.models import InvalidPayloadError
from mail_carrier.service import MailSendError, send_email

log = logging.getLogger("mail_carrier")

EXIT_OK, EXIT_SEND_OR_CONFIG, EXIT_INVALID_INPUT = 0, 1, 2


def _send(p, cfg) -> None:
    send_email(p, cfg)
    log.info("Email sent to %d recipient(s)", len(p.to))


def main(argv: Sequence[str], *, stdin: TextIO, env: Mapping[str, str]) -> int:
    """Only place where exceptions become exit codes. Never logs payload, html or config."""
    try:
        cfg = load_config(env)
        raw = Path(argv[0]).read_bytes() if argv else stdin.read()
        handle(raw, lambda p: _send(p, cfg))
    except (ConfigError, MailSendError) as e:
        print(f"Error: {e}", file=sys.stderr)
        return EXIT_SEND_OR_CONFIG
    except InvalidPayloadError as e:
        print(f"Error: {e}", file=sys.stderr)
        return EXIT_INVALID_INPUT
    except OSError as e:
        # Only reading the payload file raises a bare OSError (send errors are wrapped).
        print(f"Error: cannot read payload file: {e.strerror}", file=sys.stderr)
        return EXIT_INVALID_INPUT
    return EXIT_OK


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, stream=sys.stderr)
    sys.exit(main(sys.argv[1:], stdin=sys.stdin, env=os.environ))
