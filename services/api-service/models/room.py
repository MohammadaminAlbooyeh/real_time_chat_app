import uuid
from typing import TYPE_CHECKING
from sqlalchemy import String, Boolean, ForeignKey, Enum as SAEnum
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.postgresql import UUID
import enum

from models.base import Base, TimestampMixin, UUIDMixin

if TYPE_CHECKING:
    from models.user import User
    from models.message import Message


class MemberRole(str, enum.Enum):
    ADMIN = "admin"
    MEMBER = "member"


class Room(TimestampMixin, UUIDMixin, Base):
    __tablename__ = "rooms"

    name: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    description: Mapped[str] = mapped_column(String(500), nullable=True)
    is_private: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_by_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False
    )

    creator: Mapped["User"] = relationship("User", lazy="selectin")
    members: Mapped[list["RoomMember"]] = relationship(
        "RoomMember", back_populates="room", lazy="selectin", cascade="all, delete-orphan"
    )
    messages: Mapped[list["Message"]] = relationship(
        "Message", back_populates="room", lazy="selectin", cascade="all, delete-orphan"
    )


class RoomMember(TimestampMixin, UUIDMixin, Base):
    __tablename__ = "room_members"

    room_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("rooms.id", ondelete="CASCADE"), nullable=False
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    role: Mapped[MemberRole] = mapped_column(
        SAEnum(MemberRole), default=MemberRole.MEMBER, nullable=False
    )

    room: Mapped["Room"] = relationship("Room", back_populates="members", lazy="selectin")
    user: Mapped["User"] = relationship("User", back_populates="room_memberships", lazy="selectin")
