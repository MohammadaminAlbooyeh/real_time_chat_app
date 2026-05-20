import os
import uuid
from datetime import datetime, timezone
from main import celery_app
from database import get_session
from utils import logger
from sqlalchemy import text


@celery_app.task
def index_message(message_id: str, content: str, room_id: str, sender_id: str):
    logger.info(f"Indexing message {message_id} in search service")
    try:
        import httpx
        from config import settings
        response = httpx.post(
            f"{settings.search_service_url}/api/v1/search/index",
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
    session = get_session()
    try:
        attachments = session.execute(
            text("""
                SELECT id, file_name, file_url, file_type, file_size
                FROM message_attachments
                WHERE message_id = :message_id
            """),
            {"message_id": message_id},
        ).fetchall()

        if not attachments:
            logger.info(f"No attachments found for message {message_id}")
            return

        for att in attachments:
            file_path = att.file_url.lstrip("/uploads/")
            full_path = os.path.join(
                os.path.dirname(__file__), "..", "uploads", file_path
            )

            if os.path.exists(full_path):
                file_size = os.path.getsize(full_path)
                if file_size != att.file_size:
                    session.execute(
                        text("""
                            UPDATE message_attachments
                            SET file_size = :file_size, updated_at = :now
                            WHERE id = :id
                        """),
                        {
                            "file_size": file_size,
                            "now": datetime.now(timezone.utc),
                            "id": att.id,
                        },
                    )
                    logger.info(f"Updated file size for attachment {att.id}")

                if att.file_type == "image":
                    logger.info(f"Image attachment {att.id} ({att.file_name}) verified")
                else:
                    logger.info(f"Attachment {att.id} ({att.file_name}) verified")

            session.commit()
            logger.info(f"Processed attachment {att.id} for message {message_id}")

    except Exception as e:
        session.rollback()
        logger.error(f"Failed to process attachments for message {message_id}: {e}")
    finally:
        session.close()


@celery_app.task
def delete_message_from_index(message_id: str):
    logger.info(f"Deleting message {message_id} from search index")
    try:
        import httpx
        from config import settings
        response = httpx.delete(
            f"{settings.search_service_url}/api/v1/search/messages/{message_id}",
            timeout=5,
        )
        if response.status_code == 200:
            logger.info(f"Message {message_id} deleted from search index")
        else:
            logger.warning(f"Failed to delete message {message_id} from index: {response.status_code}")
    except Exception as e:
        logger.error(f"Failed to delete message {message_id} from index: {e}")
