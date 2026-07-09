package com.crewschedule.schedule.dto;

import com.crewschedule.schedule.domain.WeeklySlot;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 스케줄/추천 API 요청·응답 DTO 모음. */
public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    public record SlotRequest(
            @NotNull DayOfWeek dayOfWeek, boolean off, LocalTime startTime, LocalTime endTime) {
    }

    public record SlotResponse(DayOfWeek dayOfWeek, boolean off, LocalTime startTime, LocalTime endTime) {

        public static SlotResponse from(WeeklySlot slot) {
            return new SlotResponse(slot.getDayOfWeek(), slot.isOff(), slot.getStartTime(), slot.getEndTime());
        }
    }

    public record WeeklyScheduleResponse(Long userId, List<SlotResponse> slots) {
    }

    public record MemberScheduleResponse(
            Long userId, String nickname, String profileImageUrl, List<SlotResponse> slots) {
    }

    public record CrewScheduleBoardResponse(Long crewId, List<MemberScheduleResponse> members) {
    }

    /**
     * 하루 단위 추천 결과.
     *
     * @param availableFrom 전원이 가능해지는 시각. {@code allDayFree}면 null.
     * @param rank 1이 가장 추천(전원 가용 시각이 빠른 순, 동률이면 휴무 인원 많은 순).
     */
    public record DayRecommendationResponse(
            LocalDate date,
            DayOfWeek dayOfWeek,
            boolean allDayFree,
            LocalTime availableFrom,
            int offCount,
            int rank) {
    }
}
