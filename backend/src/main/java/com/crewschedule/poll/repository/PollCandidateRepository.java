package com.crewschedule.poll.repository;

import com.crewschedule.poll.domain.PollCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollCandidateRepository extends JpaRepository<PollCandidate, Long> {

    List<PollCandidate> findAllByPollIdOrderByCandidateDateAscStartTimeAsc(Long pollId);
}
