package com.crewschedule.schedule.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * 크루원들의 하루 유효 스케줄을 종합해 "다 같이 가능한 시간"을 계산하는 순수 로직.
 *
 * <p>규칙: 휴무인 멤버는 하루 종일 가능, 근무인 멤버는 퇴근 시각부터 가능.
 * 전원 가용 시각은 멤버별 가용 시각 중 가장 늦은 값이다.
 */
public final class AvailabilityCalculator {

    private AvailabilityCalculator() {
    }

    /** 하루의 유효 스케줄. 예외 일정 > 주간 스케줄 순으로 결정되며, 미입력은 휴무로 취급한다. */
    public record EffectiveSlot(boolean off, LocalTime startTime, LocalTime endTime) {

        public static final EffectiveSlot OFF = new EffectiveSlot(true, null, null);

        LocalTime availableFrom() {
            return off ? LocalTime.MIN : endTime;
        }
    }

    /** 하루 종합 결과. {@code availableFrom}이 {@link LocalTime#MIN}이면 전원 하루 종일 가능. */
    public record DayAvailability(LocalDate date, LocalTime availableFrom, int offCount) {

        public boolean allDayFree() {
            return LocalTime.MIN.equals(availableFrom);
        }
    }

    public static DayAvailability calculate(LocalDate date, List<EffectiveSlot> memberSlots) {
        if (memberSlots.isEmpty()) {
            throw new IllegalArgumentException("멤버 스케줄이 비어 있습니다.");
        }
        LocalTime availableFrom = memberSlots.stream()
                .map(EffectiveSlot::availableFrom)
                .max(Comparator.naturalOrder())
                .orElse(LocalTime.MIN);
        int offCount = (int) memberSlots.stream().filter(EffectiveSlot::off).count();
        return new DayAvailability(date, availableFrom, offCount);
    }

    /**
     * 여러 날의 결과에 추천 순위를 매긴다. 전원 가용 시각이 빠를수록,
     * 동률이면 휴무 인원이 많을수록 상위다. 반환 배열 인덱스는 입력 순서와 같고 값은 1부터 시작하는 순위다.
     */
    public static int[] rank(List<DayAvailability> days) {
        List<DayAvailability> sorted = days.stream()
                .sorted(Comparator.comparing(DayAvailability::availableFrom)
                        .thenComparing(Comparator.comparingInt(DayAvailability::offCount).reversed()))
                .toList();
        int[] ranks = new int[days.size()];
        for (int i = 0; i < days.size(); i++) {
            ranks[i] = sorted.indexOf(days.get(i)) + 1;
        }
        return ranks;
    }
}
