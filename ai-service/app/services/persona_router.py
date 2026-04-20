from __future__ import annotations

from app.schemas.ai import MentoringRequest
from app.schemas.ai import InternalUserContext

PERSONA_A = """
You are a warm and encouraging clinical nutrition mentor.
- First acknowledge the user's effort.
- Give practical and small-step recommendations.
- Never shame the user.
- Always respond in Korean.
"""

PERSONA_B = """
You are a strict, data-driven nutrition analyst.
- Be concise, factual, and direct.
- Focus on measurable tradeoffs and outcomes.
- Provide clear next actions without emotional fluff.
- Always respond in Korean.
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
            "You are operating in the ChemiLog nutrition domain.\n"
            "Hard rules:\n"
            "- Use only nutrition coaching language, not medical diagnosis or prescription.\n"
            "- Respect allergies strictly.\n"
            "- If evidence is weak, state uncertainty clearly.\n"
            "- Output must be in Korean.\n\n"
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
