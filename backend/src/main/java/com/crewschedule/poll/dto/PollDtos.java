package com.crewschedule.poll.dto;

import com.crewschedule.poll.domain.DatePoll;
import com.crewschedule.poll.domain.PollCandidate;
import com.crewschedule.poll.domain.PollStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class PollDtos {

    private PollDtos() {}

    public record CreatePollRequest(
            @NotBlank @Size(max = 100) String title,
            @NotNull @Size(min = 1, max = 20) @Valid List<CandidateInput> candidates) {}

    public record CandidateInput(@NotNull LocalDate date, LocalTime startTime) {}

    public record CandidateSummary(
            Long id,
            LocalDate date,
            LocalTime startTime,
            int voteCount,
            List<Long> voterIds,
            boolean votedByMe) {}

    public record PollResponse(
            Long id,
            Long crewId,
            Long creatorId,
            String title,
            PollStatus status,
            Long winnerCandidateId,
            LocalDateTime closedAt,
            LocalDateTime createdAt,
            List<CandidateSummary> candidates) {

        public static PollResponse of(DatePoll poll, List<PollCandidate> candidates, List<CandidateSummary> summaries) {
            return new PollResponse(
                    poll.getId(),
                    poll.getCrew().getId(),
                    poll.getCreator().getId(),
                    poll.getTitle(),
                    poll.getStatus(),
                    poll.getWinnerCandidateId(),
                    poll.getClosedAt(),
                    poll.getCreatedAt(),
                    summaries);
        }
    }
}
