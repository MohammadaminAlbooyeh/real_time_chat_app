import uuid
from datetime import datetime
from pydantic import BaseModel, Field

from models.room import MemberRole


class RoomCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    description: str | None = Field(None, max_length=500)
    is_private: bool = False


class RoomUpdate(BaseModel):
    name: str | None = Field(None, min_length=1, max_length=100)
    description: str | None = Field(None, max_length=500)
    is_private: bool | None = None


class RoomMemberResponse(BaseModel):
    id: uuid.UUID
    user_id: uuid.UUID
    username: str
    display_name: str | None
    avatar_url: str | None
    role: MemberRole
    joined_at: datetime

    model_config = {"from_attributes": True}


class RoomResponse(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None
    is_private: bool
    created_by_id: uuid.UUID
    created_at: datetime
    updated_at: datetime
    member_count: int = 0
    last_message: str | None = None
    last_message_at: datetime | None = None

    model_config = {"from_attributes": True}


class RoomDetailResponse(RoomResponse):
    members: list[RoomMemberResponse] = []


class AddMemberRequest(BaseModel):
    user_id: uuid.UUID


class UpdateMemberRoleRequest(BaseModel):
    role: MemberRole
