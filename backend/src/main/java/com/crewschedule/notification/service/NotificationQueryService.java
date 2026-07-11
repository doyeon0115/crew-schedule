package com.crewschedule.notification.service;

import com.crewschedule.notification.domain.Notification;
import com.crewschedule.notification.dto.NotificationDtos.NotificationPage;
import com.crewschedule.notification.dto.NotificationDtos.NotificationResponse;
import com.crewschedule.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 알림 목록·읽음 처리. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;

    public NotificationPage list(Long userId, Long beforeId, Integer size) {
        int pageSize = Math.min(size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        List<Notification> rows = notificationRepository.findPageByUserId(
                userId, beforeId, PageRequest.of(0, pageSize));
        Long nextBeforeId = rows.size() < pageSize ? null : rows.get(rows.size() - 1).getId();
        long unread = notificationRepository.countByUserIdAndReadAtIsNull(userId);
        return new NotificationPage(
                rows.stream().map(NotificationResponse::from).toList(), nextBeforeId, unread);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }
}
