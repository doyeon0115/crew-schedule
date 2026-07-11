package com.crewschedule.poll.service;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.domain.Crew;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.poll.domain.DatePoll;
import com.crewschedule.poll.domain.PollCandidate;
import com.crewschedule.poll.domain.PollVote;
import com.crewschedule.poll.dto.PollDtos.CandidateInput;
import com.crewschedule.poll.dto.PollDtos.CandidateSummary;
import com.crewschedule.poll.dto.PollDtos.CreatePollRequest;
import com.crewschedule.poll.dto.PollDtos.PollResponse;
import com.crewschedule.poll.repository.DatePollRepository;
import com.crewschedule.poll.repository.PollCandidateRepository;
import com.crewschedule.poll.repository.PollVoteRepository;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 날짜 투표. 크루 멤버가 후보 날짜를 담아 생성하고, 각 후보에 독립적으로 투표한다.
 * 마감은 생성자만 할 수 있으며, 최다 득표 후보가 winner로 확정된다(동점 시 이른 날짜).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollService {

    private final DatePollRepository pollRepository;
    private final PollCandidateRepository candidateRepository;
    private final PollVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final CrewService crewService;
    private final EntityManager em;

    @Transactional
    public PollResponse create(Long userId, Long crewId, CreatePollRequest request) {
        Crew crew = crewService.getCrewForMember(userId, crewId);
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (request.candidates().isEmpty()) {
            throw new BusinessException(ErrorCode.POLL_NEEDS_CANDIDATES);
        }

        DatePoll poll = pollRepository.save(DatePoll.builder()
                .crew(crew)
                .creator(creator)
                .title(request.title())
                .build());

        List<PollCandidate> candidates = request.candidates().stream()
                .distinct()
                .map(input -> candidateRepository.save(PollCandidate.builder()
                        .poll(poll)
                        .candidateDate(input.date())
                        .startTime(input.startTime())
                        .build()))
                .toList();

        return PollResponse.of(poll, candidates, buildSummaries(candidates, List.of(), userId));
    }

    public List<PollResponse> listByCrew(Long userId, Long crewId) {
        crewService.getCrewForMember(userId, crewId);
        List<DatePoll> polls = pollRepository.findAllByCrewIdOrderByCreatedAtDesc(crewId);
        return polls.stream().map(p -> loadPoll(p, userId)).toList();
    }

    public PollResponse getDetail(Long userId, Long pollId) {
        DatePoll poll = getPoll(pollId);
        crewService.getCrewForMember(userId, poll.getCrew().getId());
        return loadPoll(poll, userId);
    }

    /** 특정 후보에 투표. 이미 투표했다면 409. */
    @Transactional
    public PollResponse vote(Long userId, Long pollId, Long candidateId) {
        DatePoll poll = getPoll(pollId);
        crewService.getCrewForMember(userId, poll.getCrew().getId());
        if (poll.isClosed()) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_CLOSED);
        }
        PollCandidate candidate = getCandidateInPoll(candidateId, pollId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        try {
            voteRepository.save(PollVote.builder().candidate(candidate).user(user).build());
            // save를 flush해서 UNIQUE 위반이 여기서 잡히도록 강제. 동시 요청에서도 하나만 성공.
            em.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_POLL_VOTE);
        }
        return loadPoll(poll, userId);
    }

    /** 투표 취소. 없으면 조용히 넘어감(멱등). */
    @Transactional
    public PollResponse unvote(Long userId, Long pollId, Long candidateId) {
        DatePoll poll = getPoll(pollId);
        crewService.getCrewForMember(userId, poll.getCrew().getId());
        if (poll.isClosed()) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_CLOSED);
        }
        getCandidateInPoll(candidateId, pollId);
        voteRepository.findByCandidateIdAndUserId(candidateId, userId).ifPresent(voteRepository::delete);
        return loadPoll(poll, userId);
    }

    /** 마감. 생성자만 가능. 최다 득표 후보를 winner로 확정한다. */
    @Transactional
    public PollResponse close(Long userId, Long pollId) {
        DatePoll poll = getPoll(pollId);
        if (!poll.getCreator().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POLL_CREATOR);
        }
        if (poll.isClosed()) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_CLOSED);
        }
        List<PollCandidate> candidates =
                candidateRepository.findAllByPollIdOrderByCandidateDateAscStartTimeAsc(pollId);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.POLL_HAS_NO_CANDIDATES);
        }
        Map<Long, Long> voteCounts = countVotesByCandidate(pollId);
        PollCandidate winner = candidates.stream()
                .filter(c -> voteCounts.getOrDefault(c.getId(), 0L) > 0)
                .max(Comparator
                        .comparingLong((PollCandidate c) -> voteCounts.getOrDefault(c.getId(), 0L))
                        .thenComparing((PollCandidate c) -> c.getCandidateDate(), Comparator.reverseOrder()))
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_HAS_NO_VOTES));
        poll.close(winner.getId());
        return loadPoll(poll, userId);
    }

    private PollResponse loadPoll(DatePoll poll, Long viewerId) {
        List<PollCandidate> candidates =
                candidateRepository.findAllByPollIdOrderByCandidateDateAscStartTimeAsc(poll.getId());
        List<PollVote> votes = voteRepository.findAllWithUserByPollId(poll.getId());
        return PollResponse.of(poll, candidates, buildSummaries(candidates, votes, viewerId));
    }

    private List<CandidateSummary> buildSummaries(
            List<PollCandidate> candidates, List<PollVote> votes, Long viewerId) {
        Map<Long, List<PollVote>> byCandidate = votes.stream()
                .collect(Collectors.groupingBy(v -> v.getCandidate().getId()));
        return candidates.stream()
                .map(c -> {
                    List<PollVote> voteRows = byCandidate.getOrDefault(c.getId(), List.of());
                    List<Long> voterIds = voteRows.stream().map(v -> v.getUser().getId()).toList();
                    boolean votedByMe = voterIds.contains(viewerId);
                    return new CandidateSummary(
                            c.getId(),
                            c.getCandidateDate(),
                            c.getStartTime(),
                            voteRows.size(),
                            voterIds,
                            votedByMe);
                })
                .toList();
    }

    private Map<Long, Long> countVotesByCandidate(Long pollId) {
        Map<Long, Long> counts = new HashMap<>();
        for (PollVote v : voteRepository.findAllWithUserByPollId(pollId)) {
            counts.merge(v.getCandidate().getId(), 1L, Long::sum);
        }
        return counts;
    }

    private DatePoll getPoll(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));
    }

    private PollCandidate getCandidateInPoll(Long candidateId, Long pollId) {
        PollCandidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_CANDIDATE_NOT_FOUND));
        if (!candidate.getPoll().getId().equals(pollId)) {
            throw new BusinessException(ErrorCode.POLL_CANDIDATE_NOT_FOUND);
        }
        return candidate;
    }
}
