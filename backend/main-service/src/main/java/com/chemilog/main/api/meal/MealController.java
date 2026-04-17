package com.chemilog.main.api.meal;

import com.chemilog.main.api.common.ApiResponse;
import com.chemilog.main.security.AuthUser;
import com.chemilog.main.service.MealService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meals")
public class MealController {

    private final MealService mealService;

    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<MealTodayResponse>> today(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.ok(mealService.getToday(authUser.userId(), date)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<MealHistoryResponse>> history(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(mealService.getHistory(authUser.userId(), from, to)));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<MealSyncResponse>> sync(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody MealSyncRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(mealService.sync(authUser.userId(), request, idempotencyKey)));
    }
}
