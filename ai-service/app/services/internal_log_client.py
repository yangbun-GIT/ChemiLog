from __future__ import annotations

import httpx

from app.core.config import settings


class InternalLogClient:
    async def log_violation(
        self,
        user_id: int,
        input_text: str,
        category: str | None,
        confidence_score: float,
    ) -> None:
        payload = {
            "userId": user_id,
            "inputText": input_text,
            "violationCategory": category,
            "confidenceScore": confidence_score,
        }
        await self._post("/violation", payload)

    async def log_hallucination(
        self,
        model_version: str,
        prompt_context: str,
        generated_response: str,
        failed_reason: str,
    ) -> None:
        payload = {
            "modelVersion": model_version,
            "promptContext": prompt_context,
            "generatedResponse": generated_response,
            "failedReason": failed_reason,
        }
        await self._post("/hallucination", payload)

    async def _post(self, suffix: str, payload: dict) -> None:
        try:
            async with httpx.AsyncClient(timeout=3.0) as client:
                await client.post(
                    f"{settings.spring_main_internal_url}{suffix}",
                    headers={"X-Internal-Secret": settings.internal_api_secret},
                    json=payload,
                )
        except Exception:
            # 내부 로그 실패는 멘토링 응답을 막지 않는다.
            return
