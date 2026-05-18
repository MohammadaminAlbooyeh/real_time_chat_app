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
async def test_get_my_profile(mock_decode, client, auth_header):
    mock_decode.return_value = {"sub": "00000000-0000-0000-0000-000000000001", "type": "access"}
    with patch("dependencies.get_db") as mock_db:
        mock_session = AsyncMock()
        mock_db.return_value.__aenter__.return_value = mock_session

        mock_user = AsyncMock()
        mock_user.id = "00000000-0000-0000-0000-000000000001"
        mock_user.username = "testuser"
        mock_user.email = "test@example.com"
        mock_user.display_name = "Test User"
        mock_user.avatar_url = None
        mock_user.bio = None
        mock_user.is_online = False
        mock_user.is_active = True
        mock_session.execute.return_value.scalar_one_or_none.return_value = mock_user

        response = await client.get("/api/v1/users/me", headers=auth_header)
        assert response.status_code == 200
