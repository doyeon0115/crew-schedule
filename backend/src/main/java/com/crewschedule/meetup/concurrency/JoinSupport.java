package com.crewschedule.meetup.concurrency;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.meetup.domain.Meetup;
import com.crewschedule.meetup.domain.MeetupParticipant;
import com.crewschedule.meetup.domain.Rsvp;
import com.crewschedule.meetup.repository.MeetupParticipantRepository;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** 각 JoinStrategy에서 공통으로 쓰는 참여자 저장·중복 검사 로직. */
@Component
@RequiredArgsConstructor
public class JoinSupport {

    private final UserRepository userRepository;
    private final MeetupParticipantRepository participantRepository;

    public User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public void ensureJoinable(Meetup meetup) {
        if (!meetup.hasCapacityLimit()) {
            throw new BusinessException(ErrorCode.MEETUP_NOT_JOINABLE);
        }
    }

    public void ensureNotJoined(Long meetupId, Long userId) {
        if (participantRepository.existsByMeetupIdAndUserId(meetupId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }
    }

    /**
     * 참여자 레코드 저장. (meetup_id, user_id) UNIQUE 제약이 있으므로
     * 동시 요청에서 하나만 성공하도록 DB가 최종 방어선을 제공한다.
     */
    public Long saveParticipant(Meetup meetup, User user) {
        try {
            MeetupParticipant p = MeetupParticipant.builder().meetup(meetup).user(user).build();
            // 자동 참여자는 참석(ATTEND)으로 저장.
            p.respond(Rsvp.ATTEND);
            return participantRepository.saveAndFlush(p).getId();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }
    }
}
