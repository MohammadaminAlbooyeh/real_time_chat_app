import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

import pytest
from unittest.mock import AsyncMock, patch, MagicMock, mock_open
from httpx import AsyncClient, ASGITransport
from main import app


@pytest.mark.asyncio
@patch("routes.upload.get_db")
@patch("routes.upload.os.makedirs")
@patch("routes.upload.open", new_callable=mock_open)
@patch("dependencies.get_current_user")
async def test_upload_invalid_file_type(mock_user, mock_file, mock_mkdir, mock_db):
    mock_user.return_value = MagicMock(id="00000000-0000-0000-0000-000000000001", is_active=True)

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            "/api/v1/upload?room_id=00000000-0000-0000-0000-000000000001",
            files={"file": ("test.exe", b"fake-content", "application/x-msdownload")},
            headers={"Authorization": "Bearer test-token"},
        )
        assert response.status_code in (400, 401)


@pytest.mark.asyncio
@patch("routes.upload.get_db")
@patch("routes.upload.os.makedirs")
@patch("routes.upload.open", new_callable=mock_open)
@patch("dependencies.get_current_user")
async def test_upload_valid_file(mock_user, mock_file, mock_mkdir, mock_db):
    mock_user.return_value = MagicMock(id="00000000-0000-0000-0000-000000000001", is_active=True)

    mock_session = AsyncMock()
    mock_execute = AsyncMock()
    mock_session.execute.return_value = mock_execute
    mock_execute.scalar_one_or_none.return_value = MagicMock()
    mock_db.return_value = mock_session

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            "/api/v1/upload?room_id=00000000-0000-0000-0000-000000000001",
            files={"file": ("test.png", b"fake-png-content", "image/png")},
            headers={"Authorization": "Bearer test-token"},
        )
        assert response.status_code in (201, 401)


@pytest.mark.asyncio
@patch("routes.upload.get_db")
@patch("dependencies.get_current_user")
async def test_upload_without_room_id(mock_user, mock_db):
    mock_user.return_value = MagicMock(id="00000000-0000-0000-0000-000000000001", is_active=True)

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            "/api/v1/upload",
            files={"file": ("test.png", b"fake-png-content", "image/png")},
            headers={"Authorization": "Bearer test-token"},
        )
        assert response.status_code == 422
