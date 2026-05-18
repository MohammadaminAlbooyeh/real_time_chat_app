from sqlalchemy import text
from main import celery_app
from database import get_session
from utils import logger


@celery_app.task
def cleanup_expired_sessions():
    logger.info("Running scheduled maintenance: cleanup expired sessions")
    session = get_session()
    try:
        session.execute(text("DELETE FROM user_status_history WHERE changed_at < NOW() - INTERVAL '30 days'"))
        session.commit()
        logger.info("Expired sessions cleaned up")
    except Exception as e:
        session.rollback()
        logger.error(f"Maintenance task failed: {e}")
    finally:
        session.close()


@celery_app.task
def archive_old_messages():
    logger.info("Archiving messages older than 90 days")
    session = get_session()
    try:
        session.execute(
            text("""
                UPDATE messages
                SET is_deleted = TRUE
                WHERE created_at < NOW() - INTERVAL '90 days'
                AND is_deleted = FALSE
            """)
        )
        session.commit()
        logger.info("Old messages archived")
    except Exception as e:
        session.rollback()
        logger.error(f"Archive task failed: {e}")
    finally:
        session.close()
