package com.crewschedule.chat.service;

import com.crewschedule.chat.domain.ChatMessage;
import com.crewschedule.chat.dto.ChatDtos.ChatHistoryResponse;
import com.crewschedule.chat.dto.ChatDtos.ChatMessageResponse;
import com.crewschedule.chat.dto.ChatDtos.SendMessageRequest;
import com.crewschedule.chat.repository.ChatMessageRepository;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.domain.Crew;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 크루 채팅 저장/조회 + Redis Pub/Sub 팬아웃 트리거. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageRepository chatRepository;
    private final CrewService crewService;
    private final UserRepository userRepository;
    private final ChatBroadcaster broadcaster;

    @Transactional
    public ChatMessageResponse send(Long userId, Long crewId, SendMessageRequest request) {
        Crew crew = crewService.getCrewForMember(userId, crewId);
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        ChatMessage saved = chatRepository.save(ChatMessage.builder()
                .crew(crew)
                .sender(sender)
                .content(request.content())
                .build());
        ChatMessageResponse response = ChatMessageResponse.from(saved);
        broadcaster.publish(crewId, response);
        return response;
    }

    /** 최신순 페이지. beforeId 미만의 id만 반환(위쪽 스크롤 페이지네이션). */
    public ChatHistoryResponse getHistory(Long userId, Long crewId, Long beforeId, Integer size) {
        crewService.getCrewForMember(userId, crewId);
        int pageSize = Math.min(size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        List<ChatMessage> rows =
                chatRepository.findPage(crewId, beforeId, PageRequest.of(0, pageSize));
        Long nextBeforeId = rows.size() < pageSize
                ? null
                : rows.get(rows.size() - 1).getId();
        // 클라이언트 편의를 위해 오래된 → 최신 순으로 뒤집어 반환
        Collections.reverse(rows);
        return new ChatHistoryResponse(rows.stream().map(ChatMessageResponse::from).toList(), nextBeforeId);
    }
}
