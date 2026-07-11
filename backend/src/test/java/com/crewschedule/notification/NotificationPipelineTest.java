package com.crewschedule.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.crewschedule.crew.dto.CrewDtos.CrewResponse;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.meetup.dto.MeetupDtos.CreateRequest;
import com.crewschedule.meetup.service.MeetupService;
import com.crewschedule.notification.domain.NotificationType;
import com.crewschedule.notification.dto.NotificationDtos.NotificationPage;
import com.crewschedule.notification.service.NotificationQueryService;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Phase 6A 알림 파이프라인 통합 검증.
 * MeetupService.propose → Kafka publish → NotificationConsumer → DB 저장 흐름.
 * (온라인 STOMP 배달은 별도 테스트에서 커버.)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class NotificationPipelineTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired UserRepository userRepository;
    @Autowired CrewService crewService;
    @Autowired MeetupService meetupService;
    @Autowired NotificationQueryService notificationQuery;

    @Test
    @DisplayName("propose → Kafka → consumer → 초대된 참여자 알림 저장")
    void proposeCreatesNotificationsForOtherMembers() {
        Long jiId = userRepository.save(User.builder().email("j@t.local").nickname("지영").build()).getId();
        Long yuId = userRepository.save(User.builder().email("y@t.local").nickname("유진").build()).getId();
        Long suId = userRepository.save(User.builder().email("s@t.local").nickname("선우").build()).getId();
        CrewResponse crew = crewService.create(jiId, "우리끼리");
        crewService.join(yuId, crew.inviteCode());
        crewService.join(suId, crew.inviteCode());

        meetupService.propose(
                jiId,
                crew.id(),
                new CreateRequest(
                        "목요일 모임",
                        LocalDate.of(2026, 8, 20),
                        LocalTime.of(19, 0),
                        "홍대",
                        null,
                        null,
                        null));

        // Kafka는 비동기. 컨슈머가 이벤트를 처리해 DB에 저장할 때까지 대기.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            NotificationPage yuPage = notificationQuery.list(yuId, null, null);
            NotificationPage suPage = notificationQuery.list(suId, null, null);
            NotificationPage jiPage = notificationQuery.list(jiId, null, null);

            assertThat(yuPage.notifications()).hasSize(1);
            assertThat(yuPage.notifications().get(0).type()).isEqualTo(NotificationType.MEETUP_PROPOSED);
            assertThat(yuPage.unreadCount()).isEqualTo(1);

            assertThat(suPage.notifications()).hasSize(1);
            assertThat(suPage.notifications().get(0).payload()).contains("목요일 모임");

            // 창시자는 자기 자신에게 알림 안 옴
            assertThat(jiPage.notifications()).isEmpty();
        });

        // 읽음 처리
        int updated = notificationQuery.markAllRead(yuId);
        assertThat(updated).isEqualTo(1);
        assertThat(notificationQuery.list(yuId, null, null).unreadCount()).isZero();
    }
}
