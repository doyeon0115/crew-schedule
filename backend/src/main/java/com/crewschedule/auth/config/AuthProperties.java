package com.crewschedule.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.jwt.*} 설정 바인딩. secret은 최소 32바이트 이상을 권장. */
@ConfigurationProperties(prefix = "app.jwt")
public record AuthProperties(
        String secret,
        long accessTokenValiditySeconds,
        long refreshTokenValiditySeconds) {
}
