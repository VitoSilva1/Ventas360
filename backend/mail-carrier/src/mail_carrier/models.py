from typing import Any, Self

from pydantic import BaseModel, ConfigDict, EmailStr, Field, ValidationError, field_validator


class InvalidPayloadError(Exception):
    """The payload violates the contract. Chained from pydantic's ValidationError."""


class EmailPayload(BaseModel):
    model_config = ConfigDict(extra="forbid")

    to: list[EmailStr] = Field(min_length=1)
    subject: str = Field(min_length=1)
    html: str = Field(min_length=1)
    text: str | None = None

    @field_validator("subject")
    @classmethod
    def _no_header_injection(cls, value: str) -> str:
        if "\r" in value or "\n" in value:
            raise ValueError("subject must not contain line breaks")
        return value

    @classmethod
    def parse(cls, data: Any) -> Self:
        try:
            return cls.model_validate(data)
        except ValidationError as e:
            raise _invalid(e) from e

    @classmethod
    def parse_json(cls, raw: str | bytes) -> Self:
        try:
            return cls.model_validate_json(raw)
        except ValidationError as e:
            raise _invalid(e) from e


def _invalid(e: ValidationError) -> InvalidPayloadError:
    # Built from locations and messages only: pydantic's str() echoes input values (e.g. the html body).
    details = "; ".join(f"{'.'.join(map(str, err['loc'])) or 'payload'}: {err['msg']}" for err in e.errors())
    return InvalidPayloadError(f"Invalid payload: {details}")
