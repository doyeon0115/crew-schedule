package com.crewschedule.meetup.concurrency.strategy;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.meetup.concurrency.JoinStrategy;
import com.crewschedule.meetup.concurrency.JoinStrategyType;
import com.crewschedule.meetup.concurrency.JoinSupport;
import com.crewschedule.meetup.domain.Meetup;
import com.crewschedule.meetup.repository.MeetupRepository;
import com.crewschedule.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비관적 락. {@code SELECT ... FOR UPDATE}로 Meetup row에 락을 걸어 정원 검사·증가를 직렬화.
 * 락 획득 순서로만 진행되므로 정합성은 확실하지만 처리량은 낙관/원자보다 낮다.
 */
@Component
@RequiredArgsConstructor
public class PessimisticJoinStrategy implements JoinStrategy {

    private final MeetupRepository meetupRepository;
    private final JoinSupport support;

    @Override
    public JoinStrategyType type() {
        return JoinStrategyType.PESSIMISTIC;
    }

    @Override
    @Transactional
    public Long join(Long meetupId, Long userId) {
        // FOR UPDATE 이전에 참여 여부·정원 특성 등 빠른 shortcut 검증
        support.ensureNotJoined(meetupId, userId);

        Meetup meetup = meetupRepository.findByIdForUpdate(meetupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETUP_NOT_FOUND));
        support.ensureJoinable(meetup);
        if (meetup.isFull()) {
            throw new BusinessException(ErrorCode.MEETUP_FULL);
        }
        User user = support.loadUser(userId);
        meetup.reserveSlot();
        return support.saveParticipant(meetup, user);
    }
}
