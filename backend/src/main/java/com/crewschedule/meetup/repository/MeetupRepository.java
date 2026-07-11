package com.crewschedule.meetup.repository;

import com.crewschedule.meetup.domain.Meetup;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetupRepository extends JpaRepository<Meetup, Long> {

    List<Meetup> findAllByCrewIdOrderByMeetDateAscStartTimeAsc(Long crewId);

    /** SELECT ... FOR UPDATE. 비관적 락 전략에서 사용. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Meetup m where m.id = :id")
    Optional<Meetup> findByIdForUpdate(@Param("id") Long id);
}
