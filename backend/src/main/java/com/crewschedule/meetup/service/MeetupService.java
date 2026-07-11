package com.crewschedule.meetup.service;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.domain.Crew;
import com.crewschedule.crew.domain.CrewMember;
import com.crewschedule.crew.repository.CrewMemberRepository;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.meetup.concurrency.ConcurrencyProperties;
import com.crewschedule.meetup.concurrency.JoinStrategyRegistry;
import com.crewschedule.meetup.domain.Meetup;
import com.crewschedule.meetup.domain.MeetupParticipant;
import com.crewschedule.meetup.domain.Rsvp;
import com.crewschedule.meetup.dto.MeetupDtos.CreateRequest;
import com.crewschedule.meetup.dto.MeetupDtos.MeetupResponse;
import com.crewschedule.meetup.repository.MeetupParticipantRepository;
import com.crewschedule.meetup.repository.MeetupRepository;
import com.crewschedule.user.domain.User;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetupService {

    private final MeetupRepository meetupRepository;
    private final MeetupParticipantRepository participantRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewService crewService;
    private final JoinStrategyRegistry joinStrategies;
    private final ConcurrencyProperties concurrencyProps;

    /**
     * 약속을 제안한다.
     * <p>{@code capacity}가 null이면 기존 초대형: participantUserIds가 비면 크루 전원 초대.
     * <p>{@code capacity}가 있으면 선착순: 창시자만 자동 참여, 나머지는 /join으로 합류.
     */
    @Transactional
    public MeetupResponse propose(Long userId, Long crewId, CreateRequest request) {
        Crew crew = crewService.getCrewForMember(userId, crewId);
        List<CrewMember> crewMembers = crewMemberRepository.findAllWithUserByCrewId(crewId);
        Map<Long, User> memberUsers = crewMembers.stream()
                .collect(Collectors.toMap(cm -> cm.getUser().getId(), CrewMember::getUser));
        User creator = memberUsers.get(userId);

        List<User> participants;
        if (request.capacity() != null) {
            participants = List.of(creator);
        } else {
            participants = resolveParticipants(request.participantUserIds(), memberUsers);
        }

        Meetup meetup = Meetup.builder()
                .crew(crew)
                .creator(creator)
                .title(request.title())
                .meetDate(request.meetDate())
                .startTime(request.startTime())
                .location(request.location())
                .memo(request.memo())
                .capacity(request.capacity())
                .build();
        meetupRepository.save(meetup);

        List<MeetupParticipant> saved = participants.stream()
                .map(user -> {
                    MeetupParticipant p = MeetupParticipant.builder().meetup(meetup).user(user).build();
                    if (request.capacity() != null) {
                        // 창시자는 자동 참여(ATTEND) → 슬롯 소비
                        p.respond(Rsvp.ATTEND);
                        meetup.reserveSlot();
                    }
                    return participantRepository.save(p);
                })
                .toList();
        return MeetupResponse.of(meetup, saved);
    }

    /** 선착순 참여. 실제 락 로직은 {@link com.crewschedule.meetup.concurrency.JoinStrategy} 구현체가 담당. */
    public MeetupResponse join(Long userId, Long meetupId) {
        joinStrategies.get(concurrencyProps.joinStrategy()).join(meetupId, userId);
        return toResponse(getMeetup(meetupId));
    }

    /** 크루의 약속 목록(참여자·RSVP 포함). */
    public List<MeetupResponse> getCrewMeetups(Long userId, Long crewId) {
        crewService.getCrewForMember(userId, crewId);
        List<Meetup> meetups = meetupRepository.findAllByCrewIdOrderByMeetDateAscStartTimeAsc(crewId);
        if (meetups.isEmpty()) {
            return List.of();
        }
        Map<Long, List<MeetupParticipant>> participantsByMeetup = participantRepository
                .findAllWithUserByMeetupIdIn(meetups.stream().map(Meetup::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(p -> p.getMeetup().getId()));
        return meetups.stream()
                .map(m -> MeetupResponse.of(m, participantsByMeetup.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    /** 참석 여부 응답. 초대된 참여자만 응답할 수 있다. */
    @Transactional
    public MeetupResponse respond(Long userId, Long meetupId, Rsvp rsvp) {
        Meetup meetup = getMeetup(meetupId);
        MeetupParticipant participant = participantRepository
                .findByMeetupIdAndUserId(meetupId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_MEETUP_PARTICIPANT));
        participant.respond(rsvp);
        return toResponse(meetup);
    }

    /** 약속 확정. 생성자만 할 수 있다. */
    @Transactional
    public MeetupResponse confirm(Long userId, Long meetupId) {
        Meetup meetup = getMeetupForCreator(userId, meetupId);
        meetup.confirm();
        return toResponse(meetup);
    }

    /** 약속 취소. 생성자만 할 수 있다. */
    @Transactional
    public MeetupResponse cancel(Long userId, Long meetupId) {
        Meetup meetup = getMeetupForCreator(userId, meetupId);
        meetup.cancel();
        return toResponse(meetup);
    }

    private List<User> resolveParticipants(List<Long> requestedIds, Map<Long, User> memberUsers) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return List.copyOf(memberUsers.values());
        }
        Set<Long> uniqueIds = Set.copyOf(requestedIds);
        if (!memberUsers.keySet().containsAll(uniqueIds)) {
            throw new BusinessException(ErrorCode.NOT_CREW_MEMBER, "크루 멤버가 아닌 사용자는 초대할 수 없습니다.");
        }
        return uniqueIds.stream().map(memberUsers::get).toList();
    }

    private Meetup getMeetup(Long meetupId) {
        return meetupRepository
                .findById(meetupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETUP_NOT_FOUND));
    }

    private Meetup getMeetupForCreator(Long userId, Long meetupId) {
        Meetup meetup = getMeetup(meetupId);
        if (!meetup.getCreator().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_MEETUP_CREATOR);
        }
        return meetup;
    }

    private MeetupResponse toResponse(Meetup meetup) {
        return MeetupResponse.of(
                meetup, participantRepository.findAllWithUserByMeetupIdIn(List.of(meetup.getId())));
    }
}
