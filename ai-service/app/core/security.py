from __future__ import annotations

from fastapi import Header, HTTPException, status

from app.core.config import settings
from app.schemas.ai import InternalUserContext


def parse_internal_user_context(
    x_internal_secret: str = Header(alias="X-Internal-Secret"),
    x_internal_user_id: str = Header(alias="X-Internal-User-Id"),
    x_internal_user_role: str = Header(alias="X-Internal-User-Role"),
    x_internal_user_tier: str = Header(alias="X-Internal-User-Tier"),
    x_internal_user_goal: str | None = Header(default=None, alias="X-Internal-User-Goal"),
    x_internal_user_allergies: str | None = Header(default=None, alias="X-Internal-User-Allergies"),
    x_internal_user_strictness: str | None = Header(default=None, alias="X-Internal-User-Strictness"),
) -> InternalUserContext:
    if x_internal_secret != settings.internal_api_secret:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden internal request.")

    try:
        user_id = int(x_internal_user_id)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid internal user id.") from e

    allergies = []
    if x_internal_user_allergies:
        allergies = [value.strip() for value in x_internal_user_allergies.split(",") if value.strip()]

    return InternalUserContext(
        user_id=user_id,
        role=x_internal_user_role,
        tier=x_internal_user_tier,
        goal=(x_internal_user_goal or "MAINTAIN").upper(),
        allergies=allergies,
        strictness=(x_internal_user_strictness or "MEDIUM").upper(),
    )
