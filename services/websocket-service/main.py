import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
from config import settings
from redis_client import connect_redis, disconnect_redis, psubscribe
from connection_manager import manager
from routes import router
from database import engine


async def redis_event_forwarder():
    async def handle_room_event(message):
        room_id = message.get("payload", {}).get("room_id")
        if room_id:
            await manager.broadcast_to_room(room_id, message)

    await psubscribe("room:*", handle_room_event)


@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_redis()
    task = asyncio.create_task(redis_event_forwarder())
    yield
    task.cancel()
    await disconnect_redis()
    await engine.dispose()


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan,
)

app.include_router(router)
