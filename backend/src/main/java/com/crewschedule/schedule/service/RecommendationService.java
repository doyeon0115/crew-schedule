package com.crewschedule.schedule.service;

import com.crewschedule.crew.domain.CrewMember;
import com.crewschedule.crew.repository.CrewMemberRepository;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.schedule.domain.ScheduleException;
import com.crewschedule.schedule.domain.WeeklySlot;
import com.crewschedule.schedule.dto.ScheduleDtos.DayRecommendationResponse;
import com.crewschedule.schedule.repository.ScheduleExceptionRepository;
import com.crewschedule.schedule.service.AvailabilityCalculator.DayAvailability;
import com.crewschedule.schedule.service.AvailabilityCalculator.EffectiveSlot;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 크루 공통 빈 시간 추천.
 *
 * <p>시작일부터 7일간, 멤버별 유효 스케줄(날짜 예외 > 주간 스케줄 > 미입력=휴무)을 종합해
 * 날짜마다 "전원이 가능해지는 시각"을 계산하고 추천 순위를 매긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private static final int RECOMMEND_DAYS = 7;

    private final CrewService crewService;
    private final CrewMemberRepository crewMemberRepository;
    private final ScheduleService scheduleService;
    private final ScheduleExceptionRepository scheduleExceptionRepository;

    public List<DayRecommendationResponse> recommend(Long userId, Long crewId, LocalDate startDate) {
        crewService.getCrewForMember(userId, crewId);

        List<Long> memberUserIds = crewMemberRepository.findAllWithUserByCrewId(crewId).stream()
                .map(CrewMember::getUser)
                .map(u -> u.getId())
                .toList();

        LocalDate endDate = startDate.plusDays(RECOMMEND_DAYS - 1);
        Map<Long, Map<DayOfWeek, WeeklySlot>> weeklyByUser = scheduleService.weeklySlotMap(memberUserIds);
        Map<Long, Map<LocalDate, ScheduleException>> exceptionsByUser =
                scheduleExceptionRepository.findAllByUserIdInAndDateBetween(memberUserIds, startDate, endDate).stream()
                        .collect(Collectors.groupingBy(
                                ex -> ex.getUser().getId(),
                                Collectors.toMap(ScheduleException::getDate, ex -> ex)));

        List<DayAvailability> days = new ArrayList<>(RECOMMEND_DAYS);
        for (int i = 0; i < RECOMMEND_DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            List<EffectiveSlot> slots = memberUserIds.stream()
                    .map(memberId -> effectiveSlot(memberId, date, weeklyByUser, exceptionsByUser))
                    .toList();
            days.add(AvailabilityCalculator.calculate(date, slots));
        }

        int[] ranks = AvailabilityCalculator.rank(days);
        List<DayRecommendationResponse> result = new ArrayList<>(RECOMMEND_DAYS);
        for (int i = 0; i < days.size(); i++) {
            DayAvailability day = days.get(i);
            result.add(new DayRecommendationResponse(
                    day.date(),
                    day.date().getDayOfWeek(),
                    day.allDayFree(),
                    day.allDayFree() ? null : day.availableFrom(),
                    day.offCount(),
                    ranks[i]));
        }
        return result;
    }

    private EffectiveSlot effectiveSlot(
            Long memberId,
            LocalDate date,
            Map<Long, Map<DayOfWeek, WeeklySlot>> weeklyByUser,
            Map<Long, Map<LocalDate, ScheduleException>> exceptionsByUser) {
        ScheduleException exception =
                exceptionsByUser.getOrDefault(memberId, Map.of()).get(date);
        if (exception != null) {
            return exception.isOff()
                    ? EffectiveSlot.OFF
                    : new EffectiveSlot(false, exception.getStartTime(), exception.getEndTime());
        }
        WeeklySlot weekly = weeklyByUser.getOrDefault(memberId, Map.of()).get(date.getDayOfWeek());
        if (weekly == null || weekly.isOff()) {
            return EffectiveSlot.OFF;
        }
        return new EffectiveSlot(false, weekly.getStartTime(), weekly.getEndTime());
    }
}
