package com.crewschedule.notification.service;

import com.crewschedule.crew.repository.CrewMemberRepository;
import com.crewschedule.notification.domain.NotificationType;
import com.crewschedule.notification.event.NotificationEvent;
import com.crewschedule.notification.kafka.NotificationPublisher;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 도메인 서비스가 호출하는 알림 진입점.
 *
 * <p>수신자 확정 → Kafka 이벤트 N건 발행. Fan-out은 producer 쪽에서 완료하므로 컨슈머는 저장·전송만.
 */
@Service
public class NotificationDispatcher {

    private final NotificationPublisher publisher;
    private final CrewMemberRepository crewMemberRepository;

    public NotificationDispatcher(
            NotificationPublisher publisher, CrewMemberRepository crewMemberRepository) {
        this.publisher = publisher;
        this.crewMemberRepository = crewMemberRepository;
    }

    /**
     * 크루 전원(제외 목록 제외)에게 알림 발행.
     * 보통 actor 본인은 excludeUserIds에 넣어서 자기 자신에게는 안 오게 함.
     */
    public void dispatchToCrew(
            NotificationType type,
            Long crewId,
            Long actorId,
            Set<Long> excludeUserIds,
            Map<String, Object> payload) {
        List<Long> recipients = crewMemberRepository.findAllWithUserByCrewId(crewId).stream()
                .map(cm -> cm.getUser().getId())
                .filter(id -> !excludeUserIds.contains(id))
                .collect(Collectors.toList());
        Instant now = Instant.now();
        for (Long uid : recipients) {
            publisher.publish(new NotificationEvent(type, uid, crewId, actorId, payload, now));
        }
    }

    public void dispatchToUser(
            NotificationType type,
            Long recipientId,
            Long crewId,
            Long actorId,
            Map<String, Object> payload) {
        publisher.publish(new NotificationEvent(
                type, recipientId, crewId, actorId, payload, Instant.now()));
    }

    /** 특정 유저 목록에게 발행. 참여자 대상 알림에 사용. */
    public void dispatchToUsers(
            NotificationType type,
            List<Long> recipients,
            Long crewId,
            Long actorId,
            Map<String, Object> payload) {
        Instant now = Instant.now();
        for (Long uid : recipients) {
            publisher.publish(new NotificationEvent(type, uid, crewId, actorId, payload, now));
        }
    }
}
