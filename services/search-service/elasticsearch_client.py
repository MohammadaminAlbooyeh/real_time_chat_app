from elasticsearch import AsyncElasticsearch
from config import settings

es: AsyncElasticsearch | None = None

INDEX_NAME = "messages"


async def connect_elasticsearch():
    global es
    es = AsyncElasticsearch(settings.elasticsearch_url)
    await ensure_index()


async def disconnect_elasticsearch():
    global es
    if es:
        await es.close()


async def ensure_index():
    if not await es.indices.exists(index=INDEX_NAME):
        await es.indices.create(
            index=INDEX_NAME,
            body={
                "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 0,
                    "analysis": {
                        "analyzer": {
                            "message_analyzer": {
                                "type": "custom",
                                "tokenizer": "standard",
                                "filter": ["lowercase", "stop"],
                            }
                        }
                    },
                },
                "mappings": {
                    "properties": {
                        "id": {"type": "keyword"},
                        "content": {
                            "type": "text",
                            "analyzer": "message_analyzer",
                        },
                        "room_id": {"type": "keyword"},
                        "sender_id": {"type": "keyword"},
                        "sender_username": {"type": "keyword"},
                        "message_type": {"type": "keyword"},
                        "created_at": {"type": "date"},
                    }
                },
            },
        )


async def index_message(
    message_id: str,
    content: str,
    room_id: str,
    sender_id: str,
    sender_username: str = "",
    message_type: str = "text",
    created_at: str | None = None,
):
    doc = {
        "id": message_id,
        "content": content,
        "room_id": room_id,
        "sender_id": sender_id,
        "sender_username": sender_username,
        "message_type": message_type,
        "created_at": created_at,
    }
    await es.index(index=INDEX_NAME, id=message_id, document=doc, refresh="wait_for")


async def search_messages(
    query: str,
    room_id: str | None = None,
    limit: int = 50,
    offset: int = 0,
) -> dict:
    must_clauses = [{"match": {"content": {"query": query, "operator": "and"}}}]
    if room_id:
        must_clauses.append({"term": {"room_id": room_id}})

    body = {
        "query": {"bool": {"must": must_clauses}},
        "sort": [{"created_at": {"order": "desc"}}],
        "from": offset,
        "size": limit,
    }

    result = await es.search(index=INDEX_NAME, body=body)
    hits = result["hits"]["hits"]
    total = result["hits"]["total"]["value"]

    return {
        "total": total,
        "results": [hit["_source"] for hit in hits],
    }


async def delete_message_from_index(message_id: str):
    await es.delete(index=INDEX_NAME, id=message_id, ignore=[404])
