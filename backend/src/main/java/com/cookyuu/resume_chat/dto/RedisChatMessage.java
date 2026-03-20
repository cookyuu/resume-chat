package com.cookyuu.resume_chat.dto;

import com.cookyuu.resume_chat.common.enums.MessageType;
import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Redis 캐싱용 채팅 메시지 DTO
 *
 * <p>MySQL ChatMessage 엔티티를 Redis에 저장하기 위한 직렬화 가능한 DTO</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>Redis 저장을 위한 Serializable 구현</li>
 *   <li>ChatMessage 엔티티와 상호 변환</li>
 *   <li>연관관계 없이 단순 필드만 포함</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID messageId;
    private String sessionToken;
    private SenderType senderType;
    private MessageType messageType;
    private String content;
    private LocalDateTime createdAt;
    private boolean readStatus;

    /**
     * MySQL ChatMessage 엔티티로 변환
     *
     * @param session 채팅 세션 (연관관계 설정용)
     * @return ChatMessage 엔티티
     */
    public ChatMessage toEntity(ChatSession session) {
        return ChatMessage.builder()
                .messageId(messageId)
                .session(session)
                .senderType(senderType)
                .messageType(messageType)
                .content(content)
                .readStatus(readStatus)
                .build();
    }

    /**
     * MySQL ChatMessage에서 Redis DTO 생성
     *
     * @param message ChatMessage 엔티티
     * @return RedisChatMessage DTO
     */
    public static RedisChatMessage from(ChatMessage message) {
        return RedisChatMessage.builder()
                .messageId(message.getMessageId())
                .sessionToken(message.getSession().getSessionToken())
                .senderType(message.getSenderType())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .readStatus(message.isReadStatus())
                .build();
    }
}
