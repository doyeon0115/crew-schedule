package com.crewschedule.chat.controller;

import com.crewschedule.chat.dto.ChatDtos.ChatHistoryResponse;
import com.crewschedule.chat.dto.ChatDtos.ChatMessageResponse;
import com.crewschedule.chat.dto.ChatDtos.SendMessageRequest;
import com.crewschedule.chat.service.ChatService;
import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** WebSocket 미지원 환경/초기 로딩용 REST 히스토리 · 전송 fallback. */
@Tag(name = "Chat", description = "크루 채팅 히스토리 조회 · 메시지 전송 (REST)")
@RestController
@RequestMapping("/api/crews/{crewId}/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @Operation(summary = "채팅 히스토리 (최신순 페이지)")
    @GetMapping("/messages")
    public ApiResponse<ChatHistoryResponse> history(
            @CurrentUserId Long userId,
            @PathVariable Long crewId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(chatService.getHistory(userId, crewId, beforeId, size));
    }

    @Operation(summary = "채팅 전송 (REST fallback)")
    @PostMapping("/messages")
    public ApiResponse<ChatMessageResponse> send(
            @CurrentUserId Long userId,
            @PathVariable Long crewId,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(chatService.send(userId, crewId, request));
    }
}
