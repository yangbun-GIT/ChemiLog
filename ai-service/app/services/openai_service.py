from __future__ import annotations

import json
import re
from collections.abc import AsyncGenerator

from openai import AsyncOpenAI

from app.core.config import settings
from app.schemas.ai import PolicyResult

POLICY_SYSTEM_PROMPT = (
    "당신은 ChemiLog 헬스케어 서비스의 1차 보안 라우터입니다. "
    "사용자 입력을 분석해 JSON으로만 응답하세요. "
    "카테고리: PRO_ANA, SELF_HARM, JAILBREAK, OUT_OF_DOMAIN."
)


class OpenAIService:
    def __init__(self) -> None:
        self._client = AsyncOpenAI(api_key=settings.openai_api_key) if settings.openai_api_key else None

    async def policy_check(self, text: str) -> PolicyResult:
        if self._client is None:
            return self._heuristic_policy_check(text)

        prompt = (
            "입력 텍스트를 분류하고 다음 JSON 스키마로 응답하세요: "
            '{"valid": boolean, "category": string|null, "reason": string|null, "confidence_score": number}. '
            "유효하면 valid=true, category/reason은 null."
        )
        response = await self._client.chat.completions.create(
            model=settings.openai_policy_model,
            temperature=0,
            response_format={"type": "json_object"},
            messages=[
                {"role": "system", "content": POLICY_SYSTEM_PROMPT},
                {"role": "user", "content": f"{prompt}\n입력: {text}"},
            ],
        )
        raw = response.choices[0].message.content or "{}"
        try:
            parsed = json.loads(raw)
            return PolicyResult(
                valid=bool(parsed.get("valid", True)),
                category=parsed.get("category"),
                reason=parsed.get("reason"),
                confidence_score=float(parsed.get("confidence_score", 0.0)),
            )
        except Exception:
            return self._heuristic_policy_check(text)

    async def embed(self, text: str) -> list[float] | None:
        if self._client is None:
            return None
        response = await self._client.embeddings.create(
            model=settings.openai_embedding_model,
            input=text,
        )
        return response.data[0].embedding

    async def stream_chat(self, messages: list[dict[str, str]]) -> AsyncGenerator[str, None]:
        if self._client is None:
            fallback = (
                "현재 AI 모델 연결이 준비되지 않았습니다. "
                "식단 기록을 유지하고 첨가물 경고 라벨이 있는 식품 섭취를 줄이는 방향으로 조정하세요."
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
                reason="식단 및 영양 관리와 관련된 질문만 답변할 수 있습니다.",
                confidence_score=0.7,
            )

        return PolicyResult(valid=True, category=None, reason=None, confidence_score=0.95)
