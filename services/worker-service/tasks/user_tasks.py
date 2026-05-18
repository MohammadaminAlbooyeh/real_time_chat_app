from sqlalchemy import text
from main import celery_app
from database import get_session
from utils import logger


@celery_app.task
def update_stale_statuses():
    logger.info("Updating stale user statuses")
    session = get_session()
    try:
        session.execute(
            text("""
                UPDATE users
                SET is_online = FALSE
                WHERE is_online = TRUE
                AND last_seen < NOW() - INTERVAL '5 minutes'
            """)
        )
        session.commit()
        logger.info("Stale statuses updated")
    except Exception as e:
        session.rollback()
        logger.error(f"Failed to update statuses: {e}")
    finally:
        session.close()


@celery_app.task
def cleanup_inactive_users():
    logger.info("Cleaning up inactive users")
    session = get_session()
    try:
        session.execute(
            text("""
                UPDATE users
                SET is_active = FALSE
                WHERE is_active = TRUE
                AND last_seen < NOW() - INTERVAL '90 days'
            """)
        )
        session.commit()
        logger.info("Inactive users cleaned up")
    except Exception as e:
        session.rollback()
        logger.error(f"Failed to clean up inactive users: {e}")
    finally:
        session.close()
