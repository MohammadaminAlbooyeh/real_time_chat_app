from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    app_name: str = "Real-Time Chat API"
    app_version: str = "1.0.0"
    debug: bool = False

    database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/chat_db"
    database_url_sync: str = "postgresql+psycopg2://postgres:postgres@localhost:5432/chat_db"

    redis_url: str = "redis://localhost:6379/0"

    jwt_secret_key: str = "change-this-to-a-secure-random-secret"
    jwt_algorithm: str = "HS256"
    jwt_access_token_expire_minutes: int = 30
    jwt_refresh_token_expire_days: int = 7

    allowed_origins: list[str] = ["http://localhost:3000", "http://localhost:5173"]

    max_room_name_length: int = 100
    max_message_length: int = 5000
    max_rooms_per_user: int = 100
    pagination_default_limit: int = 50
    pagination_max_limit: int = 200
    upload_dir: str = "uploads"
    max_upload_size: int = 50 * 1024 * 1024

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
