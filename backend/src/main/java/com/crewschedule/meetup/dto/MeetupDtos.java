package com.crewschedule.meetup.dto;

import com.crewschedule.meetup.domain.Meetup;
import com.crewschedule.meetup.domain.MeetupParticipant;
import com.crewschedule.meetup.domain.MeetupStatus;
import com.crewschedule.meetup.domain.Rsvp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 약속 API 요청/응답 DTO 모음. */
public final class MeetupDtos {

    private MeetupDtos() {
    }

    /**
     * 약속 생성 요청. {@code participantUserIds}가 비어 있으면 크루 전원을 초대한다.
     */
    public record CreateRequest(
            @NotBlank(message = "약속 제목은 필수입니다.") @Size(max = 100) String title,
            @NotNull LocalDate meetDate,
            @NotNull LocalTime startTime,
            @Size(max = 200) String location,
            @Size(max = 500) String memo,
            List<Long> participantUserIds) {
    }

    public record RsvpRequest(@NotNull Rsvp rsvp) {
    }

    public record ParticipantResponse(Long userId, String nickname, Rsvp rsvp) {

        public static ParticipantResponse from(MeetupParticipant participant) {
            return new ParticipantResponse(
                    participant.getUser().getId(),
                    participant.getUser().getNickname(),
                    participant.getRsvp());
        }
    }

    public record MeetupResponse(
            Long id,
            Long crewId,
            Long creatorId,
            String title,
            LocalDate meetDate,
            LocalTime startTime,
            String location,
            String memo,
            MeetupStatus status,
            List<ParticipantResponse> participants) {

        public static MeetupResponse of(Meetup meetup, List<MeetupParticipant> participants) {
            return new MeetupResponse(
                    meetup.getId(),
                    meetup.getCrew().getId(),
                    meetup.getCreator().getId(),
                    meetup.getTitle(),
                    meetup.getMeetDate(),
                    meetup.getStartTime(),
                    meetup.getLocation(),
                    meetup.getMemo(),
                    meetup.getStatus(),
                    participants.stream().map(ParticipantResponse::from).toList());
        }
    }
}
