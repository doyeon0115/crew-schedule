package com.crewschedule.notification.dto;

import com.crewschedule.notification.domain.Notification;
import com.crewschedule.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.List;

public final class NotificationDtos {

    private NotificationDtos() {}

    public record NotificationResponse(
            Long id,
            NotificationType type,
            Long crewId,
            Long actorId,
            String payload,
            boolean read,
            LocalDateTime createdAt) {

        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(),
                    n.getType(),
                    n.getCrewId(),
                    n.getActorId(),
                    n.getPayload(),
                    !n.isUnread(),
                    n.getCreatedAt());
        }
    }

    public record NotificationPage(
            List<NotificationResponse> notifications,
            Long nextBeforeId,
            long unreadCount) {}
}
