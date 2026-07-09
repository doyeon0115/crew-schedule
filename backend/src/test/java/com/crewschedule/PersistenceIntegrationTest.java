package com.crewschedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.crewschedule.crew.domain.Crew;
import com.crewschedule.crew.domain.CrewMember;
import com.crewschedule.crew.domain.CrewRole;
import com.crewschedule.meetup.domain.Meetup;
import com.crewschedule.meetup.domain.MeetupParticipant;
import com.crewschedule.meetup.domain.MeetupStatus;
import com.crewschedule.meetup.domain.Rsvp;
import com.crewschedule.schedule.domain.ScheduleException;
import com.crewschedule.schedule.domain.WeeklySlot;
import com.crewschedule.user.domain.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Flyway 마이그레이션 + JPA 매핑 통합 검증.
 *
 * <p>Testcontainers PostgreSQL에 V1 마이그레이션을 적용한 뒤 {@code ddl-auto: validate}로
 * 스키마-엔티티 일치를 확인하고, 핵심 도메인의 저장/조회 라운드트립을 검증한다.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("V1 스키마와 엔티티 매핑이 일치하고, 크루-스케줄-약속 도메인을 저장/조회할 수 있다")
    void coreDomainRoundTrip() {
        User owner = User.builder()
                .email("jiyoung@example.com")
                .password("{noop}test")
                .nickname("지영")
                .build();
        em.persist(owner);

        Crew crew = Crew.builder()
                .name("우리끼리")
                .inviteCode("ABC123XYZ")
                .owner(owner)
                .build();
        em.persist(crew);

        em.persist(CrewMember.builder().crew(crew).user(owner).role(CrewRole.OWNER).build());

        em.persist(WeeklySlot.builder()
                .user(owner)
                .dayOfWeek(DayOfWeek.MONDAY)
                .isOff(false)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .build());
        em.persist(WeeklySlot.builder()
                .user(owner)
                .dayOfWeek(DayOfWeek.THURSDAY)
                .isOff(true)
                .build());

        em.persist(ScheduleException.builder()
                .user(owner)
                .date(LocalDate.of(2026, 7, 20))
                .isOff(true)
                .memo("여름 휴가")
                .build());

        Meetup meetup = Meetup.builder()
                .crew(crew)
                .creator(owner)
                .title("목요일 저녁 모임")
                .meetDate(LocalDate.of(2026, 7, 16))
                .startTime(LocalTime.of(19, 0))
                .location("강남")
                .build();
        em.persist(meetup);

        MeetupParticipant participant =
                MeetupParticipant.builder().meetup(meetup).user(owner).build();
        participant.respond(Rsvp.ATTEND);
        em.persist(participant);

        em.flush();
        em.clear();

        Meetup foundMeetup = em.find(Meetup.class, meetup.getId());
        assertThat(foundMeetup.getStatus()).isEqualTo(MeetupStatus.PROPOSED);
        assertThat(foundMeetup.getCrew().getInviteCode()).isEqualTo("ABC123XYZ");
        assertThat(foundMeetup.getCreatedAt()).isNotNull(); // JPA Auditing 동작 확인

        MeetupParticipant foundParticipant = em.find(MeetupParticipant.class, participant.getId());
        assertThat(foundParticipant.getRsvp()).isEqualTo(Rsvp.ATTEND);
        assertThat(foundParticipant.getRespondedAt()).isNotNull();
    }

    @Test
    @DisplayName("약속은 제안 상태에서만 확정할 수 있고, 확정 후 상태가 CONFIRMED로 바뀐다")
    void meetupConfirmTransition() {
        User owner = User.builder().email("owner@example.com").nickname("주인").build();
        em.persist(owner);
        Crew crew = Crew.builder().name("크루").inviteCode("INV456").owner(owner).build();
        em.persist(crew);

        Meetup meetup = Meetup.builder()
                .crew(crew)
                .creator(owner)
                .title("확정 테스트")
                .meetDate(LocalDate.of(2026, 8, 1))
                .startTime(LocalTime.of(18, 0))
                .build();
        em.persist(meetup);

        meetup.confirm();
        em.flush();
        em.clear();

        assertThat(em.find(Meetup.class, meetup.getId()).getStatus()).isEqualTo(MeetupStatus.CONFIRMED);
    }
}
