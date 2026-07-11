package com.crewschedule.auth.service;

import com.crewschedule.auth.domain.RefreshToken;
import com.crewschedule.auth.dto.AuthDtos.LoginRequest;
import com.crewschedule.auth.dto.AuthDtos.SignupRequest;
import com.crewschedule.auth.dto.AuthDtos.TokenResponse;
import com.crewschedule.auth.dto.AuthDtos.UserSummary;
import com.crewschedule.auth.jwt.JwtTokenProvider;
import com.crewschedule.auth.repository.RefreshTokenRepository;
import com.crewschedule.auth.security.AuthPrincipal;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.user.domain.AuthProvider;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.domain.UserRole;
import com.crewschedule.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원가입·이메일 로그인·토큰 재발급·로그아웃. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenHasher hasher;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = userRepository.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .build());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (user.isSocial() || user.getPassword() == null) {
            throw new BusinessException(ErrorCode.SOCIAL_USER_PASSWORD_LOGIN);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        AuthPrincipal principal;
        try {
            principal = tokenProvider.parseRefresh(rawRefreshToken);
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String hash = hasher.hash(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (stored.isExpired(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        // rotation: 기존 토큰을 폐기하고 새로 발급
        refreshTokenRepository.delete(stored);
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.deleteByTokenHash(hasher.hash(rawRefreshToken));
    }

    /** 소셜 로그인 콜백에서도 재사용. */
    @Transactional
    public TokenResponse issueTokens(User user) {
        String access = tokenProvider.createAccessToken(user.getId(), user.getRole());
        String refresh = tokenProvider.createRefreshToken(user.getId(), user.getRole());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hasher.hash(refresh))
                .expiresAt(LocalDateTime.now().plus(tokenProvider.refreshValidity()))
                .build());
        return new TokenResponse(
                access,
                refresh,
                tokenProvider.accessValidity().toSeconds(),
                new UserSummary(
                        user.getId(),
                        user.getEmail(),
                        user.getNickname(),
                        user.getProfileImageUrl(),
                        user.getRole(),
                        user.getProvider()));
    }
}
