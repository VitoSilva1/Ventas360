import pytest

from mail_carrier.config import ConfigError, SmtpConfig, load_config

BASE = {"SMTP_USER": "ventas@gmail.com", "SMTP_APP_PASSWORD": "s3cret-pass"}


def test_defaults_applied():
    cfg = load_config(BASE)
    assert cfg == SmtpConfig(
        host="smtp.gmail.com",
        port=587,
        user="ventas@gmail.com",
        app_password="s3cret-pass",
        from_name=None,
        timeout=cfg.timeout,
    )
    assert cfg.timeout > 0


def test_overrides_read_from_injected_env():
    cfg = load_config({**BASE, "SMTP_HOST": "mail.local", "SMTP_PORT": "2525", "SMTP_FROM_NAME": "Ventas360"})
    assert (cfg.host, cfg.port, cfg.from_name) == ("mail.local", 2525, "Ventas360")


@pytest.mark.parametrize("name", ["SMTP_USER", "SMTP_APP_PASSWORD"])
@pytest.mark.parametrize("blank", [None, "", "   "])
def test_missing_or_blank_required_names_only_the_variable(name, blank):
    env = {**BASE}
    if blank is None:
        del env[name]
    else:
        env[name] = blank
    with pytest.raises(ConfigError) as exc:
        load_config(env)
    assert name in str(exc.value)
    assert "s3cret-pass" not in str(exc.value)


def test_non_integer_port_rejected():
    with pytest.raises(ConfigError, match="SMTP_PORT"):
        load_config({**BASE, "SMTP_PORT": "abc"})


def test_password_absent_from_repr():
    assert "s3cret-pass" not in repr(load_config(BASE))
