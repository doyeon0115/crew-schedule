package com.crewschedule.chat.service;

import com.crewschedule.chat.dto.ChatDtos.ChatMessageResponse;

/** Redis Pub/Sub 채널 규약 + 팬아웃 페이로드 형태. */
public final class ChatBroadcast {

    public static final String CHANNEL_PATTERN = "chat:crew:*";

    private ChatBroadcast() {}

    public static String channel(Long crewId) {
        return "chat:crew:" + crewId;
    }

    /** Redis payload wrapper — 그대로 STOMP 브로커로 전송된다. */
    public record Envelope(Long crewId, ChatMessageResponse message) {}
}
