package com.crewschedule.crew.repository;

import com.crewschedule.crew.domain.Crew;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrewRepository extends JpaRepository<Crew, Long> {

    Optional<Crew> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
