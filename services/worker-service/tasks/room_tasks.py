from sqlalchemy import text
from main import celery_app
from database import get_session
from utils import logger


@celery_app.task
def cleanup_abandoned_rooms():
    logger.info("Cleaning up abandoned rooms")
    session = get_session()
    try:
        session.execute(
            text("""
                DELETE FROM rooms
                WHERE id NOT IN (SELECT DISTINCT room_id FROM room_members)
                AND created_at < NOW() - INTERVAL '30 days'
            """)
        )
        session.commit()
        logger.info("Abandoned rooms cleaned up")
    except Exception as e:
        session.rollback()
        logger.error(f"Failed to clean up rooms: {e}")
    finally:
        session.close()


@celery_app.task
def update_room_last_activity(room_id: str):
    logger.info(f"Updating last activity for room {room_id}")
    session = get_session()
    try:
        session.execute(
            text("UPDATE rooms SET updated_at = NOW() WHERE id = :room_id"),
            {"room_id": room_id},
        )
        session.commit()
    except Exception as e:
        session.rollback()
        logger.error(f"Failed to update room activity: {e}")
    finally:
        session.close()
