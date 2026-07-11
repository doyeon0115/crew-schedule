package com.crewschedule.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crewschedule.auth.dto.AuthDtos.SignupRequest;
import com.crewschedule.auth.dto.AuthDtos.TokenResponse;
import com.crewschedule.chat.dto.ChatDtos.ChatMessageResponse;
import com.crewschedule.chat.dto.ChatDtos.SendMessageRequest;
import com.crewschedule.chat.service.ChatBroadcaster;
import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.crew.dto.CrewDtos.CreateRequest;
import com.crewschedule.crew.dto.CrewDtos.CrewResponse;
import com.crewschedule.crew.dto.CrewDtos.JoinRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Phase 5 다중 인스턴스 브로드캐스팅 검증.
 *
 * <p>REST로 메시지 전송 → DB 저장 + Redis publish → 이 서버가 채널 구독 중이라 STOMP 브로커로 재전송
 * → 실제 STOMP 클라이언트가 /topic/crews/{id}에서 수신.
 * (2대 서버 시나리오도 같은 경로를 사용. 여기서는 1대로 Redis 왕복만 검증.)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ChatBroadcastIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @LocalServerPort int port;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired ChatBroadcaster broadcaster;

    @Test
    @DisplayName("REST 전송 → Redis publish → STOMP 구독자에게 도달")
    void restSendReachesStompSubscriber() throws Exception {
        // 1) 유저 두 명 가입 + 크루 만들기 + 초대코드로 두 번째 유저 가입
        TokenResponse alice = signup("alice@t.local", "alice");
        TokenResponse bob = signup("bob@t.local", "bob");
        CrewResponse crew = createCrew(alice.accessToken(), "우리끼리");
        joinCrew(bob.accessToken(), crew.inviteCode());

        // 2) bob이 STOMP로 연결·구독
        BlockingQueue<ChatMessageResponse> received = new LinkedBlockingQueue<>();
        StompSession session = connect(bob.accessToken());
        session.subscribe("/topic/crews/" + crew.id(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.offer((ChatMessageResponse) payload);
            }
        });
        // STOMP SUBSCRIBE는 async — 프레임이 도착할 시간을 잠깐 줌
        Thread.sleep(200);

        // 3) alice가 REST로 메시지 전송
        mvc.perform(post("/api/crews/" + crew.id() + "/chat/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + alice.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new SendMessageRequest("안녕 bob!"))))
                .andExpect(status().isOk());

        // 4) bob이 5초 안에 그 메시지를 STOMP로 받아야 함
        ChatMessageResponse msg = received.poll(5, TimeUnit.SECONDS);
        assertThat(msg).isNotNull();
        assertThat(msg.content()).isEqualTo("안녕 bob!");
        assertThat(msg.senderNickname()).isEqualTo("alice");
        assertThat(msg.crewId()).isEqualTo(crew.id());
        session.disconnect();
    }

    @Test
    @DisplayName("Broadcaster를 직접 호출해도 Redis 경유로 STOMP 구독자에 도달")
    void directRedisPublishReachesSubscriber() throws Exception {
        TokenResponse alice = signup("a2@t.local", "alice2");
        CrewResponse crew = createCrew(alice.accessToken(), "직접");

        BlockingQueue<ChatMessageResponse> received = new LinkedBlockingQueue<>();
        StompSession session = connect(alice.accessToken());
        session.subscribe("/topic/crews/" + crew.id(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.offer((ChatMessageResponse) payload);
            }
        });
        Thread.sleep(200);

        ChatMessageResponse payload = new ChatMessageResponse(
                999L,
                crew.id(),
                alice.user().id(),
                "system",
                "hello from redis",
                LocalDateTime.now());
        broadcaster.publish(crew.id(), payload);

        ChatMessageResponse msg = received.poll(5, TimeUnit.SECONDS);
        assertThat(msg).isNotNull();
        assertThat(msg.content()).isEqualTo("hello from redis");
        session.disconnect();
    }

    private StompSession connect(String accessToken) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter(new ObjectMapper()
                .findAndRegisterModules()));
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return client
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new org.springframework.web.socket.WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private TokenResponse signup(String email, String nickname) throws Exception {
        String body = mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new SignupRequest(email, "password12", nickname))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.<ApiResponse<TokenResponse>>readValue(body, new TypeReference<>() {}).data();
    }

    private CrewResponse createCrew(String token, String name) throws Exception {
        String body = mvc.perform(post("/api/crews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CreateRequest(name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.<ApiResponse<CrewResponse>>readValue(body, new TypeReference<>() {}).data();
    }

    private void joinCrew(String token, String inviteCode) throws Exception {
        mvc.perform(post("/api/crews/join")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new JoinRequest(inviteCode))))
                .andExpect(status().isOk());
    }

}
