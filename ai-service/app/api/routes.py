from __future__ import annotations

from fastapi import APIRouter, Depends
from sse_starlette.sse import EventSourceResponse

from app.core.security import parse_internal_user_context
from app.schemas.ai import InternalUserContext, MentoringRequest
from app.services.mentoring_pipeline import MentoringPipeline

router = APIRouter()
pipeline = MentoringPipeline()


@router.get("/health", tags=["health"])
async def health() -> dict[str, str]:
    return {"status": "ok"}


@router.post("/ai/mentoring", tags=["ai"], response_class=EventSourceResponse)
async def mentoring(
    request: MentoringRequest,
    context: InternalUserContext = Depends(parse_internal_user_context),
) -> EventSourceResponse:
    event_generator = pipeline.run(request, context)
    return EventSourceResponse(event_generator)
