import pytest
from unittest.mock import AsyncMock, patch


@pytest.mark.asyncio
@patch("routes.verify_token")
@patch("routes.manager")
async def test_websocket_connect_no_token(mock_manager, mock_verify):
    from routes import websocket_endpoint

    mock_ws = AsyncMock()
    mock_ws.query_params = {}
    mock_ws.close = AsyncMock()

    mock_db = AsyncMock()
    gen = mock_db()
    await gen.__anext__()

    with patch("routes.get_db", return_value=gen):
        await websocket_endpoint(mock_ws, mock_db)

    mock_ws.close.assert_called_once_with(code=4001, reason="Missing token")


@pytest.mark.asyncio
@patch("routes.verify_token")
@patch("routes.manager")
@patch("routes.set_online")
@patch("routes.set_offline")
async def test_websocket_connect_invalid_token(mock_off, mock_on, mock_manager, mock_verify, mock_token):
    mock_verify.return_value = None
    mock_ws = AsyncMock()
    mock_ws.query_params = {"token": "bad-token"}
    mock_ws.close = AsyncMock()

    mock_db = AsyncMock()

    from routes import websocket_endpoint
    await websocket_endpoint(mock_ws, mock_db)

    mock_ws.close.assert_called_once_with(code=4001, reason="Invalid or expired token")
