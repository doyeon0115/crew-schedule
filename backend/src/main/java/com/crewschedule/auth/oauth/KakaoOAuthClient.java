package com.crewschedule.auth.oauth;

import com.crewschedule.auth.config.OAuthProperties;
import com.crewschedule.auth.config.OAuthProperties.ProviderConfig;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.user.domain.AuthProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 카카오 로그인. 인가코드 → access_token → 사용자 정보(이메일·닉네임·프로필) 흐름.
 * 문서: <a href="https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api">Kakao Login REST API</a>.
 */
@Component
public class KakaoOAuthClient implements OAuthClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient rest;
    private final ProviderConfig config;

    public KakaoOAuthClient(RestClient oauthRestClient, OAuthProperties props) {
        this.rest = oauthRestClient;
        this.config = props.kakao();
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo fetchUser(String authorizationCode) {
        if (!config.isEnabled()) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "카카오 로그인이 활성화되지 않았습니다.");
        }
        String accessToken = exchangeToken(authorizationCode);
        KakaoUserResponse user = getUser(accessToken);

        String email = user.account != null ? user.account.email : null;
        String nickname = user.account != null && user.account.profile != null
                ? user.account.profile.nickname
                : "카카오 유저";
        String image = user.account != null && user.account.profile != null
                ? user.account.profile.profileImageUrl
                : null;
        if (user.id == null) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "카카오 유저 id를 받지 못했습니다.");
        }
        return new OAuthUserInfo(AuthProvider.KAKAO, String.valueOf(user.id), email, nickname, image);
    }

    private String exchangeToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", config.clientId());
        if (config.clientSecret() != null && !config.clientSecret().isBlank()) {
            form.add("client_secret", config.clientSecret());
        }
        form.add("redirect_uri", config.redirectUri());
        form.add("code", code);

        TokenResponse token = rest.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (token == null || token.accessToken == null) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "카카오 토큰 교환 실패");
        }
        return token.accessToken;
    }

    private KakaoUserResponse getUser(String accessToken) {
        KakaoUserResponse user = rest.get()
                .uri(USER_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserResponse.class);
        if (user == null) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "카카오 유저 조회 실패");
        }
        return user;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class KakaoUserResponse {
        public Long id;
        @JsonProperty("kakao_account")
        public Account account;

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Account {
            public String email;
            public Profile profile;

            @JsonIgnoreProperties(ignoreUnknown = true)
            static class Profile {
                public String nickname;
                @JsonProperty("profile_image_url")
                public String profileImageUrl;
            }
        }
    }
}
