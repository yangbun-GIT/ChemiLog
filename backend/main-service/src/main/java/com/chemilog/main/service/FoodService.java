package com.chemilog.main.service;

import com.chemilog.main.api.common.PageInfo;
import com.chemilog.main.api.common.PagedData;
import com.chemilog.main.api.food.FoodCategoryResponse;
import com.chemilog.main.api.food.FoodRecommendationResponse;
import com.chemilog.main.api.food.FoodResponse;
import com.chemilog.main.api.food.FoodUpsertRequest;
import com.chemilog.main.domain.food.Additive;
import com.chemilog.main.domain.food.FoodAdditiveMap;
import com.chemilog.main.domain.food.FoodItem;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.AdditiveRepository;
import com.chemilog.main.repository.FoodAdditiveMapRepository;
import com.chemilog.main.repository.FoodItemRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodService {

    private final FoodItemRepository foodItemRepository;
    private final AdditiveRepository additiveRepository;
    private final FoodAdditiveMapRepository foodAdditiveMapRepository;
    private final SearchMissLogService searchMissLogService;

    public FoodService(
            FoodItemRepository foodItemRepository,
            AdditiveRepository additiveRepository,
            FoodAdditiveMapRepository foodAdditiveMapRepository,
            SearchMissLogService searchMissLogService
    ) {
        this.foodItemRepository = foodItemRepository;
        this.additiveRepository = additiveRepository;
        this.foodAdditiveMapRepository = foodAdditiveMapRepository;
        this.searchMissLogService = searchMissLogService;
    }

    @Transactional(readOnly = true)
    public PagedData<FoodResponse> search(String keyword, String category, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedCategory = category == null ? "" : category.trim();

        Page<FoodItem> foods = foodItemRepository.search(
                normalizedKeyword,
                normalizedCategory,
                PageRequest.of(page, size)
        );

        List<FoodResponse> items = foods.getContent().stream()
                .map(this::toResponse)
                .toList();

        if (items.isEmpty() && !normalizedKeyword.isBlank() && page == 0) {
            searchMissLogService.record(normalizedKeyword);
        }

        return new PagedData<>(
                items,
                new PageInfo(foods.getNumber(), foods.getTotalPages(), foods.getTotalElements(), foods.hasNext())
        );
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> popular(String category, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 30));
        String normalizedCategory = category == null ? "" : category.trim();
        return foodItemRepository.findPopularFoods(normalizedCategory, PageRequest.of(0, normalizedLimit)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FoodRecommendationResponse recommendation(String goal, String strictness, String seedValue) {
        String normalizedGoal = normalizeGoal(goal);
        String normalizedStrictness = normalizeStrictness(strictness);
        long seed = resolveSeed(seedValue);

        List<RecommendationSlot> slots = buildRecommendationSlots(normalizedGoal);
        Set<Long> usedFoodIds = new HashSet<>();
        List<FoodRecommendationResponse.MealRecommendation> meals = new ArrayList<>();

        for (RecommendationSlot slot : slots) {
            FoodResponse picked = pickRecommendation(slot, normalizedGoal, normalizedStrictness, usedFoodIds, seed);
            if (picked.foodId() != null) {
                usedFoodIds.add(picked.foodId());
            }
            meals.add(new FoodRecommendationResponse.MealRecommendation(slot.mealType(), slot.reason(), picked));
        }

        return new FoodRecommendationResponse(normalizedGoal, normalizedStrictness, meals);
    }

    @Transactional
    public FoodResponse create(FoodUpsertRequest request) {
        FoodItem saved = foodItemRepository.save(FoodItem.create(
                request.name(),
                request.category(),
                request.manufacturer(),
                request.barcode(),
                request.imageUrl(),
                request.calories(),
                request.carbs(),
                request.protein(),
                request.fat(),
                request.sugars(),
                request.sodium()
        ));

        replaceAdditives(saved, request.additiveIds());
        return toResponse(saved);
    }

    @Transactional
    public FoodResponse update(Long foodId, FoodUpsertRequest request) {
        FoodItem foodItem = foodItemRepository.findByFoodIdAndDeletedFalse(foodId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FOOD-4040", "식품을 찾을 수 없습니다."));

        foodItem.update(
                request.name(),
                request.category(),
                request.manufacturer(),
                request.barcode(),
                request.imageUrl(),
                request.calories(),
                request.carbs(),
                request.protein(),
                request.fat(),
                request.sugars(),
                request.sodium()
        );

        replaceAdditives(foodItem, request.additiveIds());
        return toResponse(foodItem);
    }

    @Transactional
    public void softDelete(Long foodId) {
        FoodItem foodItem = foodItemRepository.findByFoodIdAndDeletedFalse(foodId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FOOD-4040", "식품을 찾을 수 없습니다."));
        foodItem.markDeleted();
    }

    @Transactional(readOnly = true)
    public Map<Long, FoodItem> getFoodMapByIds(List<Long> foodIds) {
        return foodItemRepository.findAllById(foodIds).stream()
                .filter(food -> !food.isDeleted())
                .collect(Collectors.toMap(FoodItem::getFoodId, f -> f));
    }

    @Transactional(readOnly = true)
    public List<FoodCategoryResponse> getCategories() {
        return foodItemRepository.countByCategory().stream()
                .map(row -> new FoodCategoryResponse(
                        row[0] == null ? "기타" : row[0].toString(),
                        row[1] == null ? 0L : ((Number) row[1]).longValue()
                ))
                .toList();
    }

    private FoodResponse toResponse(FoodItem foodItem) {
        List<FoodAdditiveMap> mappings = foodAdditiveMapRepository.findByFoodFoodIdAndDeletedFalse(foodItem.getFoodId());
        List<Long> additiveIds = mappings.stream().map(map -> map.getAdditive().getAdditiveId()).toList();
        List<String> warningLabels = mappings.stream()
                .filter(map -> map.getAdditive().getDangerLevel() != null && map.getAdditive().getDangerLevel() >= 4)
                .map(map -> map.getAdditive().getName())
                .distinct()
                .toList();

        return new FoodResponse(
                foodItem.getFoodId(),
                foodItem.getName(),
                foodItem.getCategory(),
                foodItem.getManufacturer(),
                foodItem.getImageUrl(),
                foodItem.getCalories(),
                foodItem.getCarbs(),
                foodItem.getProtein(),
                foodItem.getFat(),
                foodItem.getSugars(),
                foodItem.getSodium(),
                additiveIds,
                warningLabels
        );
    }

    private void replaceAdditives(FoodItem foodItem, List<Long> additiveIds) {
        List<FoodAdditiveMap> existing = foodAdditiveMapRepository.findByFoodFoodId(foodItem.getFoodId());
        Map<Long, FoodAdditiveMap> existingByAdditiveId = new HashMap<>();
        for (FoodAdditiveMap map : existing) {
            existingByAdditiveId.put(map.getAdditive().getAdditiveId(), map);
        }

        List<Long> targetIds = additiveIds == null ? List.of() : additiveIds;
        Set<Long> targetIdSet = targetIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        for (FoodAdditiveMap map : existing) {
            Long additiveId = map.getAdditive().getAdditiveId();
            if (targetIdSet.contains(additiveId)) {
                map.restore();
            } else {
                map.markDeleted();
            }
        }

        if (targetIds.isEmpty()) {
            return;
        }

        List<Additive> additives = additiveRepository.findAllByAdditiveIdIn(targetIds);
        Map<Long, Additive> additiveMap = new HashMap<>();
        additives.forEach(additive -> additiveMap.put(additive.getAdditiveId(), additive));

        List<FoodAdditiveMap> toSave = new ArrayList<>();
        for (Long additiveId : targetIds) {
            Additive additive = additiveMap.get(additiveId);
            if (additive == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "FOOD-4001", "존재하지 않는 첨가물 ID가 포함되어 있습니다.");
            }
            if (existingByAdditiveId.containsKey(additiveId)) {
                continue;
            }
            toSave.add(FoodAdditiveMap.create(foodItem, additive));
        }

        if (!toSave.isEmpty()) {
            foodAdditiveMapRepository.saveAll(toSave);
        }
    }

    private List<RecommendationSlot> buildRecommendationSlots(String goal) {
        if ("FAT_LOSS".equals(goal)) {
            return List.of(
                    new RecommendationSlot(
                            "BREAKFAST",
                            "저열량·고단백 중심 아침 추천",
                            List.of("건강식", "유제품", "일식"),
                            BigDecimal.valueOf(80),
                            BigDecimal.valueOf(420),
                            List.of("라면", "버거", "피자", "튀김", "곱창", "삼겹살"),
                            List.of("요거트", "오트밀", "샐러드", "닭가슴살", "두부", "계란", "포케"),
                            Sort.Direction.ASC
                    ),
                    new RecommendationSlot(
                            "LUNCH",
                            "목표 칼로리 범위 내 점심 추천",
                            List.of("건강식", "일식", "한식", "간편식"),
                            BigDecimal.valueOf(250),
                            BigDecimal.valueOf(650),
                            List.of("라면", "버거", "피자", "튀김", "곱창", "삼겹살", "떡볶이"),
                            List.of("샐러드", "현미", "연어", "닭가슴살", "두부", "회", "포케"),
                            Sort.Direction.ASC
                    ),
                    new RecommendationSlot(
                            "DINNER",
                            "과식 방지를 위한 저녁 추천",
                            List.of("건강식", "일식", "한식"),
                            BigDecimal.valueOf(220),
                            BigDecimal.valueOf(580),
                            List.of("라면", "버거", "피자", "튀김", "곱창", "삼겹살", "찌개"),
                            List.of("샐러드", "연어", "닭가슴살", "두부", "포케", "회"),
                            Sort.Direction.ASC
                    ),
                    new RecommendationSlot(
                            "SNACK",
                            "폭식 리스크를 낮춘 간식 추천",
                            List.of("간식", "유제품", "건강식"),
                            BigDecimal.valueOf(60),
                            BigDecimal.valueOf(260),
                            List.of("콜라", "사이다", "아이스크림", "도넛", "마카롱"),
                            List.of("요거트", "프로틴", "고구마", "견과"),
                            Sort.Direction.ASC
                    )
            );
        }

        if ("BULK_UP".equals(goal)) {
            return List.of(
                    new RecommendationSlot(
                            "BREAKFAST",
                            "탄수화물·단백질 확보 아침 추천",
                            List.of("한식", "일식", "간편식"),
                            BigDecimal.valueOf(320),
                            BigDecimal.valueOf(900),
                            List.of("제로콜라", "탄산수"),
                            List.of("비빔밥", "덮밥", "샌드위치", "오트밀", "달걀"),
                            Sort.Direction.DESC
                    ),
                    new RecommendationSlot(
                            "LUNCH",
                            "운동량 반영 고에너지 점심 추천",
                            List.of("한식", "중식", "일식", "간편식"),
                            BigDecimal.valueOf(450),
                            BigDecimal.valueOf(1100),
                            List.of("아메리카노"),
                            List.of("불고기", "제육", "규동", "카레", "치킨"),
                            Sort.Direction.DESC
                    ),
                    new RecommendationSlot(
                            "DINNER",
                            "회복을 위한 단백질 중심 저녁 추천",
                            List.of("한식", "건강식", "일식"),
                            BigDecimal.valueOf(380),
                            BigDecimal.valueOf(950),
                            List.of("콜라", "사이다"),
                            List.of("연어", "닭가슴살", "불고기", "회", "스테이크"),
                            Sort.Direction.DESC
                    ),
                    new RecommendationSlot(
                            "SNACK",
                            "근손실 방지를 위한 보충 간식 추천",
                            List.of("간식", "유제품", "건강식", "디저트"),
                            BigDecimal.valueOf(150),
                            BigDecimal.valueOf(420),
                            List.of("탄산수"),
                            List.of("프로틴", "요거트", "우유", "바", "바나나"),
                            Sort.Direction.DESC
                    )
            );
        }

        return List.of(
                new RecommendationSlot(
                        "BREAKFAST",
                        "균형 아침 추천",
                        List.of("건강식", "한식", "유제품"),
                        BigDecimal.valueOf(120),
                        BigDecimal.valueOf(550),
                        List.of("에너지드링크"),
                        List.of("요거트", "샐러드", "오트밀", "계란"),
                        Sort.Direction.ASC
                ),
                new RecommendationSlot(
                        "LUNCH",
                        "균형 점심 추천",
                        List.of("한식", "일식", "중식", "건강식"),
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(800),
                        List.of("콜라"),
                        List.of("비빔밥", "덮밥", "연어", "닭가슴살", "두부"),
                        Sort.Direction.ASC
                ),
                new RecommendationSlot(
                        "DINNER",
                        "균형 저녁 추천",
                        List.of("한식", "건강식", "일식"),
                        BigDecimal.valueOf(260),
                        BigDecimal.valueOf(700),
                        List.of("에너지드링크"),
                        List.of("연어", "샐러드", "닭가슴살", "회"),
                        Sort.Direction.ASC
                ),
                new RecommendationSlot(
                        "SNACK",
                        "균형 간식 추천",
                        List.of("간식", "유제품", "디저트"),
                        BigDecimal.valueOf(80),
                        BigDecimal.valueOf(300),
                        List.of("콜라", "사이다"),
                        List.of("요거트", "프로틴", "견과", "고구마"),
                        Sort.Direction.ASC
                )
        );
    }

    private FoodResponse pickRecommendation(
            RecommendationSlot slot,
            String goal,
            String strictness,
            Set<Long> usedFoodIds,
            long seed
    ) {
        List<FoodItem> categoryCandidates = loadCandidates(slot.categories(), slot.sortDirection(), 36);
        List<FoodItem> filtered = filterCandidates(categoryCandidates, slot, strictness, usedFoodIds);
        if (filtered.isEmpty()) {
            filtered = filterCandidates(categoryCandidates, slot.withRelaxedRules(), strictness, usedFoodIds);
        }
        if (filtered.isEmpty()) {
            List<FoodItem> fallback = loadCandidates(List.of(), Sort.Direction.ASC, 60);
            filtered = filterCandidates(fallback, slot.withRelaxedRules(), strictness, usedFoodIds);
        }
        if (filtered.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FOOD-4041", "추천 가능한 식품 데이터가 없습니다.");
        }

        List<FoodItem> ranked = rankCandidates(filtered, slot, goal);
        int bucketSize = Math.max(1, Math.min(ranked.size(), 6));
        Random random = new Random(seed ^ slot.mealType().hashCode() ^ usedFoodIds.hashCode());
        FoodItem picked = ranked.get(random.nextInt(bucketSize));
        return toResponse(picked);
    }

    private List<FoodItem> loadCandidates(List<String> categories, Sort.Direction direction, int size) {
        List<String> safeCategories = categories == null ? List.of() : categories;
        Map<Long, FoodItem> merged = new LinkedHashMap<>();

        if (!safeCategories.isEmpty()) {
            for (String category : safeCategories) {
                List<FoodItem> rows = foodItemRepository.search(
                        "",
                        category == null ? "" : category.trim(),
                        PageRequest.of(0, size, Sort.by(direction, "calories"))
                ).getContent();
                for (FoodItem row : rows) {
                    merged.putIfAbsent(row.getFoodId(), row);
                }
            }
        } else {
            List<FoodItem> rows = foodItemRepository.search(
                    "",
                    "",
                    PageRequest.of(0, size, Sort.by(direction, "calories"))
            ).getContent();
            for (FoodItem row : rows) {
                merged.putIfAbsent(row.getFoodId(), row);
            }
        }

        return new ArrayList<>(merged.values());
    }

    private List<FoodItem> filterCandidates(
            List<FoodItem> candidates,
            RecommendationSlot slot,
            String strictness,
            Set<Long> usedFoodIds
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> maxDangerByFoodId = findMaxDangerLevels(candidates);
        List<FoodItem> filtered = new ArrayList<>();
        for (FoodItem item : candidates) {
            if (item == null || item.getFoodId() == null || usedFoodIds.contains(item.getFoodId())) {
                continue;
            }

            BigDecimal calories = item.getCalories() == null ? BigDecimal.ZERO : item.getCalories();
            if (slot.minCalories() != null && calories.compareTo(slot.minCalories()) < 0) {
                continue;
            }
            if (slot.maxCalories() != null && calories.compareTo(slot.maxCalories()) > 0) {
                continue;
            }

            String name = item.getName() == null ? "" : item.getName();
            if (containsAny(name, slot.avoidKeywords())) {
                continue;
            }

            Integer dangerLevel = maxDangerByFoodId.getOrDefault(item.getFoodId(), 0);
            if ("HIGH".equals(strictness) && dangerLevel >= 4) {
                continue;
            }
            if ("MEDIUM".equals(strictness) && dangerLevel >= 5) {
                continue;
            }

            filtered.add(item);
        }
        return filtered;
    }

    private List<FoodItem> rankCandidates(List<FoodItem> candidates, RecommendationSlot slot, String goal) {
        List<FoodItem> sorted = new ArrayList<>(candidates);
        sorted.sort((a, b) -> {
            int scoreA = preferenceScore(a, slot, goal);
            int scoreB = preferenceScore(b, slot, goal);
            if (scoreA != scoreB) {
                return Integer.compare(scoreB, scoreA);
            }

            BigDecimal caloriesA = a.getCalories() == null ? BigDecimal.ZERO : a.getCalories();
            BigDecimal caloriesB = b.getCalories() == null ? BigDecimal.ZERO : b.getCalories();
            if ("BULK_UP".equals(goal)) {
                return caloriesB.compareTo(caloriesA);
            }
            return caloriesA.compareTo(caloriesB);
        });
        return sorted;
    }

    private int preferenceScore(FoodItem item, RecommendationSlot slot, String goal) {
        int score = 0;
        String name = item.getName() == null ? "" : item.getName();

        if (containsAny(name, slot.preferKeywords())) {
            score += 20;
        }
        if (containsAny(name, slot.avoidKeywords())) {
            score -= 100;
        }

        BigDecimal calories = item.getCalories() == null ? BigDecimal.ZERO : item.getCalories();
        if ("BULK_UP".equals(goal)) {
            if (calories.compareTo(BigDecimal.valueOf(500)) >= 0) {
                score += 8;
            }
        } else if ("FAT_LOSS".equals(goal)) {
            if (calories.compareTo(BigDecimal.valueOf(500)) <= 0) {
                score += 10;
            }
        } else {
            if (calories.compareTo(BigDecimal.valueOf(700)) <= 0) {
                score += 6;
            }
        }

        return score;
    }

    private boolean containsAny(String target, List<String> keywords) {
        if (target == null || target.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String normalized = target.trim();
        return keywords.stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(normalized::contains);
    }

    private Map<Long, Integer> findMaxDangerLevels(List<FoodItem> candidates) {
        List<Long> foodIds = candidates.stream()
                .map(FoodItem::getFoodId)
                .filter(id -> id != null)
                .toList();
        if (foodIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> result = new HashMap<>();
        for (FoodAdditiveMap map : foodAdditiveMapRepository.findByFoodFoodIdInAndDeletedFalse(foodIds)) {
            if (map.getFood() == null || map.getFood().getFoodId() == null || map.getAdditive() == null) {
                continue;
            }
            Integer level = map.getAdditive().getDangerLevel();
            if (level == null) {
                continue;
            }
            result.merge(map.getFood().getFoodId(), level, Integer::max);
        }
        return result;
    }

    private String normalizeGoal(String goal) {
        if (goal == null || goal.isBlank()) {
            return "MAINTAIN";
        }
        String normalized = goal.trim().toUpperCase();
        if (!Set.of("MAINTAIN", "FAT_LOSS", "BULK_UP").contains(normalized)) {
            return "MAINTAIN";
        }
        return normalized;
    }

    private String normalizeStrictness(String strictness) {
        if (strictness == null || strictness.isBlank()) {
            return "MEDIUM";
        }
        String normalized = strictness.trim().toUpperCase();
        if (!Set.of("LOW", "MEDIUM", "HIGH").contains(normalized)) {
            return "MEDIUM";
        }
        return normalized;
    }

    private long resolveSeed(String seed) {
        if (seed == null || seed.isBlank()) {
            return System.currentTimeMillis();
        }
        try {
            return Long.parseLong(seed.trim());
        } catch (NumberFormatException ignored) {
            return Math.abs((long) seed.hashCode());
        }
    }

    private record RecommendationSlot(
            String mealType,
            String reason,
            List<String> categories,
            BigDecimal minCalories,
            BigDecimal maxCalories,
            List<String> avoidKeywords,
            List<String> preferKeywords,
            Sort.Direction sortDirection
    ) {
        private RecommendationSlot withRelaxedRules() {
            return new RecommendationSlot(
                    mealType,
                    reason,
                    categories,
                    minCalories == null ? null : minCalories.multiply(BigDecimal.valueOf(0.65)),
                    maxCalories == null ? null : maxCalories.multiply(BigDecimal.valueOf(1.45)),
                    List.of(),
                    preferKeywords,
                    sortDirection
            );
        }
    }
}
