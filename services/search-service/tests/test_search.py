import pytest
from unittest.mock import AsyncMock, patch


@pytest.mark.asyncio
@patch("elasticsearch_client.es")
async def test_index_message(mock_es):
    from elasticsearch_client import index_message

    mock_es.index = AsyncMock()

    await index_message(
        message_id="msg-1",
        content="Hello world",
        room_id="room-1",
        sender_id="user-1",
    )
    assert mock_es.index.called


@pytest.mark.asyncio
@patch("elasticsearch_client.es")
async def test_search_messages(mock_es):
    from elasticsearch_client import search_messages

    mock_es.search = AsyncMock(return_value={
        "hits": {"hits": [], "total": {"value": 0}}
    })

    result = await search_messages(query="hello")
    assert result["total"] == 0


@pytest.mark.asyncio
@patch("elasticsearch_client.es")
async def test_delete_message(mock_es):
    from elasticsearch_client import delete_message_from_index

    mock_es.delete = AsyncMock()
    await delete_message_from_index("msg-1")
    assert mock_es.delete.called


@pytest.mark.asyncio
@patch("elasticsearch_client.es")
async def test_ensure_index_creation(mock_es):
    from elasticsearch_client import ensure_index

    mock_es.indices.exists = AsyncMock(return_value=False)
    mock_es.indices.create = AsyncMock()

    await ensure_index()
    assert mock_es.indices.create.called
