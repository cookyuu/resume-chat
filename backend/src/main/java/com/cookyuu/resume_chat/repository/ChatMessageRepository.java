package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
    long countBySessionAndReadStatusFalse(ChatSession session);

    // 페이지네이션 지원
    Page<ChatMessage> findBySessionOrderByCreatedAtDesc(ChatSession session, Pageable pageable);
    Page<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session, Pageable pageable);

    // 증분 조회 (timestamp 기반)
    List<ChatMessage> findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(ChatSession session, LocalDateTime timestamp);

    // 증분 조회 (messageId 기반)
    List<ChatMessage> findBySessionAndIdGreaterThanOrderByCreatedAtAsc(ChatSession session, Long messageId);
}
