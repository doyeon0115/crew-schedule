package com.crewschedule.chat.controller;

import com.crewschedule.auth.security.AuthPrincipal;
import com.crewschedule.chat.dto.ChatDtos.SendMessageRequest;
import com.crewschedule.chat.service.ChatService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/**
 * STOMP {@code SEND /app/crews/{crewId}/chat} 처리. 저장 + Redis 발행은 서비스가 담당.
 *
 * <p>브로커로 직접 리턴하지 않는 이유: 로컬 인스턴스만 아니라 모든 인스턴스 구독자에게 도달해야 하므로
 * 반드시 Redis Pub/Sub을 경유한다.
 */
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;

    @MessageMapping("/crews/{crewId}/chat")
    public void send(
            @DestinationVariable Long crewId,
            @Payload SendMessageRequest request,
            Principal principal) {
        AuthPrincipal auth = extract(principal);
        chatService.send(auth.userId(), crewId, request);
    }

    private AuthPrincipal extract(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof AuthPrincipal p) {
            return p;
        }
        throw new IllegalStateException("STOMP principal not set");
    }
}
