from main import celery_app
from celery.schedules import crontab

celery_app.conf.beat_schedule = {
    "cleanup-expired-sessions": {
        "task": "tasks.maintenance_tasks.cleanup_expired_sessions",
        "schedule": crontab(hour=3, minute=0),
    },
    "archive-old-messages": {
        "task": "tasks.maintenance_tasks.archive_old_messages",
        "schedule": crontab(hour=4, minute=0),
    },
    "update-user-statuses": {
        "task": "tasks.user_tasks.update_stale_statuses",
        "schedule": crontab(minute="*/5"),
    },
}
