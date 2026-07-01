package com.crewschedule;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Phase 0 스모크 테스트. 외부 인프라(MySQL/Redis/Kafka) 없이도 통과하도록 최소화했다.
 *
 * <p>TODO(Phase 1): Testcontainers 기반 {@code @SpringBootTest} 컨텍스트 로드 테스트로 확장.
 */
class CrewScheduleBackendApplicationTests {

    @Test
    void sanity() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
