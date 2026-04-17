from __future__ import annotations

from datetime import date, datetime
from decimal import Decimal
from typing import Any

from sqlalchemy import Date, DateTime, Enum, ForeignKey, Numeric, String, Text, UniqueConstraint, func, text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.db.enums import MealType, UserRole, UserStatus, ViolationCategory

# AI service must not mutate core RDBMS runtime data directly.
# These models exist for typed contracts, validation, and read-only data references.

class TimestampMixin:
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )


class SoftDeleteMixin:
    is_deleted: Mapped[bool] = mapped_column(nullable=False, server_default=text("false"))


class User(TimestampMixin, SoftDeleteMixin, Base):
    __tablename__ = "users"

    user_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    email: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[UserRole] = mapped_column(
        Enum(UserRole, native_enum=False), nullable=False, default=UserRole.USER
    )
    status: Mapped[UserStatus] = mapped_column(
        Enum(UserStatus, native_enum=False), nullable=False, default=UserStatus.ACTIVE
    )
    health_profile: Mapped[dict[str, Any] | None] = mapped_column(JSONB)


class FoodItem(TimestampMixin, SoftDeleteMixin, Base):
    __tablename__ = "food_items"

    food_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(150), nullable=False, index=True)
    manufacturer: Mapped[str | None] = mapped_column(String(100))
    barcode: Mapped[str | None] = mapped_column(String(50), unique=True)
    image_url: Mapped[str | None] = mapped_column(String(512))
    calories: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False)
    carbs: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False, default=Decimal("0.00"))
    protein: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False, default=Decimal("0.00"))
    fat: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False, default=Decimal("0.00"))
    sugars: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False, default=Decimal("0.00"))
    sodium: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False, default=Decimal("0.00"))


class Additive(TimestampMixin, Base):
    __tablename__ = "additives"

    additive_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    purpose: Mapped[str | None] = mapped_column(String(50))
    danger_level: Mapped[int] = mapped_column(nullable=False)
    daily_acceptable_intake: Mapped[str | None] = mapped_column(String(100))


class FoodAdditivesMap(TimestampMixin, Base):
    __tablename__ = "food_additives_map"
    __table_args__ = (
        UniqueConstraint("food_id", "additive_id", name="uk_food_additives_map_food_additive"),
    )

    map_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    food_id: Mapped[int] = mapped_column(ForeignKey("food_items.food_id"), nullable=False, index=True)
    additive_id: Mapped[int] = mapped_column(ForeignKey("additives.additive_id"), nullable=False, index=True)
    is_deleted: Mapped[bool] = mapped_column(nullable=False, server_default=text("false"))


class Meal(TimestampMixin, Base):
    __tablename__ = "meals"

    meal_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.user_id"), nullable=False, index=True)
    meal_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    meal_type: Mapped[MealType] = mapped_column(Enum(MealType, native_enum=False), nullable=False)
    total_calories: Mapped[Decimal | None] = mapped_column(Numeric(8, 2))
    health_score: Mapped[int | None]


class MealDetail(TimestampMixin, Base):
    __tablename__ = "meal_details"

    detail_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    meal_id: Mapped[int] = mapped_column(ForeignKey("meals.meal_id"), nullable=False)
    food_id: Mapped[int] = mapped_column(ForeignKey("food_items.food_id"), nullable=False)
    quantity: Mapped[Decimal] = mapped_column(Numeric(5, 2), nullable=False)


class ViolationLog(TimestampMixin, Base):
    __tablename__ = "violation_logs"

    log_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[int | None] = mapped_column(ForeignKey("users.user_id"))
    input_text: Mapped[str] = mapped_column(Text, nullable=False)
    violation_category: Mapped[ViolationCategory | None] = mapped_column(
        Enum(ViolationCategory, native_enum=False)
    )
    confidence_score: Mapped[Decimal | None] = mapped_column(Numeric(3, 2))


class HallucinationLog(TimestampMixin, Base):
    __tablename__ = "hallucination_logs"

    log_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    model_version: Mapped[str | None] = mapped_column(String(50))
    prompt_context: Mapped[str | None] = mapped_column(Text)
    generated_response: Mapped[str | None] = mapped_column(Text)
    failed_reason: Mapped[str | None] = mapped_column(String(255))


class AdminAuditLog(TimestampMixin, Base):
    __tablename__ = "admin_audit_logs"

    audit_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    admin_id: Mapped[int] = mapped_column(ForeignKey("users.user_id"), nullable=False)
    action_type: Mapped[str] = mapped_column(String(50), nullable=False)
    target_entity: Mapped[str] = mapped_column(String(50), nullable=False)
    target_id: Mapped[int | None]
    payload_snapshot: Mapped[dict[str, Any] | None] = mapped_column(JSONB)
    ip_address: Mapped[str] = mapped_column(String(45), nullable=False)
