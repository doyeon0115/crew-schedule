package com.crewschedule.meetup.concurrency.strategy;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.meetup.concurrency.JoinStrategy;
import com.crewschedule.meetup.concurrency.JoinStrategyType;
import com.crewschedule.meetup.concurrency.JoinSupport;
import com.crewschedule.meetup.domain.Meetup;
import com.crewschedule.meetup.repository.MeetupRepository;
import com.crewschedule.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis 원자 연산. 슬롯 확보는 Redis에서, DB에는 확보된 슬롯만큼만 참여자를 저장한다.
 *
 * <p>SETNX로 카운터를 정원값으로 초기화하고, DECR 결과가 0 이상이면 슬롯 획득.
 * 음수로 떨어지면 정원 초과이므로 INCR로 복구.
 * DB 부하가 최소화되어 처리량이 가장 높지만, Redis-DB 불일치가 발생하면 정합성이 흐트러진다.
 */
@Component
public class RedisAtomicJoinStrategy implements JoinStrategy {

    /** DECR 후 0 미만이면 정원 초과. 초과분은 INCR로 되돌린다. */
    private static final String RESERVE_SCRIPT = """
            local remaining = redis.call('DECR', KEYS[1])
            if remaining < 0 then
                redis.call('INCR', KEYS[1])
                return -1
            end
            return remaining
            """;

    private final StringRedisTemplate redis;
    private final MeetupRepository meetupRepository;
    private final JoinSupport support;
    private final DefaultRedisScript<Long> reserveScript;
    /** 트랜잭션 프록시 경유용 self */
    private final RedisAtomicJoinStrategy self;

    public RedisAtomicJoinStrategy(
            StringRedisTemplate redis,
            MeetupRepository meetupRepository,
            JoinSupport support,
            @Lazy RedisAtomicJoinStrategy self) {
        this.redis = redis;
        this.meetupRepository = meetupRepository;
        this.support = support;
        this.self = self;
        this.reserveScript = new DefaultRedisScript<>(RESERVE_SCRIPT, Long.class);
    }

    @Override
    public JoinStrategyType type() {
        return JoinStrategyType.REDIS_ATOMIC;
    }

    @Override
    public Long join(Long meetupId, Long userId) {
        // 1) 미리 DB에서 참여 여부와 정원 검증 (Redis 카운터를 낭비하지 않기 위한 shortcut)
        support.ensureNotJoined(meetupId, userId);

        // 2) Redis 슬롯 카운터 준비 및 확보
        String key = "meetup:remaining:" + meetupId;
        ensureCounterInitialized(key, meetupId);
        Long remaining = redis.execute(reserveScript, List.of(key));
        if (remaining == null || remaining < 0) {
            throw new BusinessException(ErrorCode.MEETUP_FULL);
        }

        // 3) DB에 참여자 저장. 실패하면 Redis 슬롯을 복구해야 정합성 유지.
        try {
            return self.doJoinAfterReserve(meetupId, userId);
        } catch (RuntimeException e) {
            redis.opsForValue().increment(key);
            throw e;
        }
    }

    /** Meetup 조회 후 Redis에 카운터가 없으면 capacity - currentParticipants로 초기화. */
    private void ensureCounterInitialized(String key, Long meetupId) {
        if (Boolean.TRUE.equals(redis.hasKey(key))) return;
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETUP_NOT_FOUND));
        support.ensureJoinable(meetup);
        int remaining = meetup.getCapacity() - meetup.getCurrentParticipants();
        // SETNX 세만틱: 이미 다른 스레드가 초기화했다면 덮어쓰지 않는다.
        redis.opsForValue().setIfAbsent(key, String.valueOf(Math.max(remaining, 0)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long doJoinAfterReserve(Long meetupId, Long userId) {
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETUP_NOT_FOUND));
        User user = support.loadUser(userId);
        // Redis가 게이팅했으므로 여기서는 검사 스킵. 카운터만 증가.
        meetup.incrementParticipantsWithoutCheck();
        return support.saveParticipant(meetup, user);
    }
}
