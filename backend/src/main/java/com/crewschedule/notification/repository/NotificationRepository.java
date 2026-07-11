package com.crewschedule.notification.repository;

import com.crewschedule.notification.domain.Notification;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n where n.userId = :userId "
            + "and (:beforeId is null or n.id < :beforeId) order by n.id desc")
    List<Notification> findPageByUserId(
            @Param("userId") Long userId, @Param("beforeId") Long beforeId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("update Notification n set n.readAt = CURRENT_TIMESTAMP "
            + "where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId);
}
