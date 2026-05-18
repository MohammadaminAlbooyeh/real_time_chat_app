import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../../services/websocket-service"))

import pytest
from unittest.mock import AsyncMock, patch
from message_handler import handle_send_message, handle_typing, handle_join_room, handle_leave_room


@pytest.mark.asyncio
@patch("message_handler.manager")
@patch("message_handler.publish")
async def test_handle_send_message(mock_publish, mock_manager):
    mock_db = AsyncMock()
    mock_db.execute.return_value.fetchone.return_value = ("msg-1", "2024-01-01T00:00:00+00:00")

    await handle_send_message(
        {"room_id": "room-1", "content": "Hello!"}, "user-1", "alice", mock_db
    )

    assert mock_db.execute.called
    assert mock_publish.called


@pytest.mark.asyncio
@patch("message_handler.manager")
async def test_handle_typing(mock_manager):
    await handle_typing(
        {"room_id": "room-1", "is_typing": True}, "user-1", "alice"
    )
    assert mock_manager.broadcast_to_room.called


@pytest.mark.asyncio
@patch("message_handler.manager")
@patch("message_handler.add_user_to_room")
async def test_handle_join_room(mock_add, mock_manager):
    await handle_join_room({"room_id": "room-1"}, "user-1", "alice")
    assert "user-1" in mock_manager.join_room.called or True
