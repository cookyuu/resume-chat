package com.cookyuu.resume_chat.dto;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.entity.ChatMessage;
import com.cookyuu.resume_chat.entity.ChatSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        @NotBlank(message = "채용담당자 이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String recruiterEmail;

        @NotBlank(message = "채용담당자 이름은 필수입니다")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
        private String recruiterName;

        @NotBlank(message = "회사명은 필수입니다")
        @Size(min = 2, max = 100, message = "회사명은 2자 이상 100자 이하로 입력해주세요")
        private String recruiterCompany;

        @NotBlank(message = "메시지 내용은 필수입니다")
        @Size(min = 1, max = 1000, message = "메시지는 1자 이상 1000자 이하로 입력해주세요")
        private String message;
    }

    @Getter
    @AllArgsConstructor
    public static class SendMessageResponse {
        private String sessionToken;
        private UUID messageId;
        private String recruiterEmail;
        private String recruiterName;
        private String recruiterCompany;
        private String message;
        private LocalDateTime sentAt;

        public static SendMessageResponse from(ChatSession session, ChatMessage message) {
            return new SendMessageResponse(
                    session.getSessionToken(),
                    message.getMessageId(),
                    session.getRecruiterEmail(),
                    session.getRecruiterName(),
                    session.getRecruiterCompany(),
                    message.getContent(),
                    message.getCreatedAt()
            );
        }
    }

    /**
     * 채팅 세션 정보 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class SessionInfo {
        private String sessionToken;
        private UUID resumeSlug;
        private String resumeTitle;
        private String recruiterEmail;
        private String recruiterName;
        private String recruiterCompany;
        private long totalMessages;
        private long unreadMessages;
        private LocalDateTime lastMessageAt;
        private LocalDateTime createdAt;

        public static SessionInfo from(ChatSession session, long unreadCount) {
            return new SessionInfo(
                    session.getSessionToken(),
                    session.getResume().getResumeSlug(),
                    session.getResume().getTitle(),
                    session.getRecruiterEmail(),
                    session.getRecruiterName(),
                    session.getRecruiterCompany(),
                    session.getTotalMessages(),
                    unreadCount,
                    session.getLastMessageAt(),
                    session.getCreatedAt()
            );
        }
    }

    /**
     * 채팅 메시지 정보 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class MessageInfo {
        private UUID messageId;
        private String message;
        private SenderType senderType;
        private boolean readStatus;
        private LocalDateTime sentAt;

        public static MessageInfo from(ChatMessage message) {
            return new MessageInfo(
                    message.getMessageId(),
                    message.getContent(),
                    message.getSenderType(),
                    message.isReadStatus(),
                    message.getCreatedAt()
            );
        }
    }

    /**
     * 지원자용 채팅 조회 응답 DTO (세션 + 메시지 목록)
     */
    @Getter
    @AllArgsConstructor
    public static class ChatDetailResponse {
        private SessionInfo session;
        private List<MessageInfo> messages;

        public static ChatDetailResponse of(ChatSession session, List<ChatMessage> messages, long unreadCount) {
            return new ChatDetailResponse(
                    SessionInfo.from(session, unreadCount),
                    messages.stream()
                            .map(MessageInfo::from)
                            .collect(Collectors.toList())
            );
        }
    }

    /**
     * 지원자의 이력서별 채팅 세션 목록 조회 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class ResumeChatsResponse {
        private UUID resumeSlug;
        private String resumeTitle;
        private List<SessionInfo> sessions;
    }

    /**
     * 지원자용 메시지 전송 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantSendMessageRequest {
        @NotBlank(message = "메시지 내용은 필수입니다")
        @Size(min = 1, max = 1000, message = "메시지는 1자 이상 1000자 이하로 입력해주세요")
        private String message;
    }

    /**
     * 지원자용 메시지 전송 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class ApplicantSendMessageResponse {
        private String sessionToken;
        private UUID messageId;
        private String message;
        private LocalDateTime sentAt;

        public static ApplicantSendMessageResponse from(ChatSession session, ChatMessage message) {
            return new ApplicantSendMessageResponse(
                    session.getSessionToken(),
                    message.getMessageId(),
                    message.getContent(),
                    message.getCreatedAt()
            );
        }
    }
}
