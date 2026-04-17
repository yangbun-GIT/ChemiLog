package com.chemilog.main.service;

import com.chemilog.main.api.admin.AdminUserRowResponse;
import com.chemilog.main.api.common.PageInfo;
import com.chemilog.main.api.common.PagedData;
import com.chemilog.main.api.food.FoodResponse;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserRole;
import com.chemilog.main.domain.user.UserStatus;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDataService {

    private final UserRepository userRepository;
    private final FoodService foodService;

    public AdminDataService(UserRepository userRepository, FoodService foodService) {
        this.userRepository = userRepository;
        this.foodService = foodService;
    }

    @Transactional(readOnly = true)
    public PagedData<AdminUserRowResponse> users(String keyword, String status, String role, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        UserStatus userStatus = parseStatus(status);
        UserRole userRole = parseRole(role);

        Page<User> users = userRepository.searchForAdmin(
                normalizedKeyword,
                userStatus,
                userRole,
                PageRequest.of(page, size)
        );
        List<AdminUserRowResponse> items = users.getContent().stream()
                .map(this::toUserResponse)
                .toList();

        return new PagedData<>(
                items,
                new PageInfo(users.getNumber(), users.getTotalPages(), users.getTotalElements(), users.hasNext())
        );
    }

    @Transactional(readOnly = true)
    public PagedData<FoodResponse> foods(String keyword, String category, int page, int size) {
        return foodService.search(keyword, category, page, size);
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN-4001", "유효하지 않은 status 필터입니다.");
        }
    }

    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN-4002", "유효하지 않은 role 필터입니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private AdminUserRowResponse toUserResponse(User user) {
        Map<String, Object> profile = user.getHealthProfile();
        String goal = profile != null && profile.get("goal") != null ? profile.get("goal").toString() : "MAINTAIN";
        String strictness = profile != null && profile.get("strictness") != null
                ? profile.get("strictness").toString()
                : "MEDIUM";

        List<String> allergies;
        if (profile != null && profile.get("allergies") instanceof List<?> list) {
            allergies = list.stream().map(String::valueOf).toList();
        } else {
            allergies = List.of();
        }

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
}
