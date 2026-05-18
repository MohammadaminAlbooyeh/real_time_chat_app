from pydantic import BaseModel


class PaginationParams(BaseModel):
    limit: int = 50
    offset: int = 0


class PaginatedResponse(BaseModel):
    items: list
    total: int
    limit: int
    offset: int
    has_more: bool
