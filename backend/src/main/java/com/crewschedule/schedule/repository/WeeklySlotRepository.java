package com.crewschedule.schedule.repository;

import com.crewschedule.schedule.domain.WeeklySlot;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklySlotRepository extends JpaRepository<WeeklySlot, Long> {

    List<WeeklySlot> findAllByUserId(Long userId);

    List<WeeklySlot> findAllByUserIdIn(Collection<Long> userIds);

    Optional<WeeklySlot> findByUserIdAndDayOfWeek(Long userId, DayOfWeek dayOfWeek);
}
