from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "Search Service"
    app_version: str = "1.0.0"
    debug: bool = True
    host: str = "0.0.0.0"
    port: int = 8002

    elasticsearch_host: str = "localhost"
    elasticsearch_port: int = 9200
    elasticsearch_url: str = "http://localhost:9200"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
