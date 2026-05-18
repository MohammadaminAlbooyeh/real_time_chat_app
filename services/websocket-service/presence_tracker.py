from connection_manager import manager
from redis_client import set_user_presence, publish


async def set_online(user_id: str, username: str):
    await set_user_presence(user_id, "online")
    await publish("presence", {
        "type": "presence:online",
        "payload": {"user_id": user_id, "username": username, "status": "online"},
    })
    await manager.broadcast_to_room("__global__", {
        "type": "presence:online",
        "payload": {"user_id": user_id, "username": username, "status": "online"},
    })


async def set_offline(user_id: str, username: str):
    await set_user_presence(user_id, "offline")
    await publish("presence", {
        "type": "presence:offline",
        "payload": {"user_id": user_id, "username": username, "status": "offline"},
    })
    await manager.broadcast_to_room("__global__", {
        "type": "presence:offline",
        "payload": {"user_id": user_id, "username": username, "status": "offline"},
    })
