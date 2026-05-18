import uuid
from datetime import datetime
from typing import TYPE_CHECKING
from sqlalchemy import String, Boolean, DateTime, func
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.postgresql import UUID

from models.base import Base, TimestampMixin, UUIDMixin

if TYPE_CHECKING:
    from models.room import Room, RoomMember
    from models.message import Message, MessageReaction
    from models.relationship import Relationship


class User(TimestampMixin, UUIDMixin, Base):
    __tablename__ = "users"

    username: Mapped[str] = mapped_column(String(50), unique=True, nullable=False, index=True)
    email: Mapped[str] = mapped_column(String(255), unique=True, nullable=False, index=True)
    hashed_password: Mapped[str] = mapped_column(String(255), nullable=False)
    display_name: Mapped[str] = mapped_column(String(100), nullable=True)
    avatar_url: Mapped[str] = mapped_column(String(500), nullable=True)
    bio: Mapped[str] = mapped_column(String(500), nullable=True)
    is_online: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    last_seen: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    room_memberships: Mapped[list["RoomMember"]] = relationship(
        "RoomMember", back_populates="user", lazy="selectin"
    )
    messages: Mapped[list["Message"]] = relationship(
        "Message", back_populates="sender", lazy="selectin"
    )
    reactions: Mapped[list["MessageReaction"]] = relationship(
        "MessageReaction", back_populates="user", lazy="selectin"
    )
    sent_requests: Mapped[list["Relationship"]] = relationship(
        "Relationship",
        foreign_keys="Relationship.requester_id",
        back_populates="requester",
        lazy="selectin",
    )
    received_requests: Mapped[list["Relationship"]] = relationship(
        "Relationship",
        foreign_keys="Relationship.addressee_id",
        back_populates="addressee",
        lazy="selectin",
    )
