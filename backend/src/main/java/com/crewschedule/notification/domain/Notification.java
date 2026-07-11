package com.crewschedule.notification.domain;

import com.crewschedule.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저별 알림 한 건. Kafka 이벤트가 컨슈머에 의해 이 테이블로 저장된다. */
@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    private Long crewId;

    private Long actorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    private LocalDateTime readAt;

    @Builder
    private Notification(
            Long userId,
            NotificationType type,
            Long crewId,
            Long actorId,
            String payload) {
        this.userId = userId;
        this.type = type;
        this.crewId = crewId;
        this.actorId = actorId;
        this.payload = payload;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    public boolean isUnread() {
        return readAt == null;
    }
}
