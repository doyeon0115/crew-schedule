package com.crewschedule.auth.service;

import com.crewschedule.auth.dto.AuthDtos.TokenResponse;
import com.crewschedule.auth.oauth.OAuthClient;
import com.crewschedule.auth.oauth.OAuthUserInfo;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.user.domain.AuthProvider;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.domain.UserRole;
import com.crewschedule.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** OAuth 인가코드를 받아 provider 유저 정보를 조회하고 우리 DB에 매칭·생성 후 JWT를 발급. */
@Slf4j
@Service
public class OAuthLoginService {

    private final Map<AuthProvider, OAuthClient> clients;
    private final UserRepository userRepository;
    private final AuthService authService;

    public OAuthLoginService(
            List<OAuthClient> clients,
            UserRepository userRepository,
            AuthService authService) {
        this.clients = clients.stream()
                .collect(Collectors.toMap(OAuthClient::provider, c -> c));
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional
    public TokenResponse login(AuthProvider provider, String authorizationCode) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "지원하지 않는 provider입니다.");
        }
        OAuthUserInfo info = client.fetchUser(authorizationCode);
        User user = userRepository.findByProviderAndProviderId(info.provider(), info.providerId())
                .orElseGet(() -> createSocialUser(info));
        return authService.issueTokens(user);
    }

    private User createSocialUser(OAuthUserInfo info) {
        // provider가 이메일을 안 줄 수도 있고, 같은 이메일이 이미 LOCAL로 있을 수도 있어서
        // 이메일 자체를 유일 키로 쓰기 어렵다. 소셜 유저는 provider-scoped placeholder 이메일로 저장.
        String email = uniqueEmail(info);
        return userRepository.save(User.builder()
                .email(email)
                .password(null)
                .nickname(info.nickname())
                .profileImageUrl(info.profileImageUrl())
                .role(UserRole.USER)
                .provider(info.provider())
                .providerId(info.providerId())
                .build());
    }

    private String uniqueEmail(OAuthUserInfo info) {
        if (info.email() != null && !info.email().isBlank()
                && !userRepository.existsByEmail(info.email())) {
            return info.email();
        }
        // 이메일 없음(카카오 미동의) 혹은 기존 유저 이메일과 충돌 → provider-scoped placeholder
        return info.provider().name().toLowerCase()
                + "_" + info.providerId()
                + "_" + UUID.randomUUID().toString().substring(0, 8)
                + "@social.local";
    }
}
