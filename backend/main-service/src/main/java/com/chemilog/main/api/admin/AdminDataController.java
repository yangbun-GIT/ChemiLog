package com.chemilog.main.api.admin;

import com.chemilog.main.api.common.ApiResponse;
import com.chemilog.main.api.common.PagedData;
import com.chemilog.main.api.food.FoodResponse;
import com.chemilog.main.service.AdminDataService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/data")
public class AdminDataController {

    private final AdminDataService adminDataService;

    public AdminDataController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedData<AdminUserRowResponse>>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminDataService.users(keyword, status, role, page, size)));
    }

    @GetMapping("/foods")
    public ResponseEntity<ApiResponse<PagedData<FoodResponse>>> foods(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminDataService.foods(keyword, category, page, size)));
    }
}
