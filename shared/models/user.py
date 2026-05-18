from pydantic import BaseModel


class UserPayload(BaseModel):
    id: str
    username: str
    display_name: str | None = None
    avatar_url: str | None = None
    is_online: bool = False
