from main import celery_app
from database import get_session
from utils import logger


@celery_app.task
def index_message(message_id: str, content: str, room_id: str, sender_id: str):
    logger.info(f"Indexing message {message_id} in search service")
    try:
        import httpx
        from config import settings
        response = httpx.post(
            f"{settings.search_service_url}/index",
            json={
                "id": message_id,
                "content": content,
                "room_id": room_id,
                "sender_id": sender_id,
            },
            timeout=5,
        )
        response.raise_for_status()
        logger.info(f"Message {message_id} indexed successfully")
    except Exception as e:
        logger.error(f"Failed to index message {message_id}: {e}")


@celery_app.task
def process_message_attachments(message_id: str):
    logger.info(f"Processing attachments for message {message_id}")


@celery_app.task
def delete_message_from_index(message_id: str):
    logger.info(f"Deleting message {message_id} from search index")
