from pydantic import BaseModel


class IndexMessageRequest(BaseModel):
    id: str
    content: str
    room_id: str
    sender_id: str
    sender_username: str = ""
    message_type: str = "text"
    created_at: str | None = None


class SearchRequest(BaseModel):
    query: str
    room_id: str | None = None
    limit: int = 50
    offset: int = 0


class SearchResult(BaseModel):
    total: int
    results: list[dict]
