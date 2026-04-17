from __future__ import annotations

from app.schemas.ai import CartItem, InternalUserContext

PERSONA_A = (
    "당신은 긍정적이고 따뜻한 시선을 가진 15년 차 임상 영양사입니다. "
    "공감과 권유형 어투를 사용하고 강압적 표현을 피하십시오."
)

PERSONA_B = (
    "당신은 감정을 배제하고 결과 중심으로 분석하는 냉혹한 영양 분석가입니다. "
    "불필요한 인사 없이 수치와 근거 중심으로 단호하게 제안하십시오."
)


class PersonaRouter:
    def choose_persona(self, context: InternalUserContext) -> str:
        if context.strictness in {"HIGH", "STRICT", "EXTREME"}:
            return PERSONA_B
        return PERSONA_A

    def build_system_prompt(
        self,
        context: InternalUserContext,
        cart_items: list[CartItem],
        rag_context: str,
    ) -> str:
        persona = self.choose_persona(context)
        cart_preview = ", ".join(
            f"{item.name or item.food_id} x {item.quantity}" for item in cart_items
        ) or "없음"
        allergy_text = ", ".join(context.allergies) if context.allergies else "없음"
        return (
            f"{persona}\n"
            "아래 유저 컨텍스트를 반드시 반영해 답변하십시오.\n"
            f"- 목표(goal): {context.goal}\n"
            f"- 알레르기/질환(allergies): {allergy_text}\n"
            f"- 현재 장바구니(cart): {cart_preview}\n"
            "주의: 의학적 진단/처방을 하지 말고 영양 코칭에 집중하십시오.\n"
            f"RAG 컨텍스트:\n{rag_context}"
        )
