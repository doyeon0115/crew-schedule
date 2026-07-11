package com.crewschedule.user.controller;

import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import com.crewschedule.user.dto.UserDtos.ProfileResponse;
import com.crewschedule.user.dto.UserDtos.UpdateProfileRequest;
import com.crewschedule.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "내 프로필")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> me(@CurrentUserId Long userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PatchMapping("/me")
    public ApiResponse<ProfileResponse> updateMe(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(userId, request));
    }
}
