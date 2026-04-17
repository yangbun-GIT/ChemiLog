package com.chemilog.main.api.food;

import com.chemilog.main.api.common.ApiResponse;
import com.chemilog.main.api.common.PagedData;
import com.chemilog.main.service.FoodService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/foods")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedData<FoodResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(foodService.search(keyword, category, page, size)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<FoodCategoryResponse>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok(foodService.getCategories()));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<FoodResponse>>> popular(
            @RequestParam(defaultValue = "6") @Min(1) @Max(30) int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(foodService.popular(limit)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<FoodResponse>> create(@Valid @RequestBody FoodUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(foodService.create(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{foodId}")
    public ResponseEntity<ApiResponse<FoodResponse>> update(
            @PathVariable Long foodId,
            @Valid @RequestBody FoodUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(foodService.update(foodId, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{foodId}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long foodId) {
        foodService.softDelete(foodId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
