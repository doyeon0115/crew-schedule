package com.crewschedule.chat.service;

import com.crewschedule.chat.dto.ChatDtos.ChatMessageResponse;
import com.crewschedule.chat.service.ChatBroadcast.Envelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 메시지를 Redis Pub/Sub 채널로 발행. 다중 인스턴스가 각자 구독하고 STOMP로 재전송. */
@Component
public class ChatBroadcaster {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ChatBroadcaster(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void publish(Long crewId, ChatMessageResponse message) {
        try {
            String json = objectMapper.writeValueAsString(new Envelope(crewId, message));
            redis.convertAndSend(ChatBroadcast.channel(crewId), json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("chat payload serialization failed", e);
        }
    }
}
