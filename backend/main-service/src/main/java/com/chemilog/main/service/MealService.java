package com.chemilog.main.service;

import com.chemilog.main.api.meal.MealSyncRequest;
import com.chemilog.main.api.meal.MealSyncResponse;
import com.chemilog.main.api.meal.MealTodayResponse;
import com.chemilog.main.api.meal.MealHistoryResponse;
import com.chemilog.main.domain.food.FoodItem;
import com.chemilog.main.domain.meal.Meal;
import com.chemilog.main.domain.meal.MealDetail;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserStatus;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.MealDetailRepository;
import com.chemilog.main.repository.MealRepository;
import com.chemilog.main.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealService {

    private final UserRepository userRepository;
    private final MealRepository mealRepository;
    private final MealDetailRepository mealDetailRepository;
    private final FoodService foodService;
    private final StringRedisTemplate redisTemplate;

    public MealService(
            UserRepository userRepository,
            MealRepository mealRepository,
            MealDetailRepository mealDetailRepository,
            FoodService foodService,
            StringRedisTemplate redisTemplate
    ) {
        this.userRepository = userRepository;
        this.mealRepository = mealRepository;
        this.mealDetailRepository = mealDetailRepository;
        this.foodService = foodService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(readOnly = true)
    public MealTodayResponse getToday(Long userId, LocalDate loggedDate) {
        LocalDate targetDate = loggedDate == null ? LocalDate.now() : loggedDate;
        User user = getActiveUser(userId);
        List<Meal> meals = mealRepository.findByUserAndMealDate(user, targetDate);
        if (meals.isEmpty()) {
            return new MealTodayResponse(targetDate, List.of());
        }

        List<Long> mealIds = meals.stream().map(Meal::getMealId).toList();
        List<MealDetail> details = mealDetailRepository.findByMealMealIdIn(mealIds);
        Map<Long, BigDecimal> aggregated = new LinkedHashMap<>();
        for (MealDetail detail : details) {
            aggregated.merge(
                    detail.getFood().getFoodId(),
                    detail.getQuantity(),
                    BigDecimal::add
            );
        }
        List<MealTodayResponse.Item> items = aggregated.entrySet().stream()
                .map(entry -> new MealTodayResponse.Item(entry.getKey(), entry.getValue()))
                .toList();

        return new MealTodayResponse(targetDate, items);
    }

    @Transactional(readOnly = true)
    public MealHistoryResponse getHistory(Long userId, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(27) : from;
        if (start.isAfter(end)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEAL-4004", "from 날짜는 to 날짜보다 이후일 수 없습니다.");
        }

        User user = getActiveUser(userId);
        Map<LocalDate, List<Meal>> byDate = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            byDate.put(cursor, mealRepository.findByUserAndMealDate(user, cursor));
            cursor = cursor.plusDays(1);
        }

        List<MealHistoryResponse.DaySummary> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Meal>> entry : byDate.entrySet()) {
            List<Meal> meals = entry.getValue();
            if (meals.isEmpty()) {
                days.add(new MealHistoryResponse.DaySummary(entry.getKey(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0));
                continue;
            }
            BigDecimal calories = meals.stream()
                    .map(Meal::getTotalCalories)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<Long> mealIds = meals.stream().map(Meal::getMealId).toList();
            int itemCount = mealDetailRepository.findByMealMealIdIn(mealIds).size();
            days.add(new MealHistoryResponse.DaySummary(
                    entry.getKey(),
                    calories.setScale(2, RoundingMode.HALF_UP),
                    itemCount
            ));
        }
        return new MealHistoryResponse(days);
    }

    @Transactional
    public MealSyncResponse sync(Long userId, MealSyncRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEAL-4001", "Idempotency-Key 헤더가 필요합니다.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEAL-4002", "최소 1개 이상의 식품이 필요합니다.");
        }

        String key = idempotencyStorageKey(userId, idempotencyKey);
        String cachedMealId = redisTemplate.opsForValue().get(key);
        if (cachedMealId != null) {
            Long mealId = Long.valueOf(cachedMealId);
            Meal meal = mealRepository.findById(mealId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEAL-4040", "식단 기록을 찾을 수 없습니다."));
            List<MealDetail> details = mealDetailRepository.findByMealMealIdIn(List.of(mealId));
            BigDecimal carbs = BigDecimal.ZERO;
            BigDecimal protein = BigDecimal.ZERO;
            BigDecimal fat = BigDecimal.ZERO;
            for (MealDetail detail : details) {
                BigDecimal qty = detail.getQuantity();
                carbs = carbs.add(valueOrZero(detail.getFood().getCarbs()).multiply(qty));
                protein = protein.add(valueOrZero(detail.getFood().getProtein()).multiply(qty));
                fat = fat.add(valueOrZero(detail.getFood().getFat()).multiply(qty));
            }
            return new MealSyncResponse(
                    meal.getMealId(),
                    valueOrZero(meal.getTotalCalories()),
                    carbs.setScale(2, RoundingMode.HALF_UP),
                    protein.setScale(2, RoundingMode.HALF_UP),
                    fat.setScale(2, RoundingMode.HALF_UP)
            );
        }

        User user = getActiveUser(userId);
        List<Long> foodIds = request.items().stream().map(MealSyncRequest.Item::foodId).distinct().toList();
        Map<Long, FoodItem> foodMap = foodService.getFoodMapByIds(foodIds);
        if (foodMap.size() != foodIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEAL-4003", "존재하지 않거나 삭제된 food_id가 포함되어 있습니다.");
        }

        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        List<MealDetail> details = new ArrayList<>();

        Meal meal = Meal.create(user, request.loggedDate(), request.mealType(), BigDecimal.ZERO);
        Meal savedMeal = mealRepository.save(meal);

        for (MealSyncRequest.Item item : request.items()) {
            FoodItem food = foodMap.get(item.foodId());
            BigDecimal qty = item.quantity();
            totalCalories = totalCalories.add(valueOrZero(food.getCalories()).multiply(qty));
            totalCarbs = totalCarbs.add(valueOrZero(food.getCarbs()).multiply(qty));
            totalProtein = totalProtein.add(valueOrZero(food.getProtein()).multiply(qty));
            totalFat = totalFat.add(valueOrZero(food.getFat()).multiply(qty));
            details.add(MealDetail.create(savedMeal, food, qty));
        }
        mealDetailRepository.saveAll(details);

        savedMeal.updateTotalCalories(totalCalories.setScale(2, RoundingMode.HALF_UP));
        Meal finalized = mealRepository.save(savedMeal);
        redisTemplate.opsForValue().set(key, String.valueOf(finalized.getMealId()), Duration.ofDays(1));

        return new MealSyncResponse(
                finalized.getMealId(),
                totalCalories.setScale(2, RoundingMode.HALF_UP),
                totalCarbs.setScale(2, RoundingMode.HALF_UP),
                totalProtein.setScale(2, RoundingMode.HALF_UP),
                totalFat.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH-4003", "사용자를 찾을 수 없습니다."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH-4030", "정지 또는 탈퇴된 계정입니다.");
        }
        return user;
    }

    private String idempotencyStorageKey(Long userId, String idempotencyKey) {
        return "meal:sync:" + userId + ":" + idempotencyKey;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
