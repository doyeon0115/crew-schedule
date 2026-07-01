package com.crewschedule.common.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스캐폴딩 확인용 헬스 엔드포인트. Phase 1 이후 실제 도메인 컨트롤러로 대체/보강된다.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of(
                "service", "crew-schedule-backend",
                "status", "UP"
        ));
    }
}
