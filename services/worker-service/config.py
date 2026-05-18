from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "Worker Service"
    app_version: str = "1.0.0"
    debug: bool = True

    database_url_sync: str = "postgresql+psycopg2://postgres:postgres@localhost:5432/chat_db"
    redis_url: str = "redis://localhost:6379/0"

    celery_broker_url: str = "redis://localhost:6379/1"
    celery_result_backend: str = "redis://localhost:6379/2"

    search_service_url: str = "http://search-service:8002"

    jwt_secret_key: str = "change-this-to-a-secure-random-secret"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
