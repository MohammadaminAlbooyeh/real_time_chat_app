from pydantic import BaseModel
from typing import Any


class ErrorResponse(BaseModel):
    error: str
    message: str
    details: dict[str, Any] | None = None


class PaginationRequest(BaseModel):
    limit: int = 50
    offset: int = 0
    before: str | None = None
