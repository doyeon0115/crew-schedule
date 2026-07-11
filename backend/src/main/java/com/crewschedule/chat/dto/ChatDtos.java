package com.crewschedule.chat.dto;

import com.crewschedule.chat.domain.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ChatDtos {

    private ChatDtos() {}

    public record SendMessageRequest(@NotBlank @Size(max = 2000) String content) {}

    public record ChatMessageResponse(
            Long id,
            Long crewId,
            Long senderId,
            String senderNickname,
            String content,
            LocalDateTime sentAt) {

        public static ChatMessageResponse from(ChatMessage m) {
            return new ChatMessageResponse(
                    m.getId(),
                    m.getCrew().getId(),
                    m.getSender().getId(),
                    m.getSender().getNickname(),
                    m.getContent(),
                    m.getCreatedAt());
        }
    }

    public record ChatHistoryResponse(List<ChatMessageResponse> messages, Long nextBeforeId) {}
}
