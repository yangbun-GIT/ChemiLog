package com.chemilog.main.service;

import com.chemilog.main.api.admin.AdminUserRowResponse;
import com.chemilog.main.api.admin.AdminUserUpdateRequest;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserRole;
import com.chemilog.main.domain.user.UserStatus;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AdminUserRowResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        User user = userRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ADMIN-4040", "수정할 사용자를 찾을 수 없습니다."));

        if (request.role() != null && !request.role().isBlank()) {
            user.updateRole(parseRole(request.role()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            user.updateStatus(parseStatus(request.status()));
        }

        Map<String, Object> profile = new HashMap<>(user.getHealthProfile() == null ? Map.of() : user.getHealthProfile());
        if (request.goal() != null && !request.goal().isBlank()) {
            profile.put("goal", request.goal().trim().toUpperCase());
        }
        if (request.strictness() != null && !request.strictness().isBlank()) {
            profile.put("strictness", request.strictness().trim().toUpperCase());
        }
        if (request.allergies() != null) {
            List<String> allergies = request.allergies().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .toList();
            profile.put("allergies", new ArrayList<>(allergies));
        }
        user.updateHealthProfile(profile);

        User saved = userRepository.save(user);
        return toRow(saved);
    }

    private AdminUserRowResponse toRow(User user) {
        Map<String, Object> profile = user.getHealthProfile();
        String goal = profile != null && profile.get("goal") != null ? profile.get("goal").toString() : "MAINTAIN";
        String strictness = profile != null && profile.get("strictness") != null
                ? profile.get("strictness").toString()
                : "MEDIUM";

        List<String> allergies = extractAllergies(profile);

        return new AdminUserRowResponse(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                goal,
                strictness,
                allergies,
                user.getCreatedAt()
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> extractAllergies(Map<String, Object> profile) {
        if (profile == null || profile.get("allergies") == null) {
            return List.of();
        }
        Object value = profile.get("allergies");
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(v -> v != null && !v.toString().isBlank())
                    .map(v -> v.toString().trim())
                    .toList();
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            return List.of();
        }
        return List.of(text);
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN-4003", "유효하지 않은 role 값입니다.");
        }
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN-4004", "유효하지 않은 status 값입니다.");
        }
    }
}
