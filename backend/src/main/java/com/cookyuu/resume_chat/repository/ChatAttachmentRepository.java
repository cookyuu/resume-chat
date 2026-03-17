package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.domain.ChatAttachment;
import com.cookyuu.resume_chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅 첨부파일 Repository
 */
@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {

    /**
     * Attachment ID로 첨부파일 조회
     *
     * @param attachmentId 첨부파일 UUID
     * @return Optional<ChatAttachment>
     */
    Optional<ChatAttachment> findByAttachmentId(UUID attachmentId);

    /**
     * 특정 메시지의 모든 첨부파일 조회
     *
     * @param message 채팅 메시지
     * @return 첨부파일 목록
     */
    List<ChatAttachment> findByMessage(ChatMessage message);

    /**
     * 특정 메시지의 첨부파일 개수 조회
     *
     * @param message 채팅 메시지
     * @return 첨부파일 개수
     */
    long countByMessage(ChatMessage message);
}
