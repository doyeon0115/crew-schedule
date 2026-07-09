package com.crewschedule.schedule.controller;

import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import com.crewschedule.schedule.dto.ScheduleDtos.CrewScheduleBoardResponse;
import com.crewschedule.schedule.dto.ScheduleDtos.DayRecommendationResponse;
import com.crewschedule.schedule.dto.ScheduleDtos.SlotRequest;
import com.crewschedule.schedule.dto.ScheduleDtos.WeeklyScheduleResponse;
import com.crewschedule.schedule.service.RecommendationService;
import com.crewschedule.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Schedule", description = "주간 스케줄 관리 및 공통 빈 시간 추천")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final RecommendationService recommendationService;

    @Operation(summary = "내 주간 스케줄 조회")
    @GetMapping("/me/schedule")
    public ApiResponse<WeeklyScheduleResponse> mySchedule(@CurrentUserId Long userId) {
        return ApiResponse.ok(scheduleService.getMySchedule(userId));
    }

    @Operation(summary = "내 주간 스케줄 수정", description = "요청에 포함된 요일만 갱신(upsert)한다.")
    @PutMapping("/me/schedule")
    public ApiResponse<WeeklyScheduleResponse> updateMySchedule(
            @CurrentUserId Long userId, @Valid @RequestBody List<SlotRequest> requests) {
        return ApiResponse.ok(scheduleService.updateMySchedule(userId, requests));
    }

    @Operation(summary = "크루 스케줄 보드", description = "크루 멤버 전원의 주간 스케줄을 반환한다.")
    @GetMapping("/crews/{crewId}/schedules")
    public ApiResponse<CrewScheduleBoardResponse> crewBoard(
            @CurrentUserId Long userId, @PathVariable Long crewId) {
        return ApiResponse.ok(scheduleService.getCrewBoard(userId, crewId));
    }

    @Operation(
            summary = "공통 빈 시간 추천",
            description = "시작일(기본 오늘)부터 7일간 전원 가용 시각과 추천 순위(rank=1이 최선)를 계산한다.")
    @GetMapping("/crews/{crewId}/recommendations")
    public ApiResponse<List<DayRecommendationResponse>> recommendations(
            @CurrentUserId Long userId,
            @PathVariable Long crewId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now();
        return ApiResponse.ok(recommendationService.recommend(userId, crewId, effectiveStart));
    }
}
