package com.chemilog.main.service;

import com.chemilog.main.api.auth.UserMeResponse;
import com.chemilog.main.api.auth.UserProfileRequest;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserStatus;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void updateProfile(Long userId, UserProfileRequest request) {
        User user = userRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER-4040", "사용자를 찾을 수 없습니다."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH-4030", "정지 또는 탈퇴 계정입니다.");
        }
        Map<String, Object> healthProfile = new HashMap<>();
        healthProfile.put("goal", request.goal());
        healthProfile.put("strictness", request.strictness());
        healthProfile.put("allergies", request.allergies() == null ? List.of() : request.allergies());
        user.updateHealthProfile(healthProfile);
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMe(Long userId) {
        User user = userRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER-4040", "사용자를 찾을 수 없습니다."));

        Map<String, Object> healthProfile = user.getHealthProfile() == null ? Map.of() : user.getHealthProfile();
        String goal = toText(healthProfile.get("goal"), "MAINTAIN");
        String strictness = toText(healthProfile.get("strictness"), "MEDIUM");
        List<String> allergies = toStringList(healthProfile.get("allergies"));

        return new UserMeResponse(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                goal,
                strictness,
                allergies
        );
    }

    private String toText(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? defaultValue : text;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().filter(v -> v != null && !v.toString().isBlank()).map(Object::toString).toList();
        }
        String raw = value.toString().trim();
        if (raw.isEmpty()) {
            return List.of();
        }
        return List.of(raw);
    }
}
