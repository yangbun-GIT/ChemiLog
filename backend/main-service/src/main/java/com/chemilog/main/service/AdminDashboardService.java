package com.chemilog.main.service;

import com.chemilog.main.api.admin.AdminDashboardSummaryResponse;
import com.chemilog.main.domain.food.FoodItem;
import com.chemilog.main.domain.user.UserRole;
import com.chemilog.main.domain.user.UserStatus;
import com.chemilog.main.repository.AdditiveRepository;
import com.chemilog.main.repository.FoodItemRepository;
import com.chemilog.main.repository.HallucinationLogRepository;
import com.chemilog.main.repository.MealRepository;
import com.chemilog.main.repository.UserRepository;
import com.chemilog.main.repository.ViolationLogRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final AdditiveRepository additiveRepository;
    private final MealRepository mealRepository;
    private final ViolationLogRepository violationLogRepository;
    private final HallucinationLogRepository hallucinationLogRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            FoodItemRepository foodItemRepository,
            AdditiveRepository additiveRepository,
            MealRepository mealRepository,
            ViolationLogRepository violationLogRepository,
            HallucinationLogRepository hallucinationLogRepository
    ) {
        this.userRepository = userRepository;
        this.foodItemRepository = foodItemRepository;
        this.additiveRepository = additiveRepository;
        this.mealRepository = mealRepository;
        this.violationLogRepository = violationLogRepository;
        this.hallucinationLogRepository = hallucinationLogRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDate weekFrom = today.minusDays(6);

        List<AdminDashboardSummaryResponse.CategoryStat> categoryStats = foodItemRepository.countByCategory().stream()
                .map(row -> new AdminDashboardSummaryResponse.CategoryStat(
                        row[0] == null ? "기타" : row[0].toString(),
                        row[1] == null ? 0L : ((Number) row[1]).longValue()
                ))
                .toList();

        List<AdminDashboardSummaryResponse.RecentFood> recentFoods = foodItemRepository
                .findTop8ByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toRecentFood)
                .toList();

        return new AdminDashboardSummaryResponse(
                userRepository.countByDeletedFalse(),
                userRepository.countByStatusAndDeletedFalse(UserStatus.ACTIVE),
                userRepository.countByRoleAndDeletedFalse(UserRole.PREMIUM),
                userRepository.countByStatusAndDeletedFalse(UserStatus.SUSPENDED),
                foodItemRepository.countByDeletedFalse(),
                additiveRepository.count(),
                mealRepository.countByMealDateAndDeletedFalse(today),
                mealRepository.countByMealDateBetweenAndDeletedFalse(weekFrom, today),
                violationLogRepository.count(),
                hallucinationLogRepository.count(),
                today,
                categoryStats,
                recentFoods
        );
    }

    private AdminDashboardSummaryResponse.RecentFood toRecentFood(FoodItem food) {
        return new AdminDashboardSummaryResponse.RecentFood(
                food.getFoodId(),
                food.getName(),
                food.getCategory(),
                food.getManufacturer(),
                food.getCalories()
        );
    }
}
