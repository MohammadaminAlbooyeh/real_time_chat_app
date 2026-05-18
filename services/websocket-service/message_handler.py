import uuid
from datetime import datetime, timezone
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from connection_manager import manager
from redis_client import publish, add_user_to_room, remove_user_from_room


async def handle_send_message(data: dict, user_id: str, username: str, db: AsyncSession):
    room_id = data.get("room_id")
    content = data.get("content", "")
    message_type = data.get("message_type", "text")
    reply_to_id = data.get("reply_to_id")

    result = await db.execute(
        text(
            """INSERT INTO messages (room_id, sender_id, content, message_type, reply_to_id)
               VALUES (:room_id, :sender_id, :content, :message_type, :reply_to_id)
               RETURNING id, created_at"""
        ),
        {
            "room_id": uuid.UUID(room_id),
            "sender_id": uuid.UUID(user_id),
            "content": content,
            "message_type": message_type,
            "reply_to_id": uuid.UUID(reply_to_id) if reply_to_id else None,
        },
    )
    row = result.fetchone()
    msg_id = str(row[0])
    created_at = row[1].isoformat()

    payload = {
        "id": msg_id,
        "room_id": room_id,
        "sender_id": user_id,
        "sender_username": username,
        "content": content,
        "message_type": message_type,
        "reply_to_id": reply_to_id,
        "created_at": created_at,
        "reactions": [],
        "attachments": [],
    }

    await manager.broadcast_to_room(
        room_id, {"type": "message:new", "payload": payload}, exclude=user_id
    )
    await publish(
        f"room:{room_id}", {"type": "message:new", "payload": payload}
    )


async def handle_typing(data: dict, user_id: str, username: str):
    room_id = data.get("room_id")
    is_typing = data.get("is_typing", True)
    payload = {
        "room_id": room_id,
        "user_id": user_id,
        "username": username,
        "is_typing": is_typing,
    }
    await manager.broadcast_to_room(
        room_id, {"type": "typing:indicator", "payload": payload}, exclude=user_id
    )


async def handle_join_room(data: dict, user_id: str, username: str):
    room_id = data.get("room_id")
    manager.join_room(user_id, room_id)
    await add_user_to_room(room_id, user_id)
    payload = {"user_id": user_id, "username": username, "room_id": room_id}
    await manager.broadcast_to_room(
        room_id, {"type": "room:joined", "payload": payload}, exclude=user_id
    )


async def handle_leave_room(data: dict, user_id: str, username: str):
    room_id = data.get("room_id")
    manager.leave_room(user_id, room_id)
    await remove_user_from_room(room_id, user_id)
    payload = {"user_id": user_id, "username": username, "room_id": room_id}
    await manager.broadcast_to_room(
        room_id, {"type": "room:left", "payload": payload}, exclude=user_id
    )
