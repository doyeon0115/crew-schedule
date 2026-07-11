package com.crewschedule.poll.repository;

import com.crewschedule.poll.domain.PollVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    Optional<PollVote> findByCandidateIdAndUserId(Long candidateId, Long userId);

    @Query("select v from PollVote v join fetch v.user where v.candidate.poll.id = :pollId")
    List<PollVote> findAllWithUserByPollId(@Param("pollId") Long pollId);

    boolean existsByCandidateIdAndUserId(Long candidateId, Long userId);
}
