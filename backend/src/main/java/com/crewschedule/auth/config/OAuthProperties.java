package com.crewschedule.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.oauth.{provider}.*} 설정 바인딩. 값이 비어 있으면 해당 provider는 비활성화된다. */
@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(ProviderConfig kakao, ProviderConfig google) {

    public record ProviderConfig(String clientId, String clientSecret, String redirectUri) {
        public boolean isEnabled() {
            return clientId != null && !clientId.isBlank();
        }
    }
}
