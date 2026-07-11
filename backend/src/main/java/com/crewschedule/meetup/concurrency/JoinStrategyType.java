package com.crewschedule.meetup.concurrency;

/** Phase 4 참여 락 전략. application.yml의 app.concurrency.join-strategy로 선택. */
public enum JoinStrategyType {
    /** 락 없이 count → save. 오버부킹 발생을 시연하는 baseline. */
    NAIVE,
    /** @Version 낙관적 락 + 충돌 시 재시도. */
    OPTIMISTIC,
    /** SELECT ... FOR UPDATE 비관적 락. */
    PESSIMISTIC,
    /** Redisson RLock으로 다중 인스턴스 대응. */
    DISTRIBUTED_LOCK,
    /** Redis DECR을 게이트로 사용해 DB 부하 최소화. */
    REDIS_ATOMIC
}
