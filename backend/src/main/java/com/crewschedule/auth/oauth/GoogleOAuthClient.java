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
 * 구글 로그인. 인가코드 → access_token → userinfo(email·name·picture·sub) 흐름.
 * 문서: <a href="https://developers.google.com/identity/protocols/oauth2/web-server">OAuth2 Web Server</a>.
 */
@Component
public class GoogleOAuthClient implements OAuthClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient rest;
    private final ProviderConfig config;

    public GoogleOAuthClient(RestClient oauthRestClient, OAuthProperties props) {
        this.rest = oauthRestClient;
        this.config = props.google();
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo fetchUser(String authorizationCode) {
        if (!config.isEnabled()) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "구글 로그인이 활성화되지 않았습니다.");
        }
        String accessToken = exchangeToken(authorizationCode);
        GoogleUserResponse user = getUser(accessToken);
        if (user == null || user.sub == null) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "구글 유저 정보를 받지 못했습니다.");
        }
        String nickname = user.name != null ? user.name : (user.email != null ? user.email.split("@")[0] : "구글 유저");
        return new OAuthUserInfo(AuthProvider.GOOGLE, user.sub, user.email, nickname, user.picture);
    }

    private String exchangeToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", config.clientId());
        form.add("client_secret", config.clientSecret());
        form.add("redirect_uri", config.redirectUri());
        form.add("code", code);

        TokenResponse token = rest.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (token == null || token.accessToken == null) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "구글 토큰 교환 실패");
        }
        return token.accessToken;
    }

    private GoogleUserResponse getUser(String accessToken) {
        return rest.get()
                .uri(USER_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserResponse.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoogleUserResponse {
        public String sub;
        public String email;
        public String name;
        public String picture;
    }
}
