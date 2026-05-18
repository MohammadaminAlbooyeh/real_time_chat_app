from contextlib import asynccontextmanager
from fastapi import FastAPI
from config import settings
from elasticsearch_client import connect_elasticsearch, disconnect_elasticsearch
from routes import router


@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_elasticsearch()
    yield
    await disconnect_elasticsearch()


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan,
)

app.include_router(router)
