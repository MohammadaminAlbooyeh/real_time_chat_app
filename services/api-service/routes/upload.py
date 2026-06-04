import uuid
import os
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from database import get_db
from models.user import User
from models.message import Message, MessageAttachment
from models.room import RoomMember
from dependencies import get_current_user
from config import settings

router = APIRouter(prefix="/upload", tags=["upload"])

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "..", "uploads")
ALLOWED_CONTENT_TYPES = {
    "image/jpeg": "image",
    "image/png": "image",
    "image/gif": "image",
    "image/webp": "image",
    "application/pdf": "document",
    "text/plain": "document",
    "application/zip": "archive",
    "application/x-rar-compressed": "archive",
    "application/x-7z-compressed": "archive",
    "application/msword": "document",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": "document",
}
MAX_FILE_SIZE = 50 * 1024 * 1024


@router.post("", status_code=status.HTTP_201_CREATED)
async def upload_file(
    file: UploadFile = File(...),
    room_id: str = Query(...),
    message_id: str | None = Query(None),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File type {file.content_type} is not allowed",
        )

    content = await file.read()
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File size exceeds {MAX_FILE_SIZE // (1024*1024)}MB limit",
        )

    membership = await db.execute(
        select(RoomMember).where(
            RoomMember.room_id == uuid.UUID(room_id),
            RoomMember.user_id == user.id,
        )
    )
    if membership.scalar_one_or_none() is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not a member of this room",
        )

    if message_id:
        msg_result = await db.execute(
            select(Message).where(
                Message.id == uuid.UUID(message_id),
                Message.room_id == uuid.UUID(room_id),
            )
        )
        if msg_result.scalar_one_or_none() is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Message not found in this room",
            )

    file_ext = os.path.splitext(file.filename or "file")[1]
    file_uuid = uuid.uuid4()
    stored_name = f"{file_uuid}{file_ext}"

    date_path = datetime.now(timezone.utc).strftime("%Y/%m/%d")
    dest_dir = os.path.join(UPLOAD_DIR, date_path)
    os.makedirs(dest_dir, exist_ok=True)

    file_path = os.path.join(dest_dir, stored_name)
    with open(file_path, "wb") as f:
        f.write(content)

    file_url = f"/uploads/{date_path}/{stored_name}"
    file_type = ALLOWED_CONTENT_TYPES.get(file.content_type, "other")

    if message_id:
        attachment = MessageAttachment(
            id=file_uuid,
            message_id=uuid.UUID(message_id),
            file_name=file.filename or "file",
            file_url=file_url,
            file_type=file_type,
            file_size=len(content),
        )
        db.add(attachment)
        await db.flush()
        return {
            "id": str(attachment.id),
            "message_id": message_id,
            "file_name": attachment.file_name,
            "file_url": attachment.file_url,
            "file_type": attachment.file_type,
            "file_size": attachment.file_size,
            "mime_type": file.content_type,
        }

    return {
        "id": str(file_uuid),
        "file_name": file.filename,
        "file_url": file_url,
        "file_type": file_type,
        "file_size": len(content),
        "mime_type": file.content_type,
    }
