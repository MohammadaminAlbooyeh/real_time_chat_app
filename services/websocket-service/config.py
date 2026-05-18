from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "WebSocket Service"
    app_version: str = "1.0.0"
    debug: bool = True
    host: str = "0.0.0.0"
    port: int = 8001

    database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/chat_db"
    redis_url: str = "redis://localhost:6379/0"

    jwt_secret_key: str = "change-this-to-a-secure-random-secret"
    jwt_algorithm: str = "HS256"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
