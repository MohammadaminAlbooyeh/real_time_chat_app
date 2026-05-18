from contextlib import asynccontextmanager
from fastapi import FastAPI
from config import settings
from redis_client import connect_redis, disconnect_redis
from routes import router
from database import engine


@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_redis()
    yield
    await disconnect_redis()
    await engine.dispose()


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan,
)

app.include_router(router)
