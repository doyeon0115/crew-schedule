package com.crewschedule.notification.kafka;

import com.crewschedule.notification.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Kafka로 알림 이벤트를 발행. 실패는 로그만 남기고 서비스 흐름을 막지 않는다. */
@Slf4j
@Component
public class NotificationPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafka;

    public NotificationPublisher(KafkaTemplate<String, NotificationEvent> kafka) {
        this.kafka = kafka;
    }

    /** 파티션 키를 recipientUserId로 잡아 같은 유저 이벤트는 순서 보장. */
    public void publish(NotificationEvent event) {
        try {
            kafka.send(NotificationTopics.NOTIFICATIONS, String.valueOf(event.recipientUserId()), event);
        } catch (Exception e) {
            log.warn("Kafka publish failed: {}", e.getMessage());
        }
    }
}
