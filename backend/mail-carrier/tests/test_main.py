import io
import json
import logging
from pathlib import Path

import pytest

from mail_carrier import __main__ as cli
from mail_carrier.config import ConfigError
from mail_carrier.service import MailSendError

PASSWORD = "s3cret-pass"
HTML = "<p>TOP-SECRET-BODY</p>"
ENV = {"SMTP_USER": "ventas@gmail.com", "SMTP_APP_PASSWORD": PASSWORD}
PAYLOAD = json.dumps({"to": ["a@example.com", "b@example.com"], "subject": "Hi", "html": HTML})


@pytest.fixture
def sent(monkeypatch):
    calls = []
    monkeypatch.setattr(cli, "send_email", lambda p, cfg: calls.append((p, cfg)))
    return calls


@pytest.fixture
def payload_file(tmp_path):
    f = tmp_path / "payload.json"
    f.write_text(PAYLOAD, encoding="utf-8")
    return str(f)


def run(argv, stdin="", env=ENV):
    return cli.main(argv, stdin=io.StringIO(stdin), env=env)


def test_success_from_file(sent, payload_file):
    assert run([payload_file]) == 0
    assert len(sent) == 1 and sent[0][0].to == ["a@example.com", "b@example.com"]
    assert sent[0][1].user == "ventas@gmail.com"


def test_success_from_stdin(sent):
    assert run([], stdin=PAYLOAD) == 0
    assert len(sent) == 1


def test_send_failure_exits_1(monkeypatch, payload_file, capsys):
    def boom(p, cfg):
        raise MailSendError("SMTP send failed: SMTPAuthenticationError")

    monkeypatch.setattr(cli, "send_email", boom)
    assert run([payload_file]) == 1
    assert "SMTPAuthenticationError" in capsys.readouterr().err


def test_config_error_exits_1(sent, payload_file, capsys):
    assert run([payload_file], env={}) == 1
    assert "SMTP_USER" in capsys.readouterr().err
    assert sent == []


def test_invalid_payload_exits_2_without_smtp(sent, capsys):
    bad = json.dumps({"to": [], "subject": "Hi", "html": HTML})
    assert run([], stdin=bad) == 2
    err = capsys.readouterr().err
    assert "to" in err and HTML not in err and PASSWORD not in err
    assert sent == []


def test_malformed_json_exits_2(sent):
    assert run([], stdin="{nope") == 2
    assert sent == []


def test_nonexistent_file_exits_2(sent, tmp_path):
    assert run([str(tmp_path / "missing.json")]) == 2
    assert sent == []


def test_unreadable_path_exits_2(sent, tmp_path):
    # A directory cannot be read as a file on any platform.
    assert run([str(tmp_path)]) == 2
    assert sent == []


def test_logs_and_stderr_hide_secrets(monkeypatch, payload_file, capsys, caplog):
    caplog.set_level(logging.DEBUG)
    run([payload_file], stdin="")
    run([], stdin=json.dumps({"to": [], "subject": "s", "html": HTML}))
    monkeypatch.setattr(cli, "send_email", lambda p, cfg: (_ for _ in ()).throw(MailSendError("x")))
    run([payload_file])
    out = capsys.readouterr()
    assert PASSWORD not in out.err + caplog.text
    assert HTML not in out.err + caplog.text


def test_success_logs_recipient_count_only(sent, payload_file, caplog):
    caplog.set_level(logging.INFO)
    run([payload_file])
    assert "2 recipient" in caplog.text
    assert "a@example.com" not in caplog.text


def test_sample_payload_exits_0(sent):
    sample = Path(__file__).resolve().parent.parent / "samples" / "payload.json"
    assert run([str(sample)]) == 0
    p = sent[0][0]
    assert p.text and p.html
