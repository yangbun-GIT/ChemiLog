from __future__ import annotations

from datetime import date, datetime
from decimal import Decimal
from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.db.enums import MealType, UserRole, UserStatus, ViolationCategory


class ORMBaseSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)


class UserSchema(ORMBaseSchema):
    user_id: int
    email: str
    role: UserRole
    status: UserStatus
    health_profile: dict[str, Any] | None = None
    is_deleted: bool = False
    created_at: datetime
    updated_at: datetime


class FoodItemSchema(ORMBaseSchema):
    food_id: int
    name: str
    manufacturer: str | None = None
    barcode: str | None = None
    image_url: str | None = None
    calories: Decimal
    carbs: Decimal
    protein: Decimal
    fat: Decimal
    sugars: Decimal
    sodium: Decimal
    is_deleted: bool = False
    created_at: datetime
    updated_at: datetime


class AdditiveSchema(ORMBaseSchema):
    additive_id: int
    name: str
    purpose: str | None = None
    danger_level: int = Field(ge=1, le=5)
    daily_acceptable_intake: str | None = None
    created_at: datetime
    updated_at: datetime


class FoodAdditivesMapSchema(ORMBaseSchema):
    map_id: int
    food_id: int
    additive_id: int
    is_deleted: bool = False
    created_at: datetime
    updated_at: datetime


class MealSchema(ORMBaseSchema):
    meal_id: int
    user_id: int
    meal_date: date
    meal_type: MealType
    total_calories: Decimal | None = None
    health_score: int | None = Field(default=None, ge=1, le=100)
    created_at: datetime
    updated_at: datetime


class MealDetailSchema(ORMBaseSchema):
    detail_id: int
    meal_id: int
    food_id: int
    quantity: Decimal = Field(gt=Decimal("0"))
    created_at: datetime
    updated_at: datetime


class ViolationLogSchema(ORMBaseSchema):
    log_id: int
    user_id: int | None = None
    input_text: str
    violation_category: ViolationCategory | None = None
    confidence_score: Decimal | None = None
    created_at: datetime
    updated_at: datetime


class HallucinationLogSchema(ORMBaseSchema):
    log_id: int
    model_version: str | None = None
    prompt_context: str | None = None
    generated_response: str | None = None
    failed_reason: str | None = None
    created_at: datetime
    updated_at: datetime


class AdminAuditLogSchema(ORMBaseSchema):
    audit_id: int
    admin_id: int
    action_type: str
    target_entity: str
    target_id: int | None = None
    payload_snapshot: dict[str, Any] | None = None
    ip_address: str
    created_at: datetime
    updated_at: datetime
