package com.crewschedule.meetup.concurrency.strategy;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.meetup.concurrency.JoinStrategy;
import com.crewschedule.meetup.concurrency.JoinStrategyType;
import com.crewschedule.meetup.concurrency.JoinSupport;
import com.crewschedule.meetup.domain.Meetup;
import com.crewschedule.meetup.repository.MeetupRepository;
import com.crewschedule.user.domain.User;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redisson RLock으로 분산 환경(다중 인스턴스)에서도 동시성 제어.
 * DB 인스턴스 간에 락을 공유해야 할 때 유용. 단일 인스턴스라면 pessimistic이 더 저렴.
 */
@Component
public class DistributedLockJoinStrategy implements JoinStrategy {

    private static final long WAIT_SECONDS = 3L;
    private static final long LEASE_SECONDS = 5L;

    private final RedissonClient redisson;
    private final MeetupRepository meetupRepository;
    private final JoinSupport support;
    /** 트랜잭션 프록시 경유용 self */
    private final DistributedLockJoinStrategy self;

    public DistributedLockJoinStrategy(
            RedissonClient redisson,
            MeetupRepository meetupRepository,
            JoinSupport support,
            @Lazy DistributedLockJoinStrategy self) {
        this.redisson = redisson;
        this.meetupRepository = meetupRepository;
        this.support = support;
        this.self = self;
    }

    @Override
    public JoinStrategyType type() {
        return JoinStrategyType.DISTRIBUTED_LOCK;
    }

    /**
     * 트랜잭션 밖에서 락을 잡고, 락 안에서 트랜잭션을 연다.
     * 순서가 반대면 트랜잭션 커밋 전에 락이 해제되어 정합성이 깨질 수 있다.
     */
    @Override
    public Long join(Long meetupId, Long userId) {
        RLock lock = redisson.getLock("meetup:join:" + meetupId);
        boolean acquired;
        try {
            acquired = lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.JOIN_LOCK_FAILED);
        }
        if (!acquired) {
            throw new BusinessException(ErrorCode.JOIN_LOCK_FAILED);
        }
        try {
            return self.doJoinInsideLock(meetupId, userId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long doJoinInsideLock(Long meetupId, Long userId) {
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
