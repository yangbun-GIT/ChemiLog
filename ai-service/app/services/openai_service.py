from __future__ import annotations

import json
import re
from collections.abc import AsyncGenerator

from openai import AsyncOpenAI

from app.core.config import settings
from app.schemas.ai import PolicyResult

POLICY_SYSTEM_PROMPT = """
You are ChemiLog policy router.
Classify user input and return ONLY JSON with this schema:
{
  "valid": boolean,
  "category": "PRO_ANA" | "SELF_HARM" | "JAILBREAK" | "OUT_OF_DOMAIN" | null,
  "reason": string | null,
  "confidence_score": number
}
Rules:
- valid=false when harmful or out-of-domain.
- category and reason are required when valid=false.
- valid=true for nutrition/meal/additive/health-coaching requests.
- reason must be in Korean.
"""


class OpenAIService:
    def __init__(self) -> None:
        self._client = AsyncOpenAI(api_key=settings.openai_api_key) if settings.openai_api_key else None

    async def policy_check(self, text: str) -> PolicyResult:
        if self._client is None:
            return self._heuristic_policy_check(text)

        try:
            response = await self._client.chat.completions.create(
                model=settings.openai_policy_model,
                temperature=0,
                response_format={"type": "json_object"},
                messages=[
                    {"role": "system", "content": POLICY_SYSTEM_PROMPT},
                    {"role": "user", "content": f"input: {text}"},
                ],
            )
            raw = response.choices[0].message.content or "{}"
            parsed = json.loads(raw)
            valid = bool(parsed.get("valid", True))
            category = parsed.get("category")
            reason = parsed.get("reason")
            score = float(parsed.get("confidence_score", 0.0))

            if not valid and not category:
                category = "OUT_OF_DOMAIN"
            if not valid and not reason:
                reason = "식단 및 영양 관리와 관련된 질문으로 다시 입력해 주세요."
            if not valid and category == "OUT_OF_DOMAIN":
                reason = "식단, 영양, 건강 관련 질문만 답변할 수 있습니다."

            return PolicyResult(
                valid=valid,
                category=category,
                reason=reason,
                confidence_score=score,
            )
        except Exception:
            return self._heuristic_policy_check(text)

    async def embed(self, text: str) -> list[float] | None:
        if self._client is None:
            return None
        try:
            response = await self._client.embeddings.create(
                model=settings.openai_embedding_model,
                input=text,
            )
            return response.data[0].embedding
        except Exception:
            return None

    async def stream_chat(self, messages: list[dict[str, str]]) -> AsyncGenerator[str, None]:
        if self._client is None:
            fallback = (
                "현재 AI 연결이 준비되지 않았습니다. "
                "식단 기록을 계속 입력해 주시면 첨가물과 칼로리 기준으로 기본 피드백을 제공하겠습니다."
            )
            for token in re.split(r"(\s+)", fallback):
                if token:
                    yield token
            return

        stream = await self._client.chat.completions.create(
            model=settings.openai_main_model,
            temperature=0.4,
            stream=True,
            messages=messages,
        )
        async for chunk in stream:
            token = chunk.choices[0].delta.content or ""
            if token:
                yield token

    def _heuristic_policy_check(self, text: str) -> PolicyResult:
        lowered = text.lower()
        blocked = [
            ("PRO_ANA", ["먹토", "씹뱉", "극단적 단식", "거식"]),
            ("SELF_HARM", ["자해", "죽고", "죽고싶", "해치고"]),
            ("JAILBREAK", ["이전 지시 무시", "시스템 프롬프트", "관리자 권한"]),
        ]
        for category, keywords in blocked:
            if any(keyword in lowered for keyword in keywords):
                return PolicyResult(
                    valid=False,
                    category=category,
                    reason="안전 정책상 해당 요청은 처리할 수 없습니다.",
                    confidence_score=0.8,
                )

        domain_keywords = ["식단", "영양", "칼로리", "첨가물", "다이어트", "건강", "음식", "멘토링"]
        if not any(keyword in lowered for keyword in domain_keywords):
            return PolicyResult(
                valid=False,
                category="OUT_OF_DOMAIN",
                reason="식단, 영양, 건강 관련 질문만 답변할 수 있습니다.",
                confidence_score=0.7,
            )

        return PolicyResult(valid=True, category=None, reason=None, confidence_score=0.95)
