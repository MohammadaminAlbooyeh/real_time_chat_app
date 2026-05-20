from contextlib import asynccontextmanager
from fastapi import FastAPI

from config import settings
from middleware import register_middlewares
from database import engine
from models.base import Base

from fastapi.staticfiles import StaticFiles

from routes.health import router as health_router
from routes.auth import router as auth_router
from routes.users import router as users_router
from routes.rooms import router as rooms_router
from routes.messages import router as messages_router
from routes.relationships import router as relationships_router
from routes.search import router as search_router
from routes.upload import router as upload_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    await engine.dispose()


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

register_middlewares(app)

import os

uploads_dir = os.path.join(os.path.dirname(__file__), "uploads")
os.makedirs(uploads_dir, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=uploads_dir), name="uploads")

app.include_router(health_router)
app.include_router(auth_router, prefix="/api/v1")
app.include_router(users_router, prefix="/api/v1")
app.include_router(rooms_router, prefix="/api/v1")
app.include_router(messages_router, prefix="/api/v1")
app.include_router(relationships_router, prefix="/api/v1")
app.include_router(search_router, prefix="/api/v1")
app.include_router(upload_router, prefix="/api/v1")
