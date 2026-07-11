package com.crewschedule.auth.dto;

import com.crewschedule.user.domain.AuthProvider;
import com.crewschedule.user.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 인증 관련 요청/응답 DTO 모음. */
public final class AuthDtos {

    private AuthDtos() {}

    public record SignupRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(min = 2, max = 50) String nickname) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long accessExpiresInSeconds,
            UserSummary user) {}

    public record UserSummary(
            Long id,
            String email,
            String nickname,
            String profileImageUrl,
            UserRole role,
            AuthProvider provider) {}
}
