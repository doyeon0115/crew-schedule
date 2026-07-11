package com.crewschedule.user.repository;

import com.crewschedule.user.domain.AuthProvider;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.domain.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    long countByStatus(UserStatus status);

    /** 관리자 목록. 이메일·닉네임 부분 검색. */
    @Query("select u from User u where "
            + "(:query is null or lower(u.email) like lower(concat('%', :query, '%')) "
            + " or lower(u.nickname) like lower(concat('%', :query, '%'))) "
            + "order by u.id desc")
    List<User> searchForAdmin(@Param("query") String query, Pageable pageable);
}
