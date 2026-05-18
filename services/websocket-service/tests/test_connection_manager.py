import pytest
from unittest.mock import AsyncMock
from connection_manager import ConnectionManager


@pytest.fixture
def manager():
    return ConnectionManager()


@pytest.mark.asyncio
async def test_connect_and_disconnect(manager):
    ws = AsyncMock()
    await manager.connect("user-1", ws)
    assert manager.is_online("user-1")

    await manager.disconnect("user-1")
    assert not manager.is_online("user-1")


@pytest.mark.asyncio
async def test_room_join_leave(manager):
    manager.join_room("user-1", "room-1")
    assert "user-1" in manager.room_users["room-1"]

    manager.leave_room("user-1", "room-1")
    assert "room-1" not in manager.room_users


@pytest.mark.asyncio
async def test_broadcast_to_room(manager):
    ws1 = AsyncMock()
    ws2 = AsyncMock()
    await manager.connect("user-1", ws1)
    await manager.connect("user-2", ws2)

    manager.join_room("user-1", "room-1")
    manager.join_room("user-2", "room-1")

    await manager.broadcast_to_room("room-1", {"type": "test"})
    assert ws2.send_text.called


@pytest.mark.asyncio
async def test_send_personal(manager):
    ws = AsyncMock()
    await manager.connect("user-1", ws)
    await manager.send_personal("user-1", {"type": "test"})
    assert ws.send_text.called
