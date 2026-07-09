package com.crewschedule.schedule.repository;

import com.crewschedule.schedule.domain.ScheduleException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Long> {

    List<ScheduleException> findAllByUserIdInAndDateBetween(
            Collection<Long> userIds, LocalDate startInclusive, LocalDate endInclusive);
}
