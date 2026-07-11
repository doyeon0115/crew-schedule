package com.crewschedule.meetup.concurrency;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 전략 타입 → 구현체 룩업. 스위칭·테스트에서 특정 전략을 직접 주입할 때 사용. */
@Component
public class JoinStrategyRegistry {

    private final Map<JoinStrategyType, JoinStrategy> strategies;

    public JoinStrategyRegistry(List<JoinStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(JoinStrategy::type, s -> s));
    }

    public JoinStrategy get(JoinStrategyType type) {
        JoinStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("Unknown join strategy: " + type);
        }
        return strategy;
    }
}
