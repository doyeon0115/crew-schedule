package com.crewschedule.chat.service;

import com.crewschedule.chat.service.ChatBroadcast.Envelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub {@code chat:crew:*} 를 구독해서 수신 페이로드를 STOMP {@code /topic/crews/{id}}로 재전송.
 * 모든 인스턴스가 이 리스너를 갖고 있어, 어느 서버에서 발행하든 모든 서버의 구독자에게 전달된다.
 */
@Slf4j
@Component
public class ChatSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public ChatSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Envelope envelope = objectMapper.readValue(message.getBody(), Envelope.class);
            messagingTemplate.convertAndSend("/topic/crews/" + envelope.crewId(), envelope.message());
        } catch (Exception e) {
            log.warn("failed to relay chat broadcast: {}", e.getMessage());
        }
    }
}
