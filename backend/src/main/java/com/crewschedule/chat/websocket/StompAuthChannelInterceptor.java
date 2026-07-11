package com.crewschedule.chat.websocket;

import com.crewschedule.auth.jwt.JwtTokenProvider;
import com.crewschedule.auth.security.AuthPrincipal;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.crew.repository.CrewMemberRepository;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT에서 JWT 검증 → principal 세팅, SUBSCRIBE에서 크루 멤버십 검증.
 * SEND는 컨트롤러/서비스가 서비스 계층에서 다시 검증하므로 여기서는 통과.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";
    private static final Pattern CREW_TOPIC = Pattern.compile("^/topic/crews/(\\d+)$");

    private final JwtTokenProvider tokenProvider;
    private final CrewMemberRepository crewMemberRepository;

    public StompAuthChannelInterceptor(
            JwtTokenProvider tokenProvider, CrewMemberRepository crewMemberRepository) {
        this.tokenProvider = tokenProvider;
        this.crewMemberRepository = crewMemberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = firstHeader(accessor);
        if (header == null || !header.startsWith(BEARER)) {
            throw new IllegalArgumentException("Missing Authorization header");
        }
        String token = header.substring(BEARER.length()).trim();
        AuthPrincipal principal;
        try {
            principal = tokenProvider.parseAccess(token);
        } catch (BusinessException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
        accessor.setUser(auth);
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;
        Matcher m = CREW_TOPIC.matcher(destination);
        if (!m.matches()) return; // 그 외 destination은 여기선 관여하지 않음
        Long crewId = Long.parseLong(m.group(1));
        AuthPrincipal principal = principalFrom(accessor);
        if (!crewMemberRepository.existsByCrewIdAndUserId(crewId, principal.userId())) {
            throw new AccessDeniedException("not a crew member: " + crewId);
        }
    }

    private AuthPrincipal principalFrom(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication auth
                && auth.getPrincipal() instanceof AuthPrincipal p) {
            return p;
        }
        throw new AccessDeniedException("session not authenticated");
    }

    private String firstHeader(StompHeaderAccessor accessor) {
        List<String> vals = accessor.getNativeHeader(HttpHeaders.AUTHORIZATION);
        return vals == null || vals.isEmpty() ? null : vals.get(0);
    }
}
