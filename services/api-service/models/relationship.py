import uuid
from typing import TYPE_CHECKING
from sqlalchemy import ForeignKey, Enum as SAEnum
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.postgresql import UUID
import enum

from models.base import Base, TimestampMixin, UUIDMixin

if TYPE_CHECKING:
    from models.user import User


class RelationshipStatus(str, enum.Enum):
    PENDING = "pending"
    ACCEPTED = "accepted"
    BLOCKED = "blocked"


class Relationship(TimestampMixin, UUIDMixin, Base):
    __tablename__ = "relationships"

    requester_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    addressee_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    status: Mapped[RelationshipStatus] = mapped_column(
        SAEnum(RelationshipStatus), default=RelationshipStatus.PENDING, nullable=False
    )

    requester: Mapped["User"] = relationship(
        "User",
        foreign_keys=[requester_id],
        back_populates="sent_requests",
        lazy="selectin",
    )
    addressee: Mapped["User"] = relationship(
        "User",
        foreign_keys=[addressee_id],
        back_populates="received_requests",
        lazy="selectin",
    )
