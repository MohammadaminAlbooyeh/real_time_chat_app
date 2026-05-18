import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy import select, func, desc
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from database import get_db
from models.user import User
from models.room import Room, RoomMember, MemberRole
from models.message import Message
from schemas.room_schemas import (
    RoomCreate,
    RoomUpdate,
    RoomResponse,
    RoomDetailResponse,
    RoomMemberResponse,
    AddMemberRequest,
    UpdateMemberRoleRequest,
)
from dependencies import get_current_user
from config import settings

router = APIRouter(prefix="/rooms", tags=["rooms"])


async def get_room_or_404(room_id: uuid.UUID, db: AsyncSession) -> Room:
    result = await db.execute(
        select(Room)
        .options(selectinload(Room.members).selectinload(RoomMember.user))
        .where(Room.id == room_id)
    )
    room = result.scalar_one_or_none()
    if room is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Room not found"
        )
    return room


async def is_member(room: Room, user_id: uuid.UUID) -> bool:
    return any(m.user_id == user_id for m in room.members)


async def get_member(room: Room, user_id: uuid.UUID) -> RoomMember | None:
    for m in room.members:
        if m.user_id == user_id:
            return m
    return None


@router.post("", response_model=RoomResponse, status_code=status.HTTP_201_CREATED)
async def create_room(
    payload: RoomCreate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    count_result = await db.execute(
        select(func.count(RoomMember.id)).where(RoomMember.user_id == user.id)
    )
    room_count = count_result.scalar()
    if room_count >= settings.max_rooms_per_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Maximum rooms limit reached",
        )

    room = Room(
        name=payload.name,
        description=payload.description,
        is_private=payload.is_private,
        created_by_id=user.id,
    )
    db.add(room)
    await db.flush()
    await db.refresh(room)

    member = RoomMember(
        room_id=room.id,
        user_id=user.id,
        role=MemberRole.ADMIN,
    )
    db.add(member)
    await db.flush()

    return RoomResponse(
        id=room.id,
        name=room.name,
        description=room.description,
        is_private=room.is_private,
        created_by_id=room.created_by_id,
        created_at=room.created_at,
        updated_at=room.updated_at,
        member_count=1,
    )


@router.get("", response_model=list[RoomResponse])
async def list_rooms(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    limit: int = Query(default=50, le=settings.pagination_max_limit),
    offset: int = Query(default=0, ge=0),
):
    subquery = (
        select(RoomMember.room_id)
        .where(RoomMember.user_id == user.id)
        .subquery()
    )
    result = await db.execute(
        select(Room)
        .where(Room.id.in_(subquery))
        .offset(offset)
        .limit(limit)
        .order_by(Room.updated_at.desc())
    )
    rooms = result.scalars().all()

    response = []
    for room in rooms:
        member_count = len([m for m in room.members])
        last_msg_result = await db.execute(
            select(Message.content, Message.created_at)
            .where(Message.room_id == room.id)
            .where(Message.is_deleted == False)
            .order_by(desc(Message.created_at))
            .limit(1)
        )
        last_msg = last_msg_result.first()
        response.append(
            RoomResponse(
                id=room.id,
                name=room.name,
                description=room.description,
                is_private=room.is_private,
                created_by_id=room.created_by_id,
                created_at=room.created_at,
                updated_at=room.updated_at,
                member_count=member_count,
                last_message=last_msg[0] if last_msg else None,
                last_message_at=last_msg[1] if last_msg else None,
            )
        )
    return response


@router.get("/{room_id}", response_model=RoomDetailResponse)
async def get_room(
    room_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    room = await get_room_or_404(room_id, db)
    if not await is_member(room, user.id):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not a member of this room",
        )
    members = [
        RoomMemberResponse(
            id=m.id,
            user_id=m.user_id,
            username=m.user.username,
            display_name=m.user.display_name,
            avatar_url=m.user.avatar_url,
            role=m.role,
            joined_at=m.created_at,
        )
        for m in room.members
    ]
    return RoomDetailResponse(
        id=room.id,
        name=room.name,
        description=room.description,
        is_private=room.is_private,
        created_by_id=room.created_by_id,
        created_at=room.created_at,
        updated_at=room.updated_at,
        member_count=len(members),
        members=members,
    )


@router.put("/{room_id}", response_model=RoomResponse)
async def update_room(
    room_id: uuid.UUID,
    payload: RoomUpdate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    room = await get_room_or_404(room_id, db)
    member = await get_member(room, user.id)
    if member is None or member.role != MemberRole.ADMIN:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only room admins can update the room",
        )
    update_data = payload.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(room, field, value)
    room.updated_at = datetime.now(timezone.utc)
    db.add(room)
    await db.flush()
    await db.refresh(room)
    return RoomResponse(
        id=room.id,
        name=room.name,
        description=room.description,
        is_private=room.is_private,
        created_by_id=room.created_by_id,
        created_at=room.created_at,
        updated_at=room.updated_at,
        member_count=len(room.members),
    )


@router.delete("/{room_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_room(
    room_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    room = await get_room_or_404(room_id, db)
    if room.created_by_id != user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the room creator can delete the room",
        )
    await db.delete(room)


@router.post("/{room_id}/members", status_code=status.HTTP_201_CREATED)
async def add_member(
    room_id: uuid.UUID,
    payload: AddMemberRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    room = await get_room_or_404(room_id, db)
    member = await get_member(room, user.id)
    if member is None or member.role != MemberRole.ADMIN:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only admins can add members",
        )
    if await is_member(room, payload.user_id):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="User is already a member",
        )
    new_member = RoomMember(room_id=room.id, user_id=payload.user_id)
    db.add(new_member)
    return {"message": "Member added successfully"}


@router.delete("/{room_id}/members/{user_id}")
async def remove_member(
    room_id: uuid.UUID,
    user_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    room = await get_room_or_404(room_id, db)
    member = await get_member(room, user.id)
    if member is None or member.role != MemberRole.ADMIN:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only admins can remove members",
        )
    target = await get_member(room, user_id)
    if target is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User is not a member of this room",
        )
    await db.delete(target)
    return {"message": "Member removed successfully"}


@router.post("/{room_id}/leave")
async def leave_room(
    room_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    room = await get_room_or_404(room_id, db)
    member = await get_member(room, user.id)
    if member is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="You are not a member of this room",
        )
    await db.delete(member)
    return {"message": "Left room successfully"}
