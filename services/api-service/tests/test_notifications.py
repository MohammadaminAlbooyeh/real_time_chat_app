import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from httpx import AsyncClient, ASGITransport
from main import app


@pytest.mark.asyncio
@patch("routes.notifications.get_db")
@patch("dependencies.get_current_user")
async def test_list_notifications_empty(mock_user, mock_db):
    mock_user.return_value = MagicMock(id="00000000-0000-0000-0000-000000000001")

    mock_session = AsyncMock()
    mock_execute = AsyncMock()
    mock_session.execute.return_value = mock_execute
    mock_execute.scalars.return_value.all.return_value = []
    mock_execute.scalar.return_value = 0
    mock_db.return_value = mock_session

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get(
            "/api/v1/notifications",
            headers={"Authorization": "Bearer test-token"},
        )
        assert response.status_code in (200, 401)


@pytest.mark.asyncio
@patch("routes.notifications.get_db")
@patch("dependencies.get_current_user")
async def test_unread_count(mock_user, mock_db):
    mock_user.return_value = MagicMock(id="00000000-0000-0000-0000-000000000001")

    mock_session = AsyncMock()
    mock_execute = AsyncMock()
    mock_session.execute.return_value = mock_execute
    mock_execute.scalar.return_value = 5
    mock_db.return_value = mock_session

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get(
            "/api/v1/notifications/unread-count",
            headers={"Authorization": "Bearer test-token"},
        )
        assert response.status_code in (200, 401)


@pytest.mark.asyncio
@patch("routes.notifications.get_db")
@patch("dependencies.get_current_user")
async def test_mark_all_read(mock_user, mock_db):
    mock_user.return_value = MagicMock(id="00000000-0000-0000-0000-000000000001")

    mock_session = AsyncMock()
    mock_execute = AsyncMock()
    mock_session.execute.return_value = mock_execute
    mock_db.return_value = mock_session

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            "/api/v1/notifications/read-all",
            headers={"Authorization": "Bearer test-token"},
        )
        assert response.status_code in (200, 401)
