package com.chemilog.main.api.internal;

public record HallucinationLogCreateRequest(
        String modelVersion,
        String promptContext,
        String generatedResponse,
        String failedReason
) {
}
