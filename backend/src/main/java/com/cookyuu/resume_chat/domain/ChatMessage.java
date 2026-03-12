package com.cookyuu.resume_chat.domain;

import com.cookyuu.resume_chat.common.domain.BaseTimeEntity;
import com.cookyuu.resume_chat.common.enums.MessageType;
import com.cookyuu.resume_chat.common.enums.SenderType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "rc_chat_message", indexes = {
        @Index(name = "idx_chat_message_session_created", columnList = "session_id, created_at"),
        @Index(name = "idx_chat_message_session_read_status", columnList = "session_id, read_status")
})
public class ChatMessage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Column(name = "read_status", nullable = false)
    private boolean readStatus;

    public static ChatMessage createMessage(ChatSession session, SenderType senderType, String content) {
        return ChatMessage.builder()
                .messageId(UUID.randomUUID())
                .session(session)
                .senderType(senderType)
                .messageType(MessageType.TEXT) // 기본값: TEXT
                .content(content)
                .readStatus(false)
                .build();
    }

    public static ChatMessage createMessage(ChatSession session, SenderType senderType, MessageType messageType, String content) {
        return ChatMessage.builder()
                .messageId(UUID.randomUUID())
                .session(session)
                .senderType(senderType)
                .messageType(messageType)
                .content(content)
                .readStatus(false)
                .build();
    }

    public void markAsRead() {
        this.readStatus = true;
    }
}
