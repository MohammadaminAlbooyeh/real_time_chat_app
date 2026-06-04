import json
import redis.asyncio as aioredis
from config import settings

redis: aioredis.Redis | None = None


async def connect_redis():
    global redis
    redis = await aioredis.from_url(settings.redis_url, decode_responses=True)


async def disconnect_redis():
    global redis
    if redis:
        await redis.close()


async def publish(channel: str, message: dict):
    if redis:
        await redis.publish(channel, json.dumps(message))
