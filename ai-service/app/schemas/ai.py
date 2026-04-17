from __future__ import annotations

from decimal import Decimal
from typing import Literal

from pydantic import BaseModel, Field, field_validator


class ChatMessage(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str = Field(min_length=1, max_length=4000)


class CartItem(BaseModel):
    food_id: int = Field(alias="food_id")
    name: str | None = None
    quantity: Decimal = Field(default=Decimal("1.0"), gt=Decimal("0"))
    additive_ids: list[int] = Field(default_factory=list, alias="additive_ids")

    model_config = {"populate_by_name": True}

    @field_validator("additive_ids", mode="before")
    @classmethod
    def normalize_additive_ids(cls, value: object) -> list[int] | object:
        if value is None:
            return []
        return value


class MentoringRequest(BaseModel):
    chat_history: list[ChatMessage] = Field(min_length=1, alias="chat_history")
    current_cart: list[CartItem] = Field(default_factory=list, alias="current_cart")

    model_config = {"populate_by_name": True}


class PolicyResult(BaseModel):
    valid: bool
    category: str | None = None
    reason: str | None = None
    confidence_score: float = 0.0


class InternalUserContext(BaseModel):
    user_id: int
    role: str
    tier: str
    goal: str = "MAINTAIN"
    allergies: list[str] = Field(default_factory=list)
    strictness: str = "MEDIUM"
