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
 * 락 없이 count → save. 정합성이 깨지는 baseline.
 *
 * <p>동시에 여러 스레드가 이 코드를 실행하면 {@code isFull()} 검사와 {@code reserveSlot()} 사이에
 * 다른 스레드가 이미 슬롯을 채워도 이 스레드는 그 사실을 모른다. 결과: 오버부킹.
 * (다만 uq_meetup_participants 제약 덕에 같은 유저의 중복 참여는 여기서도 막힌다.)
 */
@Component
@RequiredArgsConstructor
public class NaiveJoinStrategy implements JoinStrategy {

    private final MeetupRepository meetupRepository;
    private final JoinSupport support;

    @Override
    public JoinStrategyType type() {
        return JoinStrategyType.NAIVE;
    }

    @Override
    @Transactional
    public Long join(Long meetupId, Long userId) {
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETUP_NOT_FOUND));
        support.ensureJoinable(meetup);
        support.ensureNotJoined(meetupId, userId);

        // ⚠️ 여기가 레이스 창: 다른 트랜잭션이 방금 마지막 슬롯을 채웠어도 이 스레드는 모른다.
        if (meetup.isFull()) {
            throw new BusinessException(ErrorCode.MEETUP_FULL);
        }
        User user = support.loadUser(userId);
        meetup.reserveSlot();
        return support.saveParticipant(meetup, user);
    }
}
