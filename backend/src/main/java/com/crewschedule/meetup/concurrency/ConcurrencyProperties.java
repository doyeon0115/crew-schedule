package com.crewschedule.meetup.concurrency;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.concurrency.*} 설정. 어떤 참여 락 전략을 쓸지 스위칭. */
@ConfigurationProperties(prefix = "app.concurrency")
public record ConcurrencyProperties(JoinStrategyType joinStrategy, int optimisticRetryMax) {

    public ConcurrencyProperties {
        if (joinStrategy == null) joinStrategy = JoinStrategyType.PESSIMISTIC;
        if (optimisticRetryMax <= 0) optimisticRetryMax = 5;
    }
}
