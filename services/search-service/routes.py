from fastapi import APIRouter, HTTPException, Query
from elasticsearch_client import index_message, search_messages, delete_message_from_index
from models import IndexMessageRequest, SearchResult

router = APIRouter(prefix="/api/v1/search", tags=["search"])


@router.post("/index")
async def index_message_endpoint(payload: IndexMessageRequest):
    try:
        await index_message(
            message_id=payload.id,
            content=payload.content,
            room_id=payload.room_id,
            sender_id=payload.sender_id,
            sender_username=payload.sender_username,
            message_type=payload.message_type,
            created_at=payload.created_at,
        )
        return {"status": "indexed", "id": payload.id}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Indexing failed: {str(e)}")


@router.get("/messages", response_model=SearchResult)
async def search_messages_endpoint(
    q: str = Query(..., min_length=1),
    room_id: str | None = None,
    limit: int = Query(default=50, le=200),
    offset: int = Query(default=0, ge=0),
):
    try:
        result = await search_messages(query=q, room_id=room_id, limit=limit, offset=offset)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")


@router.delete("/messages/{message_id}")
async def delete_message_endpoint(message_id: str):
    try:
        await delete_message_from_index(message_id)
        return {"status": "deleted", "id": message_id}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Deletion failed: {str(e)}")


@router.get("/health")
async def health():
    return {"status": "ok", "service": "search-service"}
