package com.crewschedule.poll.repository;

import com.crewschedule.poll.domain.DatePoll;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatePollRepository extends JpaRepository<DatePoll, Long> {

    List<DatePoll> findAllByCrewIdOrderByCreatedAtDesc(Long crewId);
}
