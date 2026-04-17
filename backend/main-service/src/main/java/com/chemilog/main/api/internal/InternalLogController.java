package com.chemilog.main.api.internal;

import com.chemilog.main.api.common.ApiResponse;
import com.chemilog.main.config.properties.InternalProperties;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.service.InternalLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/logs")
public class InternalLogController {

    private final InternalLogService internalLogService;
    private final InternalProperties internalProperties;

    public InternalLogController(
            InternalLogService internalLogService,
            InternalProperties internalProperties
    ) {
        this.internalLogService = internalLogService;
        this.internalProperties = internalProperties;
    }

    @PostMapping("/violation")
    public ResponseEntity<ApiResponse<Void>> createViolationLog(
            @RequestHeader(name = "X-Internal-Secret", required = false) String internalSecret,
            @Valid @RequestBody ViolationLogCreateRequest request
    ) {
        validateInternalSecret(internalSecret);
        internalLogService.createViolationLog(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/hallucination")
    public ResponseEntity<ApiResponse<Void>> createHallucinationLog(
            @RequestHeader(name = "X-Internal-Secret", required = false) String internalSecret,
            @RequestBody HallucinationLogCreateRequest request
    ) {
        validateInternalSecret(internalSecret);
        internalLogService.createHallucinationLog(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void validateInternalSecret(String internalSecret) {
        if (internalSecret == null || !internalSecret.equals(internalProperties.secret())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INTERNAL-4030", "내부 API 접근 권한이 없습니다.");
        }
    }
}
