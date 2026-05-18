import json
from fastapi import WebSocket
from typing import Optional


class ConnectionManager:
    def __init__(self):
        self.active_connections: dict[str, WebSocket] = {}
        self.room_users: dict[str, set[str]] = {}

    async def connect(self, user_id: str, websocket: WebSocket):
        await websocket.accept()
        self.active_connections[user_id] = websocket

    async def disconnect(self, user_id: str):
        self.active_connections.pop(user_id, None)
        for room_id in list(self.room_users.keys()):
            self.room_users[room_id].discard(user_id)
            if not self.room_users[room_id]:
                del self.room_users[room_id]

    async def send_personal(self, user_id: str, message: dict):
        websocket = self.active_connections.get(user_id)
        if websocket:
            await websocket.send_text(json.dumps(message))

    async def broadcast_to_room(self, room_id: str, message: dict, exclude: Optional[str] = None):
        users = self.room_users.get(room_id, set())
        for user_id in users:
            if user_id == exclude:
                continue
            await self.send_personal(user_id, message)

    def join_room(self, user_id: str, room_id: str):
        if room_id not in self.room_users:
            self.room_users[room_id] = set()
        self.room_users[room_id].add(user_id)

    def leave_room(self, user_id: str, room_id: str):
        if room_id in self.room_users:
            self.room_users[room_id].discard(user_id)
            if not self.room_users[room_id]:
                del self.room_users[room_id]

    def get_online_users(self) -> list[str]:
        return list(self.active_connections.keys())

    def is_online(self, user_id: str) -> bool:
        return user_id in self.active_connections


manager = ConnectionManager()
