package com.crewschedule.notification.event;

import com.crewschedule.notification.domain.NotificationType;
import java.time.Instant;
import java.util.Map;

/**
 * Kafka 페이로드. 이벤트 하나 = 수신자 한 명 = DB 레코드 하나.
 *
 * <p>Fan-out은 producer 쪽(디스패처)에서 크루 멤버 목록을 확장해 이벤트 N건을 발행하는 방식.
 * 컨슈머는 단순히 저장 + STOMP 개인 큐로 전송한다.
 */
public record NotificationEvent(
        NotificationType type,
        Long recipientUserId,
        Long crewId,
        Long actorUserId,
        Map<String, Object> payload,
        Instant occurredAt) {
}
