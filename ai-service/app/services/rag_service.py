from __future__ import annotations

from pymilvus import Collection, connections

from app.core.config import settings
from app.services.openai_service import OpenAIService


class RagService:
    def __init__(self, openai_service: OpenAIService) -> None:
        self.openai_service = openai_service

    async def retrieve_context(self, question: str, additive_ids: list[int]) -> str:
        embedding = await self.openai_service.embed(question)
        if not embedding:
            return ""

        try:
            if not connections.has_connection("default"):
                connections.connect(
                    alias="default",
                    host=settings.milvus_host,
                    port=str(settings.milvus_port),
                )

            collection = Collection(settings.milvus_collection_name)
            expr = None
            if additive_ids:
                distinct_ids = sorted(set(additive_ids))
                expr = f"additive_id in [{','.join(str(v) for v in distinct_ids)}]"

            results = collection.search(
                data=[embedding],
                anns_field="embedding",
                param={"metric_type": "COSINE", "params": {"nprobe": 10}},
                limit=settings.rag_top_k,
                expr=expr,
                output_fields=["additive_id", "paper_title", "danger_level", "content"],
            )

            snippets: list[str] = []
            for hit in results[0]:
                distance = float(getattr(hit, "distance", 1.0))
                if distance > settings.rag_distance_threshold:
                    continue
                entity = hit.entity
                additive_id = entity.get("additive_id")
                paper_title = entity.get("paper_title")
                danger_level = entity.get("danger_level")
                content = entity.get("content")
                snippets.append(
                    f"[additive_id={additive_id}][danger={danger_level}] {paper_title}: {content}"
                )
            return "\n".join(snippets)
        except Exception:
            # Milvus가 비어 있거나 컬렉션이 없을 수 있으므로 RAG 실패 시 안전하게 폴백한다.
            return ""
