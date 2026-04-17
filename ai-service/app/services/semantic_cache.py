from __future__ import annotations

import hashlib
import json

import redis

from app.core.config import settings
from app.schemas.ai import InternalUserContext


class SemanticCacheService:
    def __init__(self) -> None:
        self._redis = redis.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            decode_responses=True,
        )

    def build_key(self, question: str, context: InternalUserContext) -> str:
        normalized = " ".join(question.lower().split())
        payload = f"{normalized}|{context.goal}|{context.tier}|{','.join(context.allergies)}"
        digest = hashlib.sha256(payload.encode("utf-8")).hexdigest()
        return f"semantic:ai:{digest}"

    def get(self, key: str) -> dict | None:
        raw = self._redis.get(key)
        if not raw:
            return None
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return None

    def set(self, key: str, answer: str, action: dict | None) -> None:
        payload = {
            "answer": answer,
            "action": action,
        }
        self._redis.setex(key, settings.redis_semantic_cache_ttl_seconds, json.dumps(payload, ensure_ascii=False))
