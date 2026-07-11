package com.crewschedule.auth.security;

import com.crewschedule.user.domain.UserRole;
import java.security.Principal;

/**
 * SecurityContext / STOMP session의 principal.
 *
 * <p>{@link Principal#getName()}이 userId 문자열을 반환하도록 구현한다.
 * Spring의 {@code SimpMessagingTemplate.convertAndSendToUser(userId, ...)}가 이 이름으로 세션을 라우팅.
 */
public record AuthPrincipal(Long userId, UserRole role) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
