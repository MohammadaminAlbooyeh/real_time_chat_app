from pydantic import BaseModel


class RoomPayload(BaseModel):
    id: str
    name: str
    description: str | None = None
    is_private: bool = False
    created_by_id: str
    member_count: int = 0
