package com.crewschedule.meetup.repository;

import com.crewschedule.meetup.domain.MeetupParticipant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetupParticipantRepository extends JpaRepository<MeetupParticipant, Long> {

    Optional<MeetupParticipant> findByMeetupIdAndUserId(Long meetupId, Long userId);

    @Query("select p from MeetupParticipant p join fetch p.user where p.meetup.id in :meetupIds order by p.id")
    List<MeetupParticipant> findAllWithUserByMeetupIdIn(@Param("meetupIds") Collection<Long> meetupIds);

    boolean existsByMeetupIdAndUserId(Long meetupId, Long userId);

    long countByMeetupId(Long meetupId);
}
