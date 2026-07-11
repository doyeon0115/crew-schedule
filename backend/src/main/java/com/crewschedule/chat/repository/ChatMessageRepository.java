package com.crewschedule.chat.repository;

import com.crewschedule.chat.domain.ChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** 최신순 페이지. 초기 히스토리 로딩과 위쪽 스크롤 페이지네이션에 사용. */
    @Query("select m from ChatMessage m join fetch m.sender where m.crew.id = :crewId "
            + "and (:beforeId is null or m.id < :beforeId) order by m.id desc")
    List<ChatMessage> findPage(
            @Param("crewId") Long crewId, @Param("beforeId") Long beforeId, Pageable pageable);
}
