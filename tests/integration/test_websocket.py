import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../../services/websocket-service"))

import pytest
from unittest.mock import AsyncMock, patch
from connection_manager import ConnectionManager


@pytest.fixture
def manager():
    return ConnectionManager()


@pytest.mark.asyncio
async def test_connect_disconnect(manager):
    mock_ws = AsyncMock()
    await manager.connect("user-1", mock_ws)
    assert "user-1" in manager.active_connections
    assert manager.is_online("user-1") is True

    await manager.disconnect("user-1")
    assert "user-1" not in manager.active_connections
    assert manager.is_online("user-1") is False


@pytest.mark.asyncio
async def test_room_management(manager):
    manager.join_room("user-1", "room-1")
    assert "room-1" in manager.room_users
    assert "user-1" in manager.room_users["room-1"]

    manager.leave_room("user-1", "room-1")
    assert "room-1" not in manager.room_users


@pytest.mark.asyncio
async def test_broadcast(manager):
    mock_ws1 = AsyncMock()
    mock_ws2 = AsyncMock()
    await manager.connect("user-1", mock_ws1)
    await manager.connect("user-2", mock_ws2)

    manager.join_room("user-1", "room-1")
    manager.join_room("user-2", "room-1")

    await manager.broadcast_to_room("room-1", {"type": "test", "payload": {}}, exclude="user-1")

    assert mock_ws2.send_text.called
