package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.entity.ChatMessage;
import com.cookyuu.resume_chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
    long countBySessionAndReadStatusFalse(ChatSession session);
}
