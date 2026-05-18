import uuid
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select, or_, and_, func
from sqlalchemy.ext.asyncio import AsyncSession

from database import get_db
from models.user import User
from models.relationship import Relationship, RelationshipStatus
from dependencies import get_current_user
from config import settings

router = APIRouter(prefix="/relationships", tags=["relationships"])


@router.post("/request/{user_id}", status_code=status.HTTP_201_CREATED)
async def send_friend_request(
    user_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if user_id == user.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot send friend request to yourself",
        )

    target = await db.execute(select(User).where(User.id == user_id))
    if target.scalar_one_or_none() is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found",
        )

    existing = await db.execute(
        select(Relationship).where(
            or_(
                and_(
                    Relationship.requester_id == user.id,
                    Relationship.addressee_id == user_id,
                ),
                and_(
                    Relationship.requester_id == user_id,
                    Relationship.addressee_id == user.id,
                ),
            )
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Relationship already exists",
        )

    rel = Relationship(
        requester_id=user.id,
        addressee_id=user_id,
        status=RelationshipStatus.PENDING,
    )
    db.add(rel)
    return {"message": "Friend request sent"}


@router.post("/respond/{relationship_id}")
async def respond_to_request(
    relationship_id: uuid.UUID,
    action: str,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Relationship).where(Relationship.id == relationship_id)
    )
    rel = result.scalar_one_or_none()
    if rel is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Relationship not found",
        )
    if rel.addressee_id != user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to respond to this request",
        )
    if rel.status != RelationshipStatus.PENDING:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Request already responded to",
        )

    if action == "accept":
        rel.status = RelationshipStatus.ACCEPTED
    elif action == "reject":
        await db.delete(rel)
        return {"message": "Friend request rejected"}
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Action must be 'accept' or 'reject'",
        )

    db.add(rel)
    return {"message": f"Friend request {action}ed"}


@router.get("/friends")
async def list_friends(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Relationship).where(
            or_(
                Relationship.requester_id == user.id,
                Relationship.addressee_id == user.id,
            )
        ).where(Relationship.status == RelationshipStatus.ACCEPTED)
    )
    rels = result.scalars().all()

    friends = []
    for rel in rels:
        friend_id = (
            rel.addressee_id if rel.requester_id == user.id else rel.requester_id
        )
        friend_result = await db.execute(
            select(User).where(User.id == friend_id)
        )
        friend = friend_result.scalar_one_or_none()
        if friend:
            friends.append(
                {
                    "id": friend.id,
                    "username": friend.username,
                    "display_name": friend.display_name,
                    "avatar_url": friend.avatar_url,
                    "is_online": friend.is_online,
                }
            )
    return friends


@router.get("/pending")
async def list_pending_requests(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Relationship)
        .where(Relationship.addressee_id == user.id)
        .where(Relationship.status == RelationshipStatus.PENDING)
    )
    rels = result.scalars().all()

    requests = []
    for rel in rels:
        requester_result = await db.execute(
            select(User).where(User.id == rel.requester_id)
        )
        requester = requester_result.scalar_one_or_none()
        if requester:
            requests.append(
                {
                    "id": rel.id,
                    "user": {
                        "id": requester.id,
                        "username": requester.username,
                        "display_name": requester.display_name,
                        "avatar_url": requester.avatar_url,
                    },
                    "created_at": rel.created_at,
                }
            )
    return requests


@router.delete("/{relationship_id}")
async def remove_friend(
    relationship_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Relationship).where(Relationship.id == relationship_id)
    )
    rel = result.scalar_one_or_none()
    if rel is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Relationship not found",
        )
    if rel.requester_id != user.id and rel.addressee_id != user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized",
        )
    await db.delete(rel)
    return {"message": "Friend removed"}
