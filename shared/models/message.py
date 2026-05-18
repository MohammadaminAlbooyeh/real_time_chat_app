from pydantic import BaseModel


class MessagePayload(BaseModel):
    id: str
    room_id: str
    sender_id: str
    sender_username: str
    sender_avatar: str | None = None
    content: str
    message_type: str = "text"
    reply_to_id: str | None = None
    edited_at: str | None = None
    created_at: str
    reactions: list[dict] = []
    attachments: list[dict] = []


class TypingPayload(BaseModel):
    room_id: str
    user_id: str
    username: str
    is_typing: bool


class PresencePayload(BaseModel):
    user_id: str
    username: str
    status: str
