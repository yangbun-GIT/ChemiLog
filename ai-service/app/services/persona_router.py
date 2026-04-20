from __future__ import annotations

from app.schemas.ai import InternalUserContext, MentoringRequest

PERSONA_A = """
당신은 긍정적이고 따뜻한 15년 차 임상 영양사입니다.
- 사용자의 기록 노력을 먼저 인정합니다.
- 작은 행동 변화 중심으로 제안합니다.
- 비난/단정/의료 처방 표현은 금지합니다.
- 모든 답변은 한국어로 작성합니다.
"""

PERSONA_B = """
당신은 데이터 중심의 엄격한 영양 분석가입니다.
- 감정적 표현 없이 수치와 근거를 중심으로 답변합니다.
- 핵심 문제를 먼저 짚고 바로 실행안을 제시합니다.
- 의료 처방/진단 표현은 금지합니다.
- 모든 답변은 한국어로 작성합니다.
"""

OUTPUT_RULES = """
출력 형식 규칙:
1) 한 줄 요약
2) 핵심 근거 2~4개 (불릿)
3) 바로 실행 2~3개 (번호 목록)
4) 마지막 줄에 '다음 체크:' 항목 1개
- 줄바꿈을 적극 사용해 가독성을 높입니다.
"""


class PersonaRouter:
    def choose_persona(self, strictness: str) -> str:
        normalized = (strictness or "MEDIUM").upper()
        if normalized in {"HIGH", "STRICT", "EXTREME"}:
            return PERSONA_B
        return PERSONA_A

    def build_system_prompt(
        self,
        context: InternalUserContext,
        request: MentoringRequest,
        rag_context: str,
    ) -> str:
        profile = request.profile_context
        goal = (profile.goal if profile and profile.goal else context.goal or "MAINTAIN").upper()
        strictness = (
            profile.strictness if profile and profile.strictness else context.strictness or "MEDIUM"
        ).upper()
        allergies = profile.allergies if profile and profile.allergies else context.allergies
        persona = self.choose_persona(strictness)

        cart_preview = ", ".join(
            f"{item.name or item.food_id} x {item.quantity}" for item in request.current_cart
        ) or "없음"

        history_preview = "없음"
        if request.meal_history:
            rows = []
            for day in request.meal_history[-7:]:
                additives = ", ".join(day.top_additives[:3]) if day.top_additives else "-"
                rows.append(
                    f"{day.date}: kcal={day.total_calories}, items={day.item_count}, additives={additives}"
                )
            history_preview = " | ".join(rows)

        allergy_text = ", ".join(allergies) if allergies else "없음"
        selected_meal = (request.selected_meal_type or "UNKNOWN").upper()

        return (
            f"{persona}\n"
            "You are operating in ChemiLog nutrition domain.\n"
            "Hard rules:\n"
            "- 식단/영양 코칭 범위 내에서만 답변합니다.\n"
            "- 약물 처방, 진단, 민간요법 단정은 금지합니다.\n"
            "- 알레르기 유발 식품은 절대 추천하지 않습니다.\n"
            "- 불확실한 정보는 불확실하다고 명시합니다.\n"
            f"{OUTPUT_RULES}\n"
            "User context:\n"
            f"- user_id: {context.user_id}\n"
            f"- role: {context.role}\n"
            f"- tier: {context.tier}\n"
            f"- goal: {goal}\n"
            f"- strictness: {strictness}\n"
            f"- allergies: {allergy_text}\n"
            f"- selected_meal_type: {selected_meal}\n"
            f"- current_cart: {cart_preview}\n"
            f"- recent_meal_history: {history_preview}\n\n"
            f"RAG context:\n{rag_context or '없음'}\n"
        )
