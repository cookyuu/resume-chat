package com.cookyuu.resume_chat.domain;

import com.cookyuu.resume_chat.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 채팅 첨부파일 엔티티
 *
 * <p>채팅 메시지에 첨부된 파일 정보를 저장합니다.</p>
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "rc_chat_attachment", indexes = {
        @Index(name = "idx_chat_attachment_message", columnList = "message_id")
})
public class ChatAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String mimeType;

    /**
     * 새 첨부파일 생성
     *
     * @param message 연결된 채팅 메시지
     * @param fileName 파일명
     * @param filePath 저장 경로
     * @param fileSize 파일 크기 (bytes)
     * @param mimeType MIME 타입
     * @return 생성된 ChatAttachment
     */
    public static ChatAttachment createAttachment(
            ChatMessage message,
            String fileName,
            String filePath,
            Long fileSize,
            String mimeType
    ) {
        return ChatAttachment.builder()
                .attachmentId(UUID.randomUUID())
                .message(message)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .build();
    }
}
