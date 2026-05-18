import pytest
from httpx import AsyncClient, ASGITransport
from main import app
from unittest.mock import AsyncMock, patch


@pytest.fixture
def client():
    transport = ASGITransport(app=app)
    return AsyncClient(transport=transport, base_url="http://test")


@pytest.fixture
def auth_header():
    return {"Authorization": "Bearer test-token"}


@pytest.mark.asyncio
@patch("dependencies.decode_token")
async def test_create_room(mock_decode, client, auth_header):
    mock_decode.return_value = {"sub": "00000000-0000-0000-0000-000000000001", "type": "access"}
    with patch("dependencies.get_db") as mock_db:
        mock_session = AsyncMock()
        mock_db.return_value.__aenter__.return_value = mock_session

        mock_session.execute.return_value.scalar.return_value = 0
        mock_session.execute.return_value.scalar_one_or_none.return_value = None

        response = await client.post(
            "/api/v1/rooms",
            json={"name": "Test Room", "description": "A test room"},
            headers=auth_header,
        )
        assert response.status_code == 201
