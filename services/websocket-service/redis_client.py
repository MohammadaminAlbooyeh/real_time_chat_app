import json
from typing import Callable
import redis.asyncio as aioredis
from config import settings

redis: aioredis.Redis | None = None
pubsub: aioredis.Redis | None = None


async def connect_redis():
    global redis, pubsub
    redis = await aioredis.from_url(settings.redis_url, decode_responses=True)
    pubsub = redis.pubsub()


async def disconnect_redis():
    global redis, pubsub
    if pubsub:
        await pubsub.close()
    if redis:
        await redis.close()


async def publish(channel: str, message: dict):
    if redis:
        await redis.publish(channel, json.dumps(message))


async def subscribe(channel: str, callback: Callable):
    if pubsub:
        await pubsub.subscribe(channel)
        async for message in pubsub.listen():
            if message["type"] == "message":
                data = json.loads(message["data"])
                await callback(data)


async def set_user_presence(user_id: str, status: str):
    if redis:
        await redis.set(f"presence:{user_id}", status)


async def get_user_presence(user_id: str) -> str | None:
    if redis:
        return await redis.get(f"presence:{user_id}")
    return None


async def add_user_to_room(room_id: str, user_id: str):
    if redis:
        await redis.sadd(f"room:{room_id}:members", user_id)


async def remove_user_from_room(room_id: str, user_id: str):
    if redis:
        await redis.srem(f"room:{room_id}:members", user_id)


async def get_room_members(room_id: str) -> set:
    if redis:
        return await redis.smembers(f"room:{room_id}:members")
    return set()
