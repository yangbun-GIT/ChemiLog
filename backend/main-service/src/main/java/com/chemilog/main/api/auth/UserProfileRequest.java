package com.chemilog.main.api.auth;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UserProfileRequest(
        @NotBlank(message = "goal은 필수입니다.")
        String goal,
        @NotBlank(message = "strictness는 필수입니다.")
        String strictness,
        List<String> allergies
) {
}
