import uuid
from sqlalchemy import String, Text, Boolean, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.dialects.postgresql import UUID, JSONB

from models.base import Base, TimestampMixin, UUIDMixin


class UserNotification(TimestampMixin, UUIDMixin, Base):
    __tablename__ = "user_notifications"

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    body: Mapped[str] = mapped_column(Text, nullable=False)
    data: Mapped[dict] = mapped_column(JSONB, default=dict)
    notification_type: Mapped[str] = mapped_column(String(20), default="push", nullable=False)
    is_read: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
