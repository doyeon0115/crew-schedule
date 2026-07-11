package com.crewschedule.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crewschedule.auth.dto.AuthDtos.LoginRequest;
import com.crewschedule.auth.dto.AuthDtos.LogoutRequest;
import com.crewschedule.auth.dto.AuthDtos.RefreshRequest;
import com.crewschedule.auth.dto.AuthDtos.SignupRequest;
import com.crewschedule.auth.dto.AuthDtos.TokenResponse;
import com.crewschedule.auth.repository.RefreshTokenRepository;
import com.crewschedule.auth.service.RefreshTokenHasher;
import com.crewschedule.common.web.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired RefreshTokenHasher hasher;

    @Test
    @DisplayName("회원가입 → 로그인 → 리프레시 → 로그아웃 전체 플로우")
    void signupLoginRefreshLogout() throws Exception {
        // 1. 회원가입 → 201 + 토큰
        TokenResponse signupTokens = extract(mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new SignupRequest("jiyoung@test.local", "password12", "지영"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.email").value("jiyoung@test.local"))
                .andReturn());
        assertThat(signupTokens.accessExpiresInSeconds()).isPositive();

        // 2. 이메일 로그인 → 200
        TokenResponse loginTokens = extract(mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new LoginRequest("jiyoung@test.local", "password12"))))
                .andExpect(status().isOk())
                .andReturn());

        // 3. 리프레시 → 새 토큰, 기존 리프레시는 폐기(회전)
        TokenResponse refreshed = extract(mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new RefreshRequest(loginTokens.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refreshed.accessToken()).isNotEqualTo(loginTokens.accessToken());

        // 기존 리프레시로 다시 요청하면 실패해야 함(rotation)
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new RefreshRequest(loginTokens.refreshToken()))))
                .andExpect(status().isUnauthorized());

        // 4. 로그아웃 → 새 리프레시도 폐기
        mvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new LogoutRequest(refreshed.refreshToken()))))
                .andExpect(status().isOk());

        assertThat(refreshTokenRepository.findByTokenHash(hasher.hash(refreshed.refreshToken()))).isEmpty();
    }

    @Test
    @DisplayName("동일 이메일로 재가입은 409")
    void duplicateEmailIsRejected() throws Exception {
        SignupRequest req = new SignupRequest("dup@test.local", "password12", "중복");
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("잘못된 비밀번호는 401")
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new SignupRequest("wrong@test.local", "password12", "테스트"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new LoginRequest("wrong@test.local", "otherpass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("잘못된 리프레시 토큰은 401")
    void invalidRefreshTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new RefreshRequest("not-a-real-token"))))
                .andExpect(status().isUnauthorized());
    }

    private TokenResponse extract(MvcResult result) throws Exception {
        ApiResponse<TokenResponse> body = om.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});
        return body.data();
    }
}
