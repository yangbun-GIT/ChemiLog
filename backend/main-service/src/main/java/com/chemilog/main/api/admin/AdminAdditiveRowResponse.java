package com.chemilog.main.api.admin;

public record AdminAdditiveRowResponse(
        Long additiveId,
        String name,
        String purpose,
        Integer dangerLevel,
        String dailyAcceptableIntake,
        long mappedFoodCount
) {
}
