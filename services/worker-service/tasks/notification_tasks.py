import json
from main import celery_app
from database import get_session
from utils import logger
from sqlalchemy import text


@celery_app.task
def send_push_notification(user_id: str, title: str, body: str, data: dict | None = None):
    logger.info(f"Sending push notification to user {user_id}: {title}")
    session = get_session()
    try:
        user = session.execute(
            text("SELECT id, username, display_name FROM users WHERE id = :id"),
            {"id": user_id},
        ).fetchone()
        if user is None:
            logger.warning(f"User {user_id} not found for push notification")
            return

        notification_data = {
            "user_id": user_id,
            "title": title,
            "body": body,
            "data": data or {},
            "username": user.username,
        }
        logger.info(f"Push notification queued: {notification_data}")

        session.execute(
            text("""
                INSERT INTO user_notifications (user_id, title, body, data, notification_type)
                VALUES (:user_id, :title, :body, :data, 'push')
            """),
            {
                "user_id": user_id,
                "title": title,
                "body": body,
                "data": json.dumps(data or {}),
            },
        )
        session.commit()
        logger.info(f"Push notification stored for user {user_id}")
    except Exception as e:
        session.rollback()
        logger.error(f"Failed to send push notification to user {user_id}: {e}")
    finally:
        session.close()


@celery_app.task
def send_email_notification(user_id: str, subject: str, body: str):
    logger.info(f"Sending email to user {user_id}: {subject}")
    session = get_session()
    try:
        user = session.execute(
            text("SELECT id, email, username FROM users WHERE id = :id"),
            {"id": user_id},
        ).fetchone()
        if user is None:
            logger.warning(f"User {user_id} not found for email notification")
            return

        logger.info(
            f"Email would be sent to {user.email}:\n"
            f"Subject: {subject}\n"
            f"Body: {body[:200]}..."
        )

        session.execute(
            text("""
                INSERT INTO user_notifications (user_id, title, body, data, notification_type)
                VALUES (:user_id, :title, :body, :data, 'email')
            """),
            {
                "user_id": user_id,
                "title": subject,
                "body": body,
                "data": json.dumps({"to": user.email}),
            },
        )
        session.commit()
        logger.info(f"Email notification stored for user {user.email}")
    except Exception as e:
        session.rollback()
        logger.error(f"Failed to send email notification: {e}")
    finally:
        session.close()


@celery_app.task
def notify_offline_users(room_id: str, message_preview: str, sender_name: str, exclude_user_ids: list[str]):
    logger.info(f"Notifying offline users in room {room_id} about message from {sender_name}")
    session = get_session()
    try:
        members = session.execute(
            text("""
                SELECT u.id, u.username, u.email
                FROM room_members rm
                JOIN users u ON u.id = rm.user_id
                WHERE rm.room_id = :room_id
                AND u.is_online = FALSE
                AND u.is_active = TRUE
            """),
            {"room_id": room_id},
        ).fetchall()

        room = session.execute(
            text("SELECT name FROM rooms WHERE id = :id"),
            {"id": room_id},
        ).fetchone()
        room_name = room.name if room else "Unknown Room"

        exclude_set = set(exclude_user_ids)
        for member in members:
            if str(member.id) in exclude_set:
                continue
            send_push_notification.delay(
                user_id=str(member.id),
                title=f"New message in {room_name}",
                body=f"{sender_name}: {message_preview[:100]}",
                data={"room_id": room_id, "type": "new_message"},
            )

        logger.info(f"Queued offline notifications for {len(members)} users in room {room_id}")
    except Exception as e:
        logger.error(f"Failed to notify offline users in room {room_id}: {e}")
    finally:
        session.close()
