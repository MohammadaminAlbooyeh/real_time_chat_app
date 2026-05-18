import uuid
from datetime import datetime
from pydantic import BaseModel, Field

from models.message import MessageType


class MessageSend(BaseModel):
    room_id: uuid.UUID
    content: str = Field(..., min_length=1, max_length=5000)
    message_type: MessageType = MessageType.TEXT
    reply_to_id: uuid.UUID | None = None


class MessageEdit(BaseModel):
    content: str = Field(..., min_length=1, max_length=5000)


class MessageReactionAdd(BaseModel):
    emoji: str = Field(..., min_length=1, max_length=50)


class AttachmentResponse(BaseModel):
    id: uuid.UUID
    file_name: str
    file_url: str
    file_type: str
    file_size: int
    created_at: datetime

    model_config = {"from_attributes": True}


class ReactionResponse(BaseModel):
    id: uuid.UUID
    user_id: uuid.UUID
    username: str
    emoji: str
    created_at: datetime

    model_config = {"from_attributes": True}


class MessageResponse(BaseModel):
    id: uuid.UUID
    room_id: uuid.UUID
    sender_id: uuid.UUID | None
    sender_username: str | None
    sender_avatar: str | None
    content: str
    message_type: MessageType
    reply_to_id: uuid.UUID | None
    reply_preview: str | None = None
    is_deleted: bool
    edited_at: datetime | None
    created_at: datetime
    reactions: list[ReactionResponse] = []
    attachments: list[AttachmentResponse] = []

    model_config = {"from_attributes": True}
