import json
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from connection_manager import manager
from middleware import verify_token
from presence_tracker import set_online, set_offline
from message_handler import (
    handle_send_message,
    handle_typing,
    handle_join_room,
    handle_leave_room,
)
from database import get_db

router = APIRouter()


@router.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket, db: AsyncSession = Depends(get_db)):
    token = websocket.query_params.get("token")
    if not token:
        await websocket.close(code=4001, reason="Missing token")
        return

    payload = await verify_token(token)
    if payload is None:
        await websocket.close(code=4001, reason="Invalid or expired token")
        return

    user_id = payload.get("sub")
    username = payload.get("username", "unknown")

    await manager.connect(user_id, websocket)
    await set_online(user_id, username)

    try:
        while True:
            raw = await websocket.receive_text()
            data = json.loads(raw)
            event_type = data.get("type")
            event_data = data.get("payload", {})

            if event_type == "message:send":
                await handle_send_message(event_data, user_id, username, db)
            elif event_type == "typing:start":
                await handle_typing({**event_data, "is_typing": True}, user_id, username)
            elif event_type == "typing:stop":
                await handle_typing({**event_data, "is_typing": False}, user_id, username)
            elif event_type == "room:join":
                await handle_join_room(event_data, user_id, username)
            elif event_type == "room:leave":
                await handle_leave_room(event_data, user_id, username)
            else:
                await manager.send_personal(
                    user_id,
                    {"type": "error", "payload": {"message": f"Unknown event type: {event_type}"}},
                )
    except WebSocketDisconnect:
        pass
    except Exception as e:
        await manager.send_personal(
            user_id,
            {"type": "error", "payload": {"message": str(e)}},
        )
    finally:
        await manager.disconnect(user_id)
        await set_offline(user_id, username)
