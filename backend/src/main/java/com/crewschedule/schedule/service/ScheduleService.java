package com.crewschedule.schedule.service;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.domain.CrewMember;
import com.crewschedule.crew.repository.CrewMemberRepository;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.schedule.domain.WeeklySlot;
import com.crewschedule.schedule.dto.ScheduleDtos.CrewScheduleBoardResponse;
import com.crewschedule.schedule.dto.ScheduleDtos.MemberScheduleResponse;
import com.crewschedule.schedule.dto.ScheduleDtos.SlotRequest;
import com.crewschedule.schedule.dto.ScheduleDtos.SlotResponse;
import com.crewschedule.schedule.dto.ScheduleDtos.WeeklyScheduleResponse;
import com.crewschedule.schedule.repository.WeeklySlotRepository;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final WeeklySlotRepository weeklySlotRepository;
    private final UserRepository userRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewService crewService;

    /** 내 주간 스케줄 조회. 입력하지 않은 요일은 응답에 포함되지 않는다. */
    public WeeklyScheduleResponse getMySchedule(Long userId) {
        List<SlotResponse> slots = weeklySlotRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(WeeklySlot::getDayOfWeek))
                .map(SlotResponse::from)
                .toList();
        return new WeeklyScheduleResponse(userId, slots);
    }

    /** 내 주간 스케줄 upsert. 요청에 포함된 요일만 갱신한다. */
    @Transactional
    public WeeklyScheduleResponse updateMySchedule(Long userId, List<SlotRequest> requests) {
        validateNoDuplicateDays(requests);
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        for (SlotRequest request : requests) {
            weeklySlotRepository
                    .findByUserIdAndDayOfWeek(userId, request.dayOfWeek())
                    .ifPresentOrElse(
                            slot -> applyTo(slot, request),
                            () -> weeklySlotRepository.save(WeeklySlot.builder()
                                    .user(user)
                                    .dayOfWeek(request.dayOfWeek())
                                    .isOff(request.off())
                                    .startTime(request.startTime())
                                    .endTime(request.endTime())
                                    .build()));
        }
        return getMySchedule(userId);
    }

    /** 크루 스케줄 보드: 멤버 전원의 주간 스케줄. 크루 멤버만 조회할 수 있다. */
    public CrewScheduleBoardResponse getCrewBoard(Long userId, Long crewId) {
        crewService.getCrewForMember(userId, crewId);
        List<CrewMember> members = crewMemberRepository.findAllWithUserByCrewId(crewId);
        List<Long> userIds = members.stream().map(cm -> cm.getUser().getId()).toList();

        Map<Long, List<WeeklySlot>> slotsByUser = weeklySlotRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(slot -> slot.getUser().getId()));

        List<MemberScheduleResponse> memberSchedules = members.stream()
                .map(cm -> new MemberScheduleResponse(
                        cm.getUser().getId(),
                        cm.getUser().getNickname(),
                        cm.getUser().getProfileImageUrl(),
                        slotsByUser.getOrDefault(cm.getUser().getId(), List.of()).stream()
                                .sorted(Comparator.comparing(WeeklySlot::getDayOfWeek))
                                .map(SlotResponse::from)
                                .toList()))
                .toList();
        return new CrewScheduleBoardResponse(crewId, memberSchedules);
    }

    private void applyTo(WeeklySlot slot, SlotRequest request) {
        if (request.off()) {
            slot.markOff();
        } else {
            slot.updateWorkHours(request.startTime(), request.endTime());
        }
    }

    private void validateNoDuplicateDays(List<SlotRequest> requests) {
        long distinctDays = requests.stream().map(SlotRequest::dayOfWeek).distinct().count();
        if (distinctDays != requests.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "같은 요일이 중복되었습니다.");
        }
    }

    /** 요일별 유효 스케줄 조회용: userIds의 주간 슬롯을 (userId, dayOfWeek) 맵으로 반환. */
    Map<Long, Map<DayOfWeek, WeeklySlot>> weeklySlotMap(List<Long> userIds) {
        return weeklySlotRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(
                        slot -> slot.getUser().getId(),
                        Collectors.toMap(WeeklySlot::getDayOfWeek, slot -> slot)));
    }
}
