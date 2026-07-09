package com.crewschedule.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.crewschedule.schedule.service.AvailabilityCalculator.DayAvailability;
import com.crewschedule.schedule.service.AvailabilityCalculator.EffectiveSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AvailabilityCalculatorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 16);

    private static EffectiveSlot work(String start, String end) {
        return new EffectiveSlot(false, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    @DisplayName("전원 가용 시각은 근무 멤버 중 가장 늦은 퇴근 시각이다")
    void availableFromIsLatestEndTime() {
        DayAvailability result = AvailabilityCalculator.calculate(
                DATE, List.of(EffectiveSlot.OFF, work("09:00", "17:00"), work("10:00", "16:00")));

        assertThat(result.availableFrom()).isEqualTo(LocalTime.of(17, 0));
        assertThat(result.offCount()).isEqualTo(1);
        assertThat(result.allDayFree()).isFalse();
    }

    @Test
    @DisplayName("전원 휴무면 하루 종일 가능하다")
    void allOffMeansAllDayFree() {
        DayAvailability result =
                AvailabilityCalculator.calculate(DATE, List.of(EffectiveSlot.OFF, EffectiveSlot.OFF));

        assertThat(result.allDayFree()).isTrue();
        assertThat(result.offCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("멤버가 없으면 계산할 수 없다")
    void emptyMembersRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> AvailabilityCalculator.calculate(DATE, List.of()));
    }

    @Test
    @DisplayName("순위는 전원 가용 시각이 빠른 날이 먼저, 동률이면 휴무 인원이 많은 날이 먼저다")
    void rankByAvailableFromThenOffCount() {
        DayAvailability lateDay = AvailabilityCalculator.calculate(
                DATE, List.of(work("09:00", "22:00"), EffectiveSlot.OFF));
        DayAvailability earlyDay = AvailabilityCalculator.calculate(
                DATE.plusDays(1), List.of(work("09:00", "17:00"), work("09:00", "15:00")));
        DayAvailability earlyDayMoreOff = AvailabilityCalculator.calculate(
                DATE.plusDays(2), List.of(work("09:00", "17:00"), EffectiveSlot.OFF));

        int[] ranks = AvailabilityCalculator.rank(List.of(lateDay, earlyDay, earlyDayMoreOff));

        assertThat(ranks[0]).isEqualTo(3); // 22:00 — 최하위
        assertThat(ranks[1]).isEqualTo(2); // 17:00, 휴무 0명
        assertThat(ranks[2]).isEqualTo(1); // 17:00, 휴무 1명 — 최상위
    }

    @Test
    @DisplayName("전원 휴무일은 어떤 근무일보다 상위다")
    void allDayFreeRanksFirst() {
        DayAvailability workDay = AvailabilityCalculator.calculate(
                DATE, List.of(work("09:00", "15:00"), work("09:00", "15:00")));
        DayAvailability freeDay =
                AvailabilityCalculator.calculate(DATE.plusDays(1), List.of(EffectiveSlot.OFF, EffectiveSlot.OFF));

        int[] ranks = AvailabilityCalculator.rank(List.of(workDay, freeDay));

        assertThat(ranks[1]).isEqualTo(1);
        assertThat(ranks[0]).isEqualTo(2);
    }
}
