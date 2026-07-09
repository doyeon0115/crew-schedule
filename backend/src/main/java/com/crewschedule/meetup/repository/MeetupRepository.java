package com.crewschedule.meetup.repository;

import com.crewschedule.meetup.domain.Meetup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetupRepository extends JpaRepository<Meetup, Long> {

    List<Meetup> findAllByCrewIdOrderByMeetDateAscStartTimeAsc(Long crewId);
}
