package com.crewschedule.notification.kafka;

import com.crewschedule.notification.domain.Notification;
import com.crewschedule.notification.dto.NotificationDtos.NotificationResponse;
import com.crewschedule.notification.event.NotificationEvent;
import com.crewschedule.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka {@code crew-schedule.notifications} 컨슈머.
 * 이벤트를 DB에 저장하고 수신자가 온라인이면 STOMP {@code /user/queue/notifications}로 전송.
 */
@Slf4j
@Component
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = NotificationTopics.NOTIFICATIONS,
            containerFactory = "notificationKafkaListenerContainerFactory")
    @Transactional
    public void consume(NotificationEvent event) {
        String payloadJson = serialize(event);
        Notification saved = notificationRepository.save(Notification.builder()
                .userId(event.recipientUserId())
                .type(event.type())
                .crewId(event.crewId())
                .actorId(event.actorUserId())
                .payload(payloadJson)
                .build());

        // 온라인 세션에 실시간 전달. 오프라인이면 그냥 DB에 남아 다음 조회 때 보인다.
        NotificationResponse response = NotificationResponse.from(saved);
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(event.recipientUserId()),
                    "/queue/notifications",
                    response);
        } catch (Exception e) {
            log.warn("STOMP delivery failed for user {}: {}", event.recipientUserId(), e.getMessage());
        }
    }

    private String serialize(NotificationEvent event) {
        try {
            return objectMapper.writeValueAsString(event.payload());
        } catch (JsonProcessingException e) {
            log.warn("payload serialization failed, storing empty object: {}", e.getMessage());
            return "{}";
        }
    }
}
