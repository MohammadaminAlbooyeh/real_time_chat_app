import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy import select, desc, func
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from database import get_db
from models.user import User
from models.room import Room, RoomMember
from models.message import Message, MessageReaction, MessageAttachment, MessageType
from schemas.message_schemas import (
    MessageSend,
    MessageEdit,
    MessageReactionAdd,
    MessageResponse,
    ReactionResponse,
    AttachmentResponse,
)
from schemas.pagination import PaginatedResponse
from dependencies import get_current_user
from config import settings

router = APIRouter(prefix="/rooms/{room_id}/messages", tags=["messages"])


async def verify_room_membership(
    room_id: uuid.UUID, user_id: uuid.UUID, db: AsyncSession
):
    result = await db.execute(
        select(RoomMember).where(
            RoomMember.room_id == room_id, RoomMember.user_id == user_id
        )
    )
    if result.scalar_one_or_none() is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not a member of this room",
        )


def message_to_response(msg: Message) -> MessageResponse:
    return MessageResponse(
        id=msg.id,
        room_id=msg.room_id,
        sender_id=msg.sender_id,
        sender_username=msg.sender.username if msg.sender else None,
        sender_avatar=msg.sender.avatar_url if msg.sender else None,
        content="[deleted]" if msg.is_deleted else msg.content,
        message_type=msg.message_type,
        reply_to_id=msg.reply_to_id,
        reply_preview=msg.reply_to.content[:100] if msg.reply_to and not msg.reply_to.is_deleted else None,
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


@router.get("", response_model=PaginatedResponse)
async def get_messages(
    room_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    limit: int = Query(default=50, le=settings.pagination_max_limit),
    offset: int = Query(default=0, ge=0),
    before: datetime | None = None,
):
    await verify_room_membership(room_id, user.id, db)

    query = (
        select(Message)
        .options(
            selectinload(Message.sender),
            selectinload(Message.reactions).selectinload(MessageReaction.user),
            selectinload(Message.attachments),
            selectinload(Message.reply_to),
        )
        .where(Message.room_id == room_id)
        .order_by(desc(Message.created_at))
    )

    if before:
        query = query.where(Message.created_at < before)

    total_result = await db.execute(
        select(func.count(Message.id)).where(Message.room_id == room_id)
    )
    total = total_result.scalar()

    result = await db.execute(query.offset(offset).limit(limit))
    messages = result.scalars().all()

    items = [message_to_response(m) for m in messages]
    return PaginatedResponse(
        items=items,
        total=total,
        limit=limit,
        offset=offset,
        has_more=(offset + limit) < total,
    )


@router.post("", response_model=MessageResponse, status_code=status.HTTP_201_CREATED)
async def send_message(
    room_id: uuid.UUID,
    payload: MessageSend,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await verify_room_membership(room_id, user.id, db)

    if payload.reply_to_id:
        reply_result = await db.execute(
            select(Message).where(Message.id == payload.reply_to_id)
        )
        reply_msg = reply_result.scalar_one_or_none()
        if reply_msg is None or reply_msg.room_id != room_id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid reply message",
            )

    msg = Message(
        room_id=room_id,
        sender_id=user.id,
        content=payload.content,
        message_type=payload.message_type,
        reply_to_id=payload.reply_to_id,
    )
    db.add(msg)
    await db.flush()
    await db.refresh(msg)
    msg.sender = user

    return message_to_response(msg)


@router.put("/{message_id}", response_model=MessageResponse)
async def edit_message(
    room_id: uuid.UUID,
    message_id: uuid.UUID,
    payload: MessageEdit,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Message)
        .options(
            selectinload(Message.sender),
            selectinload(Message.reactions).selectinload(MessageReaction.user),
            selectinload(Message.attachments),
            selectinload(Message.reply_to),
        )
        .where(Message.id == message_id, Message.room_id == room_id)
    )
    msg = result.scalar_one_or_none()
    if msg is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Message not found"
        )
    if msg.sender_id != user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Can only edit your own messages",
        )
    if msg.is_deleted:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot edit a deleted message",
        )

    msg.content = payload.content
    msg.edited_at = datetime.now(timezone.utc)
    db.add(msg)
    await db.flush()
    return message_to_response(msg)


@router.delete("/{message_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_message(
    room_id: uuid.UUID,
    message_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Message).where(Message.id == message_id, Message.room_id == room_id)
    )
    msg = result.scalar_one_or_none()
    if msg is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Message not found"
        )
    if msg.sender_id != user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Can only delete your own messages",
        )
    msg.is_deleted = True
    db.add(msg)


@router.post("/{message_id}/reactions", response_model=ReactionResponse, status_code=status.HTTP_201_CREATED)
async def add_reaction(
    room_id: uuid.UUID,
    message_id: uuid.UUID,
    payload: MessageReactionAdd,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await verify_room_membership(room_id, user.id, db)

    msg_result = await db.execute(
        select(Message).where(Message.id == message_id, Message.room_id == room_id)
    )
    if msg_result.scalar_one_or_none() is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Message not found"
        )

    existing = await db.execute(
        select(MessageReaction).where(
            MessageReaction.message_id == message_id,
            MessageReaction.user_id == user.id,
            MessageReaction.emoji == payload.emoji,
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Reaction already exists",
        )

    reaction = MessageReaction(
        message_id=message_id,
        user_id=user.id,
        emoji=payload.emoji,
    )
    db.add(reaction)
    await db.flush()
    await db.refresh(reaction)
    reaction.user = user

    return ReactionResponse(
        id=reaction.id,
        user_id=user.id,
        username=user.username,
        emoji=reaction.emoji,
        created_at=reaction.created_at,
    )


@router.delete("/{message_id}/reactions/{reaction_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_reaction(
    room_id: uuid.UUID,
    message_id: uuid.UUID,
    reaction_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(MessageReaction).where(
            MessageReaction.id == reaction_id,
            MessageReaction.message_id == message_id,
            MessageReaction.user_id == user.id,
        )
    )
    reaction = result.scalar_one_or_none()
    if reaction is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Reaction not found"
        )
    await db.delete(reaction)
