import uuid
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy import select, func, update
from sqlalchemy.ext.asyncio import AsyncSession

from database import get_db
from models.user import User
from models.notification import UserNotification
from schemas.notification_schemas import NotificationResponse, NotificationListResponse
from dependencies import get_current_user
from config import settings

router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.get("", response_model=NotificationListResponse)
async def list_notifications(
    limit: int = Query(default=50, le=settings.pagination_max_limit),
    offset: int = Query(default=0, ge=0),
    unread_only: bool = Query(default=False),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    query = select(UserNotification).where(
        UserNotification.user_id == user.id
    )
    if unread_only:
        query = query.where(UserNotification.is_read == False)

    total_result = await db.execute(
        select(func.count(UserNotification.id)).where(
            UserNotification.user_id == user.id
        )
    )
    total = total_result.scalar()

    unread_result = await db.execute(
        select(func.count(UserNotification.id)).where(
            UserNotification.user_id == user.id,
            UserNotification.is_read == False,
        )
    )
    unread_count = unread_result.scalar()

    query = query.order_by(UserNotification.created_at.desc()).offset(offset).limit(limit)
    result = await db.execute(query)
    notifications = result.scalars().all()

    return NotificationListResponse(
        items=[NotificationResponse.model_validate(n) for n in notifications],
        total=total,
        unread_count=unread_count,
    )


@router.get("/unread-count")
async def unread_notification_count(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(func.count(UserNotification.id)).where(
            UserNotification.user_id == user.id,
            UserNotification.is_read == False,
        )
    )
    return {"unread_count": result.scalar()}


@router.patch("/{notification_id}/read", response_model=NotificationResponse)
async def mark_as_read(
    notification_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(UserNotification).where(
            UserNotification.id == notification_id,
            UserNotification.user_id == user.id,
        )
    )
    notification = result.scalar_one_or_none()
    if notification is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Notification not found",
        )

    notification.is_read = True
    db.add(notification)
    await db.flush()
    return NotificationResponse.model_validate(notification)


@router.post("/read-all")
async def mark_all_as_read(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await db.execute(
        update(UserNotification)
        .where(
            UserNotification.user_id == user.id,
            UserNotification.is_read == False,
        )
        .values(is_read=True)
    )
    return {"message": "All notifications marked as read"}
