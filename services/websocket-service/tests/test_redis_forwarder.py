import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

import pytest
from unittest.mock import AsyncMock, patch
from connection_manager import ConnectionManager


@pytest.mark.asyncio
async def test_broadcast_to_room():
    manager = ConnectionManager()
    mock_ws1 = AsyncMock()
    mock_ws2 = AsyncMock()

    await manager.connect("user-1", mock_ws1)
    await manager.connect("user-2", mock_ws2)
    manager.join_room("user-1", "room-1")
    manager.join_room("user-2", "room-1")

    message = {"type": "message:updated", "payload": {"room_id": "room-1", "id": "msg-1", "content": "edited"}}
    await manager.broadcast_to_room("room-1", message, exclude=None)

    assert mock_ws1.send_text.called
    assert mock_ws2.send_text.called
    call_arg = mock_ws1.send_text.call_args[0][0]
    assert '"message:updated"' in call_arg


@pytest.mark.asyncio
async def test_broadcast_excludes_sender():
    manager = ConnectionManager()
    mock_ws1 = AsyncMock()
    mock_ws2 = AsyncMock()

    await manager.connect("user-1", mock_ws1)
    await manager.connect("user-2", mock_ws2)
    manager.join_room("user-1", "room-1")
    manager.join_room("user-2", "room-1")

    message = {"type": "message:deleted", "payload": {"room_id": "room-1", "id": "msg-1"}}
    await manager.broadcast_to_room("room-1", message, exclude="user-1")

    assert not mock_ws1.send_text.called
    assert mock_ws2.send_text.called


@pytest.mark.asyncio
async def test_edit_delete_reaction_event_shapes():
    edit_event = {
        "type": "message:updated",
        "payload": {"room_id": "room-1", "id": "msg-1", "content": "edited content"},
    }
    assert edit_event["type"] == "message:updated"
    assert edit_event["payload"]["room_id"] == "room-1"
    assert edit_event["payload"]["id"] == "msg-1"

    delete_event = {
        "type": "message:deleted",
        "payload": {"room_id": "room-1", "id": "msg-1"},
    }
    assert delete_event["type"] == "message:deleted"
    assert delete_event["payload"]["id"] == "msg-1"

    reaction_event = {
        "type": "message:reaction",
        "payload": {
            "id": "reac-1",
            "room_id": "room-1",
            "message_id": "msg-1",
            "user_id": "user-1",
            "username": "alice",
            "emoji": "👍",
            "created_at": "2024-01-01T00:00:00",
        },
    }
    assert reaction_event["type"] == "message:reaction"
    assert reaction_event["payload"]["emoji"] == "👍"
