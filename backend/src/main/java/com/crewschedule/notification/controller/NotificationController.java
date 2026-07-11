package com.crewschedule.notification.controller;

import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import com.crewschedule.notification.dto.NotificationDtos.NotificationPage;
import com.crewschedule.notification.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "내 알림 목록·읽음 처리 (Kafka 컨슈머가 저장)")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationService;

    @Operation(summary = "내 알림 (최신순 페이지, unreadCount 포함)")
    @GetMapping
    public ApiResponse<NotificationPage> list(
            @CurrentUserId Long userId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(notificationService.list(userId, beforeId, size));
    }

    @Operation(summary = "모두 읽음 처리")
    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead(@CurrentUserId Long userId) {
        return ApiResponse.ok(Map.of("updated", notificationService.markAllRead(userId)));
    }
}
