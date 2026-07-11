package com.crewschedule.poll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.dto.CrewDtos.CrewResponse;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.poll.domain.PollStatus;
import com.crewschedule.poll.dto.PollDtos.CandidateInput;
import com.crewschedule.poll.dto.PollDtos.CandidateSummary;
import com.crewschedule.poll.dto.PollDtos.CreatePollRequest;
import com.crewschedule.poll.dto.PollDtos.PollResponse;
import com.crewschedule.poll.service.PollService;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
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

/** poll 생성 → 다중 후보 투표 → 마감(winner 확정) 전체 흐름과 검증 케이스. */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PollServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository userRepository;
    @Autowired CrewService crewService;
    @Autowired PollService pollService;

    Long jiyoungId;
    Long yujinId;
    Long sunwooId;
    Long outsiderId;
    Long crewId;

    @BeforeEach
    void setUp() {
        jiyoungId = createUser("jiyoung@test.local", "지영");
        yujinId = createUser("yujin@test.local", "유진");
        sunwooId = createUser("sunwoo@test.local", "선우");
        outsiderId = createUser("out@test.local", "외부인");

        CrewResponse crew = crewService.create(jiyoungId, "우리끼리");
        crewService.join(yujinId, crew.inviteCode());
        crewService.join(sunwooId, crew.inviteCode());
        crewId = crew.id();
    }

    @Test
    @DisplayName("생성 → 서로 다른 후보에 투표 → 마감 시 최다 득표 후보가 winner")
    void createVoteClose() {
        LocalDate d1 = LocalDate.of(2026, 7, 16);
        LocalDate d2 = LocalDate.of(2026, 7, 17);
        LocalDate d3 = LocalDate.of(2026, 7, 18);

        PollResponse poll = pollService.create(jiyoungId, crewId, new CreatePollRequest("주말 만남", List.of(
                new CandidateInput(d1, LocalTime.of(19, 0)),
                new CandidateInput(d2, null),
                new CandidateInput(d3, null))));
        assertThat(poll.status()).isEqualTo(PollStatus.OPEN);
        assertThat(poll.candidates()).hasSize(3);

        Long c1 = poll.candidates().get(0).id();
        Long c2 = poll.candidates().get(1).id();
        Long c3 = poll.candidates().get(2).id();

        // 지영: c1, c2 / 유진: c2, c3 / 선우: c2  → c1=1, c2=3, c3=1 → winner=c2
        pollService.vote(jiyoungId, poll.id(), c1);
        pollService.vote(jiyoungId, poll.id(), c2);
        pollService.vote(yujinId, poll.id(), c2);
        pollService.vote(yujinId, poll.id(), c3);
        PollResponse afterVotes = pollService.vote(sunwooId, poll.id(), c2);

        assertThat(candidateOf(afterVotes, c2).voteCount()).isEqualTo(3);
        assertThat(candidateOf(afterVotes, c2).voterIds()).containsExactlyInAnyOrder(jiyoungId, yujinId, sunwooId);
        assertThat(candidateOf(afterVotes, c2).votedByMe()).isTrue(); // 선우 시점의 응답

        PollResponse closed = pollService.close(jiyoungId, poll.id());
        assertThat(closed.status()).isEqualTo(PollStatus.CLOSED);
        assertThat(closed.winnerCandidateId()).isEqualTo(c2);
        assertThat(closed.closedAt()).isNotNull();
    }

    @Test
    @DisplayName("동점일 때 이른 날짜 후보가 winner")
    void tieBreakerByEarlierDate() {
        PollResponse poll = pollService.create(jiyoungId, crewId, new CreatePollRequest("동점 테스트", List.of(
                new CandidateInput(LocalDate.of(2026, 8, 10), null),
                new CandidateInput(LocalDate.of(2026, 8, 20), null))));
        Long earlier = poll.candidates().get(0).id();
        Long later = poll.candidates().get(1).id();

        pollService.vote(jiyoungId, poll.id(), earlier);
        pollService.vote(yujinId, poll.id(), later);

        PollResponse closed = pollService.close(jiyoungId, poll.id());
        assertThat(closed.winnerCandidateId()).isEqualTo(earlier);
    }

    @Test
    @DisplayName("이미 투표한 후보에 다시 투표하면 409 (DUPLICATE_POLL_VOTE)")
    void duplicateVoteIsRejected() {
        PollResponse poll = pollService.create(jiyoungId, crewId, new CreatePollRequest("중복 방지", List.of(
                new CandidateInput(LocalDate.of(2026, 9, 1), null))));
        Long cid = poll.candidates().get(0).id();
        pollService.vote(yujinId, poll.id(), cid);

        assertThatThrownBy(() -> pollService.vote(yujinId, poll.id(), cid))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_POLL_VOTE);
    }

    @Test
    @DisplayName("투표 취소는 멱등: 없는 표를 취소해도 오류 없음")
    void unvoteIsIdempotent() {
        PollResponse poll = pollService.create(jiyoungId, crewId, new CreatePollRequest("취소 테스트", List.of(
                new CandidateInput(LocalDate.of(2026, 9, 10), null))));
        Long cid = poll.candidates().get(0).id();
        pollService.vote(yujinId, poll.id(), cid);

        PollResponse afterFirst = pollService.unvote(yujinId, poll.id(), cid);
        assertThat(candidateOf(afterFirst, cid).voteCount()).isZero();

        // 두 번째 취소는 조용히 통과
        PollResponse afterSecond = pollService.unvote(yujinId, poll.id(), cid);
        assertThat(candidateOf(afterSecond, cid).voteCount()).isZero();
    }

    @Test
    @DisplayName("크루 비멤버는 투표 상세를 볼 수 없고 투표할 수 없다")
    void nonMemberCannotAccess() {
        PollResponse poll = pollService.create(jiyoungId, crewId, new CreatePollRequest("비멤버 차단", List.of(
                new CandidateInput(LocalDate.of(2026, 9, 20), null))));
        assertThatThrownBy(() -> pollService.getDetail(outsiderId, poll.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CREW_MEMBER);

        assertThatThrownBy(() -> pollService.vote(outsiderId, poll.id(), poll.candidates().get(0).id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CREW_MEMBER);
    }

    @Test
    @DisplayName("마감은 생성자만, 마감 후 재투표·재마감 불가")
    void closeGuards() {
        PollResponse poll = pollService.create(jiyoungId, crewId, new CreatePollRequest("마감 검증", List.of(
                new CandidateInput(LocalDate.of(2026, 10, 1), null))));
        Long cid = poll.candidates().get(0).id();
        pollService.vote(jiyoungId, poll.id(), cid);

        // 생성자가 아니면 마감 불가
        assertThatThrownBy(() -> pollService.close(yujinId, poll.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_POLL_CREATOR);

        pollService.close(jiyoungId, poll.id());

        // 마감된 poll에는 재투표 불가
        assertThatThrownBy(() -> pollService.vote(yujinId, poll.id(), cid))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.POLL_ALREADY_CLOSED);

        // 재마감 불가
        assertThatThrownBy(() -> pollService.close(jiyoungId, poll.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.POLL_ALREADY_CLOSED);
    }

    @Test
    @DisplayName("득표가 하나도 없으면 마감 실패")
    void closeWithoutVotesFails() {
        PollResponse poll = pollService.create(jiyoungId, crewId, new CreatePollRequest("무득표", List.of(
                new CandidateInput(LocalDate.of(2026, 11, 1), null))));
        assertThatThrownBy(() -> pollService.close(jiyoungId, poll.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.POLL_HAS_NO_VOTES);
    }

    private CandidateSummary candidateOf(PollResponse response, Long candidateId) {
        return response.candidates().stream()
                .filter(c -> c.id().equals(candidateId))
                .findFirst()
                .orElseThrow();
    }

    private Long createUser(String email, String nickname) {
        return userRepository
                .save(User.builder().email(email).nickname(nickname).build())
                .getId();
    }
}
