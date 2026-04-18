package com.chemilog.main.api.admin;

import com.chemilog.main.api.common.ApiResponse;
import com.chemilog.main.api.common.PagedData;
import com.chemilog.main.api.food.FoodResponse;
import com.chemilog.main.service.AdminDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

    @GetMapping("/additives")
    public ResponseEntity<ApiResponse<PagedData<AdminAdditiveRowResponse>>> additives(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminDataService.additives(keyword, page, size)));
    }

    @GetMapping("/violation-logs")
    public ResponseEntity<ApiResponse<PagedData<AdminViolationLogRowResponse>>> violationLogs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminDataService.violationLogs(category, keyword, page, size)));
    }

    @GetMapping("/hallucination-logs")
    public ResponseEntity<ApiResponse<PagedData<AdminHallucinationLogRowResponse>>> hallucinationLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminDataService.hallucinationLogs(keyword, page, size)));
    }

    @PostMapping("/additives")
    public ResponseEntity<ApiResponse<AdminAdditiveRowResponse>> createAdditive(
            @Valid @RequestBody AdminAdditiveUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminDataService.createAdditive(request)));
    }

    @PatchMapping("/additives/{additiveId}")
    public ResponseEntity<ApiResponse<AdminAdditiveRowResponse>> updateAdditive(
            @PathVariable Long additiveId,
            @Valid @RequestBody AdminAdditiveUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminDataService.updateAdditive(additiveId, request)));
    }

    @GetMapping("/search-miss-logs")
    public ResponseEntity<ApiResponse<PagedData<AdminSearchMissRowResponse>>> searchMissLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminDataService.searchMissLogs(keyword, resolved, page, size)));
    }

    @PatchMapping("/search-miss-logs/{missId}")
    public ResponseEntity<ApiResponse<Void>> resolveSearchMiss(
            @PathVariable Long missId,
            @RequestParam(defaultValue = "true") boolean resolved
    ) {
        adminDataService.resolveSearchMiss(missId, resolved);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
