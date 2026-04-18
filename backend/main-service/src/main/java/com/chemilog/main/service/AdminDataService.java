package com.chemilog.main.service;

import com.chemilog.main.api.admin.AdminAdditiveRowResponse;
import com.chemilog.main.api.admin.AdminAdditiveUpsertRequest;
import com.chemilog.main.api.admin.AdminHallucinationLogRowResponse;
import com.chemilog.main.api.admin.AdminSearchMissRowResponse;
import com.chemilog.main.api.admin.AdminUserRowResponse;
import com.chemilog.main.api.admin.AdminViolationLogRowResponse;
import com.chemilog.main.api.common.PageInfo;
import com.chemilog.main.api.common.PagedData;
import com.chemilog.main.api.food.FoodResponse;
import com.chemilog.main.domain.food.Additive;
import com.chemilog.main.domain.log.HallucinationLog;
import com.chemilog.main.domain.log.ViolationCategory;
import com.chemilog.main.domain.log.ViolationLog;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserRole;
import com.chemilog.main.domain.user.UserStatus;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.AdditiveRepository;
import com.chemilog.main.repository.FoodAdditiveMapRepository;
import com.chemilog.main.repository.HallucinationLogRepository;
import com.chemilog.main.repository.UserRepository;
import com.chemilog.main.repository.ViolationLogRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDataService {

    private final UserRepository userRepository;
    private final FoodService foodService;
    private final AdditiveRepository additiveRepository;
    private final FoodAdditiveMapRepository foodAdditiveMapRepository;
    private final ViolationLogRepository violationLogRepository;
    private final HallucinationLogRepository hallucinationLogRepository;
    private final SearchMissLogService searchMissLogService;

    public AdminDataService(
            UserRepository userRepository,
            FoodService foodService,
            AdditiveRepository additiveRepository,
            FoodAdditiveMapRepository foodAdditiveMapRepository,
            ViolationLogRepository violationLogRepository,
            HallucinationLogRepository hallucinationLogRepository,
            SearchMissLogService searchMissLogService
    ) {
        this.userRepository = userRepository;
        this.foodService = foodService;
        this.additiveRepository = additiveRepository;
        this.foodAdditiveMapRepository = foodAdditiveMapRepository;
        this.violationLogRepository = violationLogRepository;
        this.hallucinationLogRepository = hallucinationLogRepository;
        this.searchMissLogService = searchMissLogService;
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

    @Transactional(readOnly = true)
    public PagedData<AdminAdditiveRowResponse> additives(String keyword, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Page<Additive> additives = additiveRepository.searchForAdmin(
                normalizedKeyword,
                PageRequest.of(page, size)
        );

        Map<Long, Long> mappedFoodCounts = new HashMap<>();
        for (Object[] row : foodAdditiveMapRepository.countByAdditive()) {
            Long additiveId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            mappedFoodCounts.put(additiveId, count);
        }

        List<AdminAdditiveRowResponse> items = additives.getContent().stream()
                .map(additive -> new AdminAdditiveRowResponse(
                        additive.getAdditiveId(),
                        additive.getName(),
                        additive.getPurpose(),
                        additive.getDangerLevel(),
                        additive.getDailyAcceptableIntake(),
                        mappedFoodCounts.getOrDefault(additive.getAdditiveId(), 0L)
                ))
                .toList();

        return new PagedData<>(
                items,
                new PageInfo(additives.getNumber(), additives.getTotalPages(), additives.getTotalElements(), additives.hasNext())
        );
    }

    @Transactional(readOnly = true)
    public PagedData<AdminViolationLogRowResponse> violationLogs(
            String category,
            String keyword,
            int page,
            int size
    ) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        ViolationCategory parsedCategory = parseViolationCategory(category);
        Page<ViolationLog> logs = violationLogRepository.searchForAdmin(
                parsedCategory,
                normalizedKeyword,
                PageRequest.of(page, size)
        );

        List<AdminViolationLogRowResponse> items = logs.getContent().stream()
                .map(log -> new AdminViolationLogRowResponse(
                        log.getLogId(),
                        log.getCreatedAt(),
                        log.getUser() == null ? null : log.getUser().getUserId(),
                        log.getUser() == null ? null : maskEmail(log.getUser().getEmail()),
                        log.getViolationCategory() == null ? null : log.getViolationCategory().name(),
                        log.getConfidenceScore(),
                        abbreviate(log.getInputText(), 200)
                ))
                .toList();

        return new PagedData<>(
                items,
                new PageInfo(logs.getNumber(), logs.getTotalPages(), logs.getTotalElements(), logs.hasNext())
        );
    }

    @Transactional(readOnly = true)
    public PagedData<AdminHallucinationLogRowResponse> hallucinationLogs(String keyword, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Page<HallucinationLog> logs = hallucinationLogRepository.searchForAdmin(
                normalizedKeyword,
                PageRequest.of(page, size)
        );

        List<AdminHallucinationLogRowResponse> items = logs.getContent().stream()
                .map(log -> new AdminHallucinationLogRowResponse(
                        log.getLogId(),
                        log.getCreatedAt(),
                        log.getModelVersion(),
                        log.getFailedReason(),
                        abbreviate(log.getPromptContext(), 180),
                        abbreviate(log.getGeneratedResponse(), 200)
                ))
                .toList();

        return new PagedData<>(
                items,
                new PageInfo(logs.getNumber(), logs.getTotalPages(), logs.getTotalElements(), logs.hasNext())
        );
    }

    @Transactional
    public AdminAdditiveRowResponse createAdditive(AdminAdditiveUpsertRequest request) {
        String normalizedName = normalizeName(request.name());
        Optional<Additive> existing = additiveRepository.findByNameIgnoreCase(normalizedName);
        if (existing.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN-4091", "already existing additive name");
        }

        Additive created = Additive.create(
                normalizedName,
                normalizeOptionalText(request.purpose()),
                request.dangerLevel(),
                normalizeOptionalText(request.dailyAcceptableIntake())
        );
        Additive saved = additiveRepository.save(created);
        return toAdditiveRow(saved, 0L);
    }

    @Transactional
    public AdminAdditiveRowResponse updateAdditive(Long additiveId, AdminAdditiveUpsertRequest request) {
        Additive additive = additiveRepository.findByAdditiveId(additiveId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ADMIN-4042", "additive not found"));

        String normalizedName = normalizeName(request.name());
        Optional<Additive> sameName = additiveRepository.findByNameIgnoreCase(normalizedName);
        if (sameName.isPresent() && !sameName.get().getAdditiveId().equals(additiveId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN-4092", "already existing additive name");
        }

        additive.update(
                normalizedName,
                normalizeOptionalText(request.purpose()),
                request.dangerLevel(),
                normalizeOptionalText(request.dailyAcceptableIntake())
        );
        Additive saved = additiveRepository.save(additive);

        long mappedCount = foodAdditiveMapRepository.countByAdditive().stream()
                .filter(row -> ((Number) row[0]).longValue() == additiveId)
                .map(row -> ((Number) row[1]).longValue())
                .findFirst()
                .orElse(0L);

        return toAdditiveRow(saved, mappedCount);
    }

    @Transactional(readOnly = true)
    public PagedData<AdminSearchMissRowResponse> searchMissLogs(
            String keyword,
            Boolean resolved,
            int page,
            int size
    ) {
        return searchMissLogService.list(keyword, resolved, page, size);
    }

    @Transactional
    public void resolveSearchMiss(Long missId, boolean resolved) {
        searchMissLogService.resolve(missId, resolved);
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

    private ViolationCategory parseViolationCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return ViolationCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN-4005", "유효하지 않은 violation category입니다.");
        }
    }

    private AdminUserRowResponse toUserResponse(User user) {
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

    private AdminAdditiveRowResponse toAdditiveRow(Additive additive, long mappedFoodCount) {
        return new AdminAdditiveRowResponse(
                additive.getAdditiveId(),
                additive.getName(),
                additive.getPurpose(),
                additive.getDangerLevel(),
                additive.getDailyAcceptableIntake(),
                mappedFoodCount
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

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN-4007", "name is required");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return "*@" + domain;
        }
        return local.substring(0, 2) + "***@" + domain;
    }
}
