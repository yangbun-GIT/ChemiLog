package com.chemilog.main.exception;

import com.chemilog.main.api.common.ApiErrorField;
import com.chemilog.main.api.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.fail(e.getCode(), e.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<ApiErrorField> errors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorField)
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("COMMON-4000", "요청 값이 유효하지 않습니다.", errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBind(BindException e) {
        List<ApiErrorField> errors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorField)
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("COMMON-4001", "바인딩 중 오류가 발생했습니다.", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        List<ApiErrorField> errors = e.getConstraintViolations().stream()
                .map(v -> new ApiErrorField(v.getPropertyPath().toString(), v.getInvalidValue(), v.getMessage()))
                .collect(Collectors.toList());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("COMMON-4002", "검증 오류가 발생했습니다.", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("COMMON-5000", "서버 내부 오류가 발생했습니다.", List.of()));
    }

    private ApiErrorField toErrorField(FieldError error) {
        return new ApiErrorField(
                error.getField(),
                error.getRejectedValue(),
                error.getDefaultMessage()
        );
    }
}
