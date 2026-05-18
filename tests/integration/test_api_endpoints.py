import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../../services/api-service"))

import pytest
from httpx import AsyncClient, ASGITransport
from main import app


@pytest.fixture
def client():
    transport = ASGITransport(app=app)
    return AsyncClient(transport=transport, base_url="http://test")


@pytest.mark.asyncio
async def test_health_endpoint(client):
    response = await client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"
    assert response.json()["service"] == "api-service"


@pytest.mark.asyncio
async def test_docs_endpoint(client):
    response = await client.get("/docs")
    assert response.status_code in (200, 404)


@pytest.mark.asyncio
async def test_openapi_schema(client):
    response = await client.get("/openapi.json")
    assert response.status_code == 200
