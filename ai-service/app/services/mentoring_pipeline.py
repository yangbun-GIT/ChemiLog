from __future__ import annotations

import json
from collections.abc import AsyncGenerator

from app.schemas.ai import InternalUserContext, MentoringRequest
from app.services.internal_log_client import InternalLogClient
from app.services.openai_service import OpenAIService
from app.services.persona_router import PersonaRouter
from app.services.quality_guard import QualityGuard
from app.services.rag_service import RagService
from app.services.semantic_cache import SemanticCacheService

FALLBACK_MESSAGE = "현재 정확한 의학적 정보를 분석하기 어렵습니다. 일반적인 영양 지침을 참고해 주세요."


class MentoringPipeline:
    def __init__(self) -> None:
        self.openai_service = OpenAIService()
        self.persona_router = PersonaRouter()
        self.rag_service = RagService(self.openai_service)
        self.quality_guard = QualityGuard()
        self.cache_service = SemanticCacheService()
        self.log_client = InternalLogClient()

    async def run(
        self,
        request: MentoringRequest,
        context: InternalUserContext,
    ) -> AsyncGenerator[dict[str, str], None]:
        latest_user_text = self._latest_user_text(request)
        policy_result = await self.openai_service.policy_check(latest_user_text)
        if not policy_result.valid:
            await self.log_client.log_violation(
                user_id=context.user_id,
                input_text=latest_user_text,
                category=policy_result.category,
                confidence_score=policy_result.confidence_score,
            )
            yield self._error_event(policy_result.reason or "안전 정책상 처리할 수 없는 요청입니다.")
            return

        additive_ids = [add_id for item in request.current_cart for add_id in item.additive_ids]
        rag_context = await self.rag_service.retrieve_context(latest_user_text, additive_ids)
        system_prompt = self.persona_router.build_system_prompt(context, request.current_cart, rag_context)

        cache_key = self.cache_service.build_key(latest_user_text, context)
        cached = self.cache_service.get(cache_key)
        if cached:
            yield self._message_event(cached.get("answer", ""), "generating")
            completed_payload = {"chunk": "", "status": "completed"}
            if cached.get("action"):
                completed_payload["action"] = cached["action"]
            yield {"event": "message", "data": json.dumps(completed_payload, ensure_ascii=False)}
            return

        messages: list[dict[str, str]] = [{"role": "system", "content": system_prompt}]
        messages.extend([msg.model_dump() for msg in request.chat_history])

        generated = ""
        async for token in self.openai_service.stream_chat(messages):
            generated += token
            quality_result = self.quality_guard.evaluate(generated, context.allergies)
            if quality_result.blocked:
                await self.log_client.log_hallucination(
                    model_version=self.openai_service.__class__.__name__,
                    prompt_context=system_prompt,
                    generated_response=generated,
                    failed_reason=quality_result.reason or "UNKNOWN",
                )
                yield self._error_event(FALLBACK_MESSAGE)
                return
            yield self._message_event(token, "generating")

        action = self._decide_action(request)
        self.cache_service.set(cache_key, generated, action)

        payload = {"chunk": "", "status": "completed"}
        if action:
            payload["action"] = action
        yield {"event": "message", "data": json.dumps(payload, ensure_ascii=False)}

    def _latest_user_text(self, request: MentoringRequest) -> str:
        for message in reversed(request.chat_history):
            if message.role == "user":
                return message.content
        return request.chat_history[-1].content

    def _message_event(self, chunk: str, status: str) -> dict[str, str]:
        return {
            "event": "message",
            "data": json.dumps({"chunk": chunk, "status": status}, ensure_ascii=False),
        }

    def _error_event(self, message: str) -> dict[str, str]:
        return {
            "event": "error",
            "data": json.dumps({"status": "fallback", "message": message}, ensure_ascii=False),
        }

    def _decide_action(self, request: MentoringRequest) -> dict | None:
        # 간단한 휴리스틱: 장바구니가 비어 있으면 예시 food_id를 제안하지 않는다.
        if not request.current_cart:
            return None
        return None
