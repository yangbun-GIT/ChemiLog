from __future__ import annotations

from enum import Enum


class UserRole(str, Enum):
    GUEST = "GUEST"
    USER = "USER"
    PREMIUM = "PREMIUM"
    ADMIN = "ADMIN"


class UserStatus(str, Enum):
    ACTIVE = "ACTIVE"
    SUSPENDED = "SUSPENDED"
    WITHDRAWN = "WITHDRAWN"


class MealType(str, Enum):
    BREAKFAST = "BREAKFAST"
    LUNCH = "LUNCH"
    DINNER = "DINNER"
    SNACK = "SNACK"


class ViolationCategory(str, Enum):
    PRO_ANA = "PRO_ANA"
    SELF_HARM = "SELF_HARM"
    JAILBREAK = "JAILBREAK"
    OUT_OF_DOMAIN = "OUT_OF_DOMAIN"
