package com.crewschedule.auth.jwt;

import com.crewschedule.auth.config.AuthProperties;
import com.crewschedule.auth.security.AuthPrincipal;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** access/refresh JWT 발급·파싱. HS256, sub=userId, role=UserRole, type=access|refresh. */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessValidity;
    private final Duration refreshValidity;

    public JwtTokenProvider(AuthProperties props) {
        byte[] secretBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.accessValidity = Duration.ofSeconds(props.accessTokenValiditySeconds());
        this.refreshValidity = Duration.ofSeconds(props.refreshTokenValiditySeconds());
    }

    public String createAccessToken(Long userId, UserRole role) {
        return build(userId, role, TYPE_ACCESS, accessValidity);
    }

    public String createRefreshToken(Long userId, UserRole role) {
        return build(userId, role, TYPE_REFRESH, refreshValidity);
    }

    public Duration accessValidity() {
        return accessValidity;
    }

    public Duration refreshValidity() {
        return refreshValidity;
    }

    /** access 토큰 파싱. 타입/서명/만료 검증 후 principal 반환. */
    public AuthPrincipal parseAccess(String token) {
        Claims claims = parse(token, TYPE_ACCESS);
        return new AuthPrincipal(Long.parseLong(claims.getSubject()), UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }

    public AuthPrincipal parseRefresh(String token) {
        Claims claims = parse(token, TYPE_REFRESH);
        return new AuthPrincipal(Long.parseLong(claims.getSubject()), UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }

    private String build(Long userId, UserRole role, String type, Duration validity) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    private Claims parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String type = claims.get(CLAIM_TYPE, String.class);
            if (!expectedType.equals(type)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "잘못된 토큰 타입");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰");
        }
    }
}
