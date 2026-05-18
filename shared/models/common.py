from pydantic import BaseModel
from typing import Any


class WSMessage(BaseModel):
    type: str
    payload: dict[str, Any]
    room_id: str | None = None


class ErrorPayload(BaseModel):
    code: str
    message: str
    details: dict[str, Any] | None = None
