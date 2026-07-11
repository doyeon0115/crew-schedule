package com.crewschedule.meetup.concurrency.strategy;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.meetup.concurrency.ConcurrencyProperties;
import com.crewschedule.meetup.concurrency.JoinStrategy;
import com.crewschedule.meetup.concurrency.JoinStrategyType;
import com.crewschedule.meetup.concurrency.JoinSupport;
import com.crewschedule.meetup.domain.Meetup;
import com.crewschedule.meetup.repository.MeetupRepository;
import com.crewschedule.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 낙관적 락. Meetup.currentParticipants가 증가하면서 @Version이 커밋 시 조건에 붙는다.
 * 다른 트랜잭션이 먼저 커밋했다면 UPDATE rowcount=0으로 예외 → 재시도.
 * 최대 재시도 초과 시 {@code JOIN_LOCK_FAILED}.
 */
@Slf4j
@Component
public class OptimisticJoinStrategy implements JoinStrategy {

    private final MeetupRepository meetupRepository;
    private final JoinSupport support;
    private final ConcurrencyProperties props;
    /** 자기참조 프록시. 클래스 내부 호출로는 @Transactional이 안 걸리므로 프록시 경유 호출. */
    private final OptimisticJoinStrategy self;

    public OptimisticJoinStrategy(
            MeetupRepository meetupRepository,
            JoinSupport support,
            ConcurrencyProperties props,
            @Lazy OptimisticJoinStrategy self) {
        this.meetupRepository = meetupRepository;
        this.support = support;
        this.props = props;
        this.self = self;
    }

    @Override
    public JoinStrategyType type() {
        return JoinStrategyType.OPTIMISTIC;
    }

    @Override
    public Long join(Long meetupId, Long userId) {
        int attempts = 0;
        while (true) {
            try {
                return self.attemptJoin(meetupId, userId);
            } catch (ObjectOptimisticLockingFailureException e) {
                attempts++;
                if (attempts >= props.optimisticRetryMax()) {
                    log.warn("optimistic join gave up after {} retries for meetup {}", attempts, meetupId);
                    throw new BusinessException(ErrorCode.JOIN_LOCK_FAILED);
                }
                try {
                    Thread.sleep(5L + attempts * 3L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.JOIN_LOCK_FAILED);
                }
            }
        }
    }

    /**
     * 한 번의 시도. REQUIRES_NEW로 시도마다 독립 트랜잭션을 열어야 재시도 시 이전 상태가 롤백된다.
     * public이어야 프록시 경유 호출이 성립.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long attemptJoin(Long meetupId, Long userId) {
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETUP_NOT_FOUND));
        support.ensureJoinable(meetup);
        support.ensureNotJoined(meetupId, userId);
        if (meetup.isFull()) {
            throw new BusinessException(ErrorCode.MEETUP_FULL);
        }
        User user = support.loadUser(userId);
        meetup.reserveSlot();
        return support.saveParticipant(meetup, user);
    }
}
