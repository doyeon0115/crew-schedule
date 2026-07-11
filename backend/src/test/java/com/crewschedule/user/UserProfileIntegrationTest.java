package com.crewschedule.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crewschedule.auth.dto.AuthDtos.SignupRequest;
import com.crewschedule.auth.dto.AuthDtos.TokenResponse;
import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.user.dto.UserDtos.UpdateProfileRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class UserProfileIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    @DisplayName("토큰 없으면 /me는 401, 토큰 있으면 프로필을 반환하고 PATCH로 닉네임을 바꿀 수 있다")
    void meRequiresAuthAndAllowsUpdate() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String access = signupAndGetAccessToken("me@test.local", "지영");

        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("me@test.local"))
                .andExpect(jsonPath("$.data.nickname").value("지영"))
                .andExpect(jsonPath("$.data.provider").value("LOCAL"));

        UpdateProfileRequest patch = new UpdateProfileRequest("지영2", "https://cdn/img.png");
        mvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("지영2"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn/img.png"));
    }

    @Test
    @DisplayName("잘못된 토큰이면 401")
    void invalidTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    private String signupAndGetAccessToken(String email, String nickname) throws Exception {
        String body = mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new SignupRequest(email, "password12", nickname))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ApiResponse<TokenResponse> parsed = om.readValue(body, new TypeReference<>() {});
        return parsed.data().accessToken();
    }
}
