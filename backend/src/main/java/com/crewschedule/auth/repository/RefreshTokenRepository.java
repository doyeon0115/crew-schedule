package com.crewschedule.auth.repository;

import com.crewschedule.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken r where r.tokenHash = :hash")
    void deleteByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("delete from RefreshToken r where r.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
