from __future__ import annotations

from dataclasses import dataclass


@dataclass(slots=True)
class QualityResult:
    blocked: bool
    reason: str | None = None


class QualityGuard:
    def evaluate(self, text: str, allergies: list[str]) -> QualityResult:
        lowered = text.lower()

        forbidden_medical = [
            "처방",
            "약을 복용",
            "약물 복용",
            "진단 결과",
        ]
        if any(keyword in lowered for keyword in forbidden_medical):
            return QualityResult(True, "의학적 처방 수준의 조언이 감지되었습니다.")

        risky_remedies = [
            "검증되지 않은 민간요법",
            "단식만 하면",
            "해독 주스만",
        ]
        if any(keyword in lowered for keyword in risky_remedies):
            return QualityResult(True, "검증되지 않은 민간요법 제안이 감지되었습니다.")

        for allergy in allergies:
            allergy_token = allergy.lower()
            if allergy_token and allergy_token in lowered and ("추천" in lowered or "권장" in lowered):
                return QualityResult(True, f"알레르기({allergy}) 충돌 가능 식품이 추천되었습니다.")

        return QualityResult(False, None)
