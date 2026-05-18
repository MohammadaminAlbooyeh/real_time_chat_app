import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../../services/api-service"))

import pytest
from httpx import AsyncClient, ASGITransport
from main import app


@pytest.mark.asyncio
async def test_full_user_journey():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        health = await client.get("/health")
        assert health.status_code == 200

        register = await client.post(
            "/api/v1/auth/register",
            json={
                "username": f"testuser_{id(client)}",
                "email": f"test_{id(client)}@example.com",
                "password": "StrongPass1!",
            },
        )
        assert register.status_code in (200, 201, 409)
