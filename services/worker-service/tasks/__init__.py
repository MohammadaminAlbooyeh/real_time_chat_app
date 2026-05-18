from main import celery_app

celery_app.autodiscover_tasks()
