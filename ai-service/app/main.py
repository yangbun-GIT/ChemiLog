from fastapi import FastAPI

from app.api.routes import router as api_router
from app.core.config import settings

app = FastAPI(
    title="ChemiLog AI Service",
    version="0.1.0",
    docs_url="/docs",
    redoc_url="/redoc",
)


@app.get("/", tags=["system"])
async def root() -> dict[str, str]:
    return {"service": "chemilog-ai-service", "env": settings.app_env}


app.include_router(api_router, prefix="/api/v1")
