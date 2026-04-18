package com.chemilog.main.service;

import com.chemilog.main.api.meal.MealDayResponse;
import com.chemilog.main.api.meal.MealHistoryResponse;
import com.chemilog.main.api.meal.MealSyncRequest;
import com.chemilog.main.api.meal.MealSyncResponse;
import com.chemilog.main.api.meal.MealTodayResponse;
import com.chemilog.main.domain.food.FoodAdditiveMap;
import com.chemilog.main.domain.food.FoodItem;
import com.chemilog.main.domain.meal.Meal;
import com.chemilog.main.domain.meal.MealDetail;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserStatus;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.FoodAdditiveMapRepository;
import com.chemilog.main.repository.MealDetailRepository;
import com.chemilog.main.repository.MealRepository;
import com.chemilog.main.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final FoodAdditiveMapRepository foodAdditiveMapRepository;
    private final StringRedisTemplate redisTemplate;

    public MealService(
            UserRepository userRepository,
            MealRepository mealRepository,
            MealDetailRepository mealDetailRepository,
            FoodService foodService,
            FoodAdditiveMapRepository foodAdditiveMapRepository,
            StringRedisTemplate redisTemplate
    ) {
        this.userRepository = userRepository;
        this.mealRepository = mealRepository;
        this.mealDetailRepository = mealDetailRepository;
        this.foodService = foodService;
        this.foodAdditiveMapRepository = foodAdditiveMapRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(readOnly = true)
    public MealTodayResponse getToday(Long userId, LocalDate loggedDate) {
        LocalDate targetDate = loggedDate == null ? LocalDate.now() : loggedDate;
        User user = getActiveUser(userId);
        List<Meal> meals = mealRepository.findByUserAndMealDateAndDeletedFalse(user, targetDate);
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
            byDate.put(cursor, mealRepository.findByUserAndMealDateAndDeletedFalse(user, cursor));
            cursor = cursor.plusDays(1);
        }

        List<MealHistoryResponse.DaySummary> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Meal>> entry : byDate.entrySet()) {
            List<Meal> meals = entry.getValue();
            if (meals.isEmpty()) {
                days.add(new MealHistoryResponse.DaySummary(
                        entry.getKey(),
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        0,
                        List.of()
                ));
                continue;
            }

            BigDecimal calories = meals.stream()
                    .map(Meal::getTotalCalories)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            List<Long> mealIds = meals.stream().map(Meal::getMealId).toList();
            List<MealDetail> details = mealDetailRepository.findByMealMealIdIn(mealIds).stream()
                    .filter(detail -> detail.getQuantity() != null && detail.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            List<String> topAdditives = summarizeTopAdditives(details);
            days.add(new MealHistoryResponse.DaySummary(
                    entry.getKey(),
                    calories,
                    details.size(),
                    topAdditives
            ));
        }
        return new MealHistoryResponse(days);
    }

    @Transactional(readOnly = true)
    public MealDayResponse getDay(Long userId, LocalDate loggedDate) {
        LocalDate targetDate = loggedDate == null ? LocalDate.now() : loggedDate;
        User user = getActiveUser(userId);
        List<Meal> meals = mealRepository.findByUserAndMealDateAndDeletedFalse(user, targetDate);
        if (meals.isEmpty()) {
            return new MealDayResponse(targetDate, List.of());
        }

        List<Long> mealIds = meals.stream().map(Meal::getMealId).toList();
        Map<Long, List<MealDetail>> detailsByMealId = new HashMap<>();
        for (MealDetail detail : mealDetailRepository.findByMealMealIdIn(mealIds)) {
            detailsByMealId.computeIfAbsent(detail.getMeal().getMealId(), ignored -> new ArrayList<>()).add(detail);
        }

        List<MealDayResponse.MealEntry> entries = meals.stream()
                .sorted(Comparator.comparing(Meal::getCreatedAt).reversed())
                .map(meal -> {
                    List<MealDayResponse.Item> items = detailsByMealId
                            .getOrDefault(meal.getMealId(), List.of())
                            .stream()
                            .filter(detail -> detail.getQuantity() != null && detail.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                            .map(detail -> new MealDayResponse.Item(
                                    detail.getDetailId(),
                                    detail.getFood().getFoodId(),
                                    detail.getFood().getName(),
                                    detail.getFood().getCategory(),
                                    detail.getQuantity(),
                                    detail.getFood().getCalories()
                            ))
                            .toList();

                    return new MealDayResponse.MealEntry(
                            meal.getMealId(),
                            meal.getMealType(),
                            valueOrZero(meal.getTotalCalories()).setScale(2, RoundingMode.HALF_UP),
                            items
                    );
                })
                .filter(entry -> !entry.items().isEmpty())
                .toList();

        return new MealDayResponse(targetDate, entries);
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
            if (meal.isDeleted()) {
                redisTemplate.delete(key);
            } else {
                return buildSyncResponse(meal, mealDetailRepository.findByMealMealIdIn(List.of(mealId)));
            }
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

    @Transactional
    public MealSyncResponse updateMeal(Long userId, Long mealId, MealSyncRequest request) {
        if (request.items() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEAL-4002", "items 필드는 필수입니다.");
        }

        User user = getActiveUser(userId);
        Meal meal = mealRepository.findByMealIdAndUserAndDeletedFalse(mealId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEAL-4041", "수정할 식단 기록을 찾을 수 없습니다."));

        List<Long> foodIds = request.items().stream()
                .map(MealSyncRequest.Item::foodId)
                .distinct()
                .toList();

        Map<Long, FoodItem> foodMap = foodService.getFoodMapByIds(foodIds);
        if (foodMap.size() != foodIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEAL-4003", "존재하지 않거나 삭제된 food_id가 포함되어 있습니다.");
        }

        List<MealDetail> existingDetails = mealDetailRepository.findByMealMealId(mealId);
        Map<Long, MealDetail> detailByFoodId = new HashMap<>();
        for (MealDetail detail : existingDetails) {
            if (detail.getFood() != null && detail.getFood().getFoodId() != null) {
                detailByFoodId.putIfAbsent(detail.getFood().getFoodId(), detail);
            }
            detail.updateQuantity(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        List<MealDetail> newDetails = new ArrayList<>();
        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;

        for (MealSyncRequest.Item item : request.items()) {
            FoodItem food = foodMap.get(item.foodId());
            BigDecimal qty = item.quantity();

            MealDetail existing = detailByFoodId.get(item.foodId());
            if (existing != null) {
                existing.updateQuantity(qty);
            } else {
                newDetails.add(MealDetail.create(meal, food, qty));
            }

            totalCalories = totalCalories.add(valueOrZero(food.getCalories()).multiply(qty));
            totalCarbs = totalCarbs.add(valueOrZero(food.getCarbs()).multiply(qty));
            totalProtein = totalProtein.add(valueOrZero(food.getProtein()).multiply(qty));
            totalFat = totalFat.add(valueOrZero(food.getFat()).multiply(qty));
        }

        if (!newDetails.isEmpty()) {
            mealDetailRepository.saveAll(newDetails);
        }

        if (request.items().isEmpty()) {
            meal.markDeleted();
            meal.updateTotalCalories(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            Meal savedDeleted = mealRepository.save(meal);
            return new MealSyncResponse(
                    savedDeleted.getMealId(),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        meal.restore();
        meal.updateMealInfo(request.loggedDate(), request.mealType());
        meal.updateTotalCalories(totalCalories.setScale(2, RoundingMode.HALF_UP));

        Meal saved = mealRepository.save(meal);
        return new MealSyncResponse(
                saved.getMealId(),
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
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH-4030", "정지 또는 탈퇴 계정입니다.");
        }
        return user;
    }

    private String idempotencyStorageKey(Long userId, String idempotencyKey) {
        return "meal:sync:" + userId + ":" + idempotencyKey;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private MealSyncResponse buildSyncResponse(Meal meal, List<MealDetail> details) {
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;

        for (MealDetail detail : details) {
            BigDecimal qty = valueOrZero(detail.getQuantity());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            carbs = carbs.add(valueOrZero(detail.getFood().getCarbs()).multiply(qty));
            protein = protein.add(valueOrZero(detail.getFood().getProtein()).multiply(qty));
            fat = fat.add(valueOrZero(detail.getFood().getFat()).multiply(qty));
        }

        return new MealSyncResponse(
                meal.getMealId(),
                valueOrZero(meal.getTotalCalories()).setScale(2, RoundingMode.HALF_UP),
                carbs.setScale(2, RoundingMode.HALF_UP),
                protein.setScale(2, RoundingMode.HALF_UP),
                fat.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private List<String> summarizeTopAdditives(List<MealDetail> details) {
        List<Long> foodIds = details.stream()
                .map(detail -> detail.getFood().getFoodId())
                .distinct()
                .toList();
        if (foodIds.isEmpty()) {
            return List.of();
        }

        Map<Long, List<FoodAdditiveMap>> additiveMapByFoodId = foodAdditiveMapRepository
                .findByFoodFoodIdInAndDeletedFalse(foodIds)
                .stream()
                .collect(Collectors.groupingBy(map -> map.getFood().getFoodId()));

        Map<String, BigDecimal> additiveScore = new HashMap<>();
        for (MealDetail detail : details) {
            List<FoodAdditiveMap> mappings = additiveMapByFoodId.get(detail.getFood().getFoodId());
            if (mappings == null || mappings.isEmpty()) {
                continue;
            }
            BigDecimal quantity = valueOrZero(detail.getQuantity());
            for (FoodAdditiveMap map : mappings) {
                Integer dangerLevel = map.getAdditive().getDangerLevel();
                if (dangerLevel == null || dangerLevel < 4) {
                    continue;
                }
                BigDecimal score = quantity.multiply(BigDecimal.valueOf(dangerLevel));
                additiveScore.merge(map.getAdditive().getName(), score, BigDecimal::add);
            }
        }

        return additiveScore.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .limit(3)
                .toList();
    }
}
