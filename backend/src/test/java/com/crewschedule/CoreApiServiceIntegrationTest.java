package com.crewschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.dto.CrewDtos.CrewResponse;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.meetup.domain.MeetupStatus;
import com.crewschedule.meetup.domain.Rsvp;
import com.crewschedule.meetup.dto.MeetupDtos.CreateRequest;
import com.crewschedule.meetup.dto.MeetupDtos.MeetupResponse;
import com.crewschedule.meetup.service.MeetupService;
import com.crewschedule.schedule.dto.ScheduleDtos.DayRecommendationResponse;
import com.crewschedule.schedule.dto.ScheduleDtos.SlotRequest;
import com.crewschedule.schedule.service.RecommendationService;
import com.crewschedule.schedule.service.ScheduleService;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 크루 → 스케줄 → 추천 → 약속(RSVP)까지 핵심 서비스 흐름 통합 테스트.
 * local 프로필 시드가 실행되지 않도록 test 프로필로 돌린다.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class CoreApiServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    UserRepository userRepository;

    @Autowired
    CrewService crewService;

    @Autowired
    ScheduleService scheduleService;

    @Autowired
    RecommendationService recommendationService;

    @Autowired
    MeetupService meetupService;

    Long jiyoungId;
    Long yujinId;

    @BeforeEach
    void setUp() {
        jiyoungId = userRepository
                .save(User.builder().email("jiyoung@test.local").nickname("지영").build())
                .getId();
        yujinId = userRepository
                .save(User.builder().email("yujin@test.local").nickname("유진").build())
                .getId();
    }

    @Test
    @DisplayName("크루 생성 → 초대코드 가입 → 스케줄 입력 → 추천 → 약속 제안/RSVP/확정 전체 흐름")
    void fullFlow() {
        // 1. 크루 생성 + 초대코드 가입
        CrewResponse crew = crewService.create(jiyoungId, "우리끼리");
        assertThat(crew.inviteCode()).hasSize(8);
        crewService.join(yujinId, crew.inviteCode());
        assertThat(crewService.getDetail(jiyoungId, crew.id()).members()).hasSize(2);

        // 2. 주간 스케줄 입력 — 목요일: 지영 휴무, 유진 17시 퇴근
        scheduleService.updateMySchedule(jiyoungId, List.of(
                new SlotRequest(DayOfWeek.THURSDAY, true, null, null),
                new SlotRequest(DayOfWeek.FRIDAY, false, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        scheduleService.updateMySchedule(yujinId, List.of(
                new SlotRequest(DayOfWeek.THURSDAY, false, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new SlotRequest(DayOfWeek.FRIDAY, false, LocalTime.of(9, 0), LocalTime.of(20, 30))));

        // 3. 추천 — 2026-07-16(목)부터 7일. 목요일이 금요일보다 상위여야 한다.
        LocalDate thursday = LocalDate.of(2026, 7, 16);
        List<DayRecommendationResponse> days = recommendationService.recommend(jiyoungId, crew.id(), thursday);
        assertThat(days).hasSize(7);

        DayRecommendationResponse thu = days.get(0);
        assertThat(thu.dayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
        assertThat(thu.availableFrom()).isEqualTo(LocalTime.of(17, 0));
        assertThat(thu.offCount()).isEqualTo(1);

        DayRecommendationResponse fri = days.get(1);
        assertThat(fri.availableFrom()).isEqualTo(LocalTime.of(20, 30));
        assertThat(thu.rank()).isLessThan(fri.rank());

        // 스케줄 미입력 요일은 휴무 취급 → 전원 하루 종일 가능
        DayRecommendationResponse sat = days.get(2);
        assertThat(sat.allDayFree()).isTrue();
        assertThat(sat.rank()).isEqualTo(1);

        // 4. 약속 제안(전원 초대) → RSVP → 확정
        MeetupResponse meetup = meetupService.propose(jiyoungId, crew.id(), new CreateRequest(
                "목요일 저녁 모임", thursday, LocalTime.of(19, 0), "강남", null, null));
        assertThat(meetup.participants()).hasSize(2);
        assertThat(meetup.status()).isEqualTo(MeetupStatus.PROPOSED);

        MeetupResponse afterRsvp = meetupService.respond(yujinId, meetup.id(), Rsvp.ATTEND);
        assertThat(afterRsvp.participants())
                .filteredOn(p -> p.userId().equals(yujinId))
                .singleElement()
                .satisfies(p -> assertThat(p.rsvp()).isEqualTo(Rsvp.ATTEND));

        MeetupResponse confirmed = meetupService.confirm(jiyoungId, meetup.id());
        assertThat(confirmed.status()).isEqualTo(MeetupStatus.CONFIRMED);
    }

    @Test
    @DisplayName("크루 멤버가 아니면 크루 조회·추천을 볼 수 없고, 잘못된 초대코드는 거부된다")
    void accessControl() {
        CrewResponse crew = crewService.create(jiyoungId, "우리끼리");

        assertThatThrownBy(() -> crewService.getDetail(yujinId, crew.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CREW_MEMBER);

        assertThatThrownBy(() -> crewService.join(yujinId, "WRONGCODE"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INVITE_CODE);

        // 이미 가입한 크루에 다시 가입 불가
        assertThatThrownBy(() -> crewService.join(jiyoungId, crew.inviteCode()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_CREW_MEMBER);

        // 생성자가 아니면 확정 불가
        crewService.join(yujinId, crew.inviteCode());
        MeetupResponse meetup = meetupService.propose(jiyoungId, crew.id(), new CreateRequest(
                "모임", LocalDate.of(2026, 8, 1), LocalTime.of(19, 0), null, null, null));
        assertThatThrownBy(() -> meetupService.confirm(yujinId, meetup.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_MEETUP_CREATOR);
    }
}
