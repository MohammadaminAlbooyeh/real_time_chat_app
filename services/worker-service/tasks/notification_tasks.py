from main import celery_app
from utils import logger


@celery_app.task
def send_push_notification(user_id: str, title: str, body: str, data: dict | None = None):
    logger.info(f"Sending push notification to user {user_id}: {title}")


@celery_app.task
def send_email_notification(user_id: str, subject: str, body: str):
    logger.info(f"Sending email to user {user_id}: {subject}")


@celery_app.task
def notify_offline_users(room_id: str, message_preview: str, sender_name: str, exclude_user_ids: list[str]):
    logger.info(f"Notifying offline users in room {room_id} about message from {sender_name}")
