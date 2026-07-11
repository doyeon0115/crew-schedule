package com.crewschedule.meetup.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.crew.dto.CrewDtos.CrewResponse;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.meetup.domain.Meetup;
import com.crewschedule.meetup.dto.MeetupDtos.CreateRequest;
import com.crewschedule.meetup.dto.MeetupDtos.MeetupResponse;
import com.crewschedule.meetup.repository.MeetupParticipantRepository;
import com.crewschedule.meetup.repository.MeetupRepository;
import com.crewschedule.meetup.service.MeetupService;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import org.testcontainers.utility.DockerImageName;

/**
 * Phase 4 정합성 검증: 정원 8 모임에 50명이 동시에 참여를 시도하고 정확히 8명만 성공하는지.
 *
 * <p>NAIVE 전략은 오버부킹이 발생할 수 있고(참여자 &gt; 정원), 나머지 4개 전략은 반드시 정원 이하만 성공한다.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class JoinConcurrencyTest {

    private static final int CAPACITY = 8;
    private static final int CONTENDERS = 50;

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

    @Autowired UserRepository userRepository;
    @Autowired CrewService crewService;
    @Autowired MeetupService meetupService;
    @Autowired MeetupRepository meetupRepository;
    @Autowired MeetupParticipantRepository participantRepository;
    @Autowired JoinStrategyRegistry strategies;

    Long creatorId;
    Long crewId;
    List<Long> contenderIds;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행마다 완전히 새 크루/유저 생성 (스키마 공유되므로 격리는 데이터 레벨)
        creatorId = createUser("creator@t.local", "생성자");
        contenderIds = new ArrayList<>(CONTENDERS);
        for (int i = 0; i < CONTENDERS; i++) {
            contenderIds.add(createUser("u" + i + "@t.local", "유저" + i));
        }
        CrewResponse crew = crewService.create(creatorId, "번개크루");
        for (Long id : contenderIds) {
            crewService.join(id, crew.inviteCode());
        }
        crewId = crew.id();
    }

    @Test
    @DisplayName("NAIVE 전략은 동시 참여 시 오버부킹이 발생할 수 있다 (baseline)")
    void naiveOverbooks() throws Exception {
        Long meetupId = createFlashMeetup();
        Result r = runConcurrentJoins(JoinStrategyType.NAIVE, meetupId);

        // NAIVE는 오버부킹을 완전히 시연하지 못할 때도 있지만, 최소한 성공 수는 정원 이상.
        // 결정론적 어서션: 성공 수 >= 정원 (아마 초과)
        assertThat(r.success).isGreaterThanOrEqualTo(CAPACITY);
        // 다만 uq_meetup_participants 제약 덕에 유저 중복 참여는 여기서도 없음
        long dbCount = participantRepository.countByMeetupId(meetupId);
        assertThat(dbCount).isEqualTo(r.success);
    }

    @ParameterizedTest(name = "정합성 있는 전략은 정확히 {0}명만 참여시킨다")
    @EnumSource(
            value = JoinStrategyType.class,
            names = {"OPTIMISTIC", "PESSIMISTIC", "DISTRIBUTED_LOCK", "REDIS_ATOMIC"})
    @DisplayName("정합성 있는 전략들은 정확히 capacity만큼만 성공한다")
    void safeStrategiesDoNotOverbook(JoinStrategyType strategyType) throws Exception {
        Long meetupId = createFlashMeetup();
        Result r = runConcurrentJoins(strategyType, meetupId);

        // 창시자가 이미 1명 소비했으므로 남은 슬롯 = CAPACITY - 1
        int remaining = CAPACITY - 1;
        assertThat(r.success)
                .as("전략 %s: 성공 수는 정확히 남은 슬롯 %d개여야 함", strategyType, remaining)
                .isEqualTo(remaining);
        assertThat(r.overbookErrors + r.otherErrors).isEqualTo(CONTENDERS - remaining);

        Meetup after = meetupRepository.findById(meetupId).orElseThrow();
        assertThat(after.getCurrentParticipants()).isEqualTo(CAPACITY);
        long dbCount = participantRepository.countByMeetupId(meetupId);
        assertThat(dbCount).isEqualTo(CAPACITY);
    }

    private Long createFlashMeetup() {
        MeetupResponse m = meetupService.propose(
                creatorId,
                crewId,
                new CreateRequest(
                        "번개",
                        LocalDate.of(2026, 12, 24),
                        LocalTime.of(19, 0),
                        "홍대",
                        null,
                        null,
                        CAPACITY));
        return m.id();
    }

    private Result runConcurrentJoins(JoinStrategyType type, Long meetupId) throws Exception {
        JoinStrategy strategy = strategies.get(type);
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(CONTENDERS, 32));
        CountDownLatch ready = new CountDownLatch(CONTENDERS);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONTENDERS);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger overbookErrors = new AtomicInteger();
        AtomicInteger otherErrors = new AtomicInteger();

        for (Long userId : contenderIds) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    strategy.join(meetupId, userId);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    overbookErrors.incrementAndGet();
                } catch (Exception e) {
                    otherErrors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        go.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();
        return new Result(success.get(), overbookErrors.get(), otherErrors.get());
    }

    private Long createUser(String email, String nickname) {
        return userRepository.save(User.builder().email(email).nickname(nickname).build()).getId();
    }

    private record Result(int success, int overbookErrors, int otherErrors) {}
}
