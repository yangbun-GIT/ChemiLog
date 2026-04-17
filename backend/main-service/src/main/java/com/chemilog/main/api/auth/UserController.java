package com.chemilog.main.api.auth;

import com.chemilog.main.api.common.ApiResponse;
import com.chemilog.main.security.AuthUser;
import com.chemilog.main.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> me(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMe(authUser.userId())));
    }

    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<Void>> onboarding(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UserProfileRequest request
    ) {
        userService.updateProfile(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UserProfileRequest request
    ) {
        userService.updateProfile(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
