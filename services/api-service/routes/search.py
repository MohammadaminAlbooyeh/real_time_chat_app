import uuid
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select, or_
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from database import get_db
from models.user import User
from models.message import Message, MessageReaction, MessageAttachment
from schemas.message_schemas import MessageResponse, ReactionResponse, AttachmentResponse
from dependencies import get_current_user
from config import settings

router = APIRouter(prefix="/search", tags=["search"])


@router.get("/messages", response_model=list[MessageResponse])
async def search_messages(
    q: str = Query(..., min_length=1, max_length=200),
    room_id: uuid.UUID | None = None,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    limit: int = Query(default=50, le=settings.pagination_max_limit),
):
    from models.room import RoomMember

    query = (
        select(Message)
        .options(
            selectinload(Message.sender),
            selectinload(Message.reactions).selectinload(MessageReaction.user),
            selectinload(Message.attachments),
            selectinload(Message.reply_to),
        )
        .where(Message.is_deleted == False)
        .where(Message.content.ilike(f"%{q}%"))
    )

    if room_id:
        query = query.where(Message.room_id == room_id)
    else:
        member_rooms = (
            select(RoomMember.room_id).where(RoomMember.user_id == user.id)
        ).subquery()
        query = query.where(Message.room_id.in_(member_rooms))

    query = query.order_by(Message.created_at.desc()).limit(limit)
    result = await db.execute(query)
    messages = result.scalars().all()

    response = []
    for msg in messages:
        response.append(
            MessageResponse(
                id=msg.id,
                room_id=msg.room_id,
                sender_id=msg.sender_id,
                sender_username=msg.sender.username if msg.sender else None,
                sender_avatar=msg.sender.avatar_url if msg.sender else None,
                content=msg.content,
                message_type=msg.message_type,
                reply_to_id=msg.reply_to_id,
                reply_preview=msg.reply_to.content[:100]
                if msg.reply_to and not msg.reply_to.is_deleted
                else None,
                is_deleted=msg.is_deleted,
                edited_at=msg.edited_at,
                created_at=msg.created_at,
                reactions=[
                    ReactionResponse(
                        id=r.id,
                        user_id=r.user_id,
                        username=r.user.username if r.user else "unknown",
                        emoji=r.emoji,
                        created_at=r.created_at,
                    )
                    for r in msg.reactions
                ],
                attachments=[
                    AttachmentResponse(
                        id=a.id,
                        file_name=a.file_name,
                        file_url=a.file_url,
                        file_type=a.file_type,
                        file_size=a.file_size,
                        created_at=a.created_at,
                    )
                    for a in msg.attachments
                ],
            )
        )
    return response
