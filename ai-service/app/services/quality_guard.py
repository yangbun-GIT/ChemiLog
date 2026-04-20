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
            "영양제 복용만으로 치료",
            "진단 결과 확정",
        ]
        if any(keyword in lowered for keyword in forbidden_medical):
            return QualityResult(True, "의료 처방/진단으로 해석될 수 있는 표현이 감지되었습니다.")

        risky_remedies = [
            "검증되지 않은 민간요법",
            "단식만 하면",
            "해독 주스만",
        ]
        if any(keyword in lowered for keyword in risky_remedies):
            return QualityResult(True, "검증되지 않은 민간요법 제안이 감지되었습니다.")

        for allergy in allergies:
            token = allergy.lower().strip()
            if token and token in lowered and ("추천" in lowered or "권장" in lowered):
                return QualityResult(True, f"알레르기({allergy}) 충돌 가능 식품 추천이 감지되었습니다.")

        return QualityResult(False, None)
