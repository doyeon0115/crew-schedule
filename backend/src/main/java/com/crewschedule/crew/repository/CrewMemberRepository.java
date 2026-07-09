package com.crewschedule.crew.repository;

import com.crewschedule.crew.domain.CrewMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    boolean existsByCrewIdAndUserId(Long crewId, Long userId);

    Optional<CrewMember> findByCrewIdAndUserId(Long crewId, Long userId);

    @Query("select cm from CrewMember cm join fetch cm.user where cm.crew.id = :crewId order by cm.id")
    List<CrewMember> findAllWithUserByCrewId(@Param("crewId") Long crewId);

    @Query("select cm from CrewMember cm join fetch cm.crew where cm.user.id = :userId order by cm.id")
    List<CrewMember> findAllWithCrewByUserId(@Param("userId") Long userId);

    long countByCrewId(Long crewId);
}
