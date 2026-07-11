package com.crewschedule.auth.controller;

import com.crewschedule.auth.dto.AuthDtos.TokenResponse;
import com.crewschedule.auth.service.OAuthLoginService;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.user.domain.AuthProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 소셜 로그인 — 프론트에서 받은 인가코드를 우리 JWT로 변환. */
@Tag(name = "Auth - OAuth", description = "소셜 로그인 (카카오·구글)")
@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthLoginService oauthLoginService;

    @PostMapping("/{provider}")
    public ApiResponse<TokenResponse> login(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request) {
        AuthProvider parsed = parseProvider(provider);
        return ApiResponse.ok(oauthLoginService.login(parsed, request.code()));
    }

    private AuthProvider parseProvider(String raw) {
        try {
            AuthProvider provider = AuthProvider.valueOf(raw.toUpperCase());
            if (provider == AuthProvider.LOCAL) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "LOCAL은 소셜 provider가 아닙니다.");
            }
            return provider;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 provider: " + raw);
        }
    }

    public record OAuthLoginRequest(@NotBlank String code) {}
}
