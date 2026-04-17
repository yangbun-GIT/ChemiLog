package com.chemilog.main.service;

import com.chemilog.main.api.common.PageInfo;
import com.chemilog.main.api.common.PagedData;
import com.chemilog.main.api.food.FoodCategoryResponse;
import com.chemilog.main.api.food.FoodResponse;
import com.chemilog.main.api.food.FoodUpsertRequest;
import com.chemilog.main.domain.food.Additive;
import com.chemilog.main.domain.food.FoodAdditiveMap;
import com.chemilog.main.domain.food.FoodItem;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.AdditiveRepository;
import com.chemilog.main.repository.FoodAdditiveMapRepository;
import com.chemilog.main.repository.FoodItemRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodService {

    private final FoodItemRepository foodItemRepository;
    private final AdditiveRepository additiveRepository;
    private final FoodAdditiveMapRepository foodAdditiveMapRepository;

    public FoodService(
            FoodItemRepository foodItemRepository,
            AdditiveRepository additiveRepository,
            FoodAdditiveMapRepository foodAdditiveMapRepository
    ) {
        this.foodItemRepository = foodItemRepository;
        this.additiveRepository = additiveRepository;
        this.foodAdditiveMapRepository = foodAdditiveMapRepository;
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

        return new PagedData<>(
                items,
                new PageInfo(foods.getNumber(), foods.getTotalPages(), foods.getTotalElements(), foods.hasNext())
        );
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> popular(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 30));
        return foodItemRepository.findPopularFoods(PageRequest.of(0, normalizedLimit)).stream()
                .map(this::toResponse)
                .toList();
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
                .filter(map -> map.getAdditive().getDangerLevel() >= 4)
                .map(map -> "주의: " + map.getAdditive().getName())
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
}
