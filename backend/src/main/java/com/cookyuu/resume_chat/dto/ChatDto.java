package com.cookyuu.resume_chat.dto;

import com.cookyuu.resume_chat.common.enums.MessageType;
import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
        private String applicantEmail;
        private String applicantName;
        private String recruiterEmail;
        private String recruiterName;
        private String recruiterCompany;
        private long totalMessages;
        private long unreadMessages;
        private LocalDateTime lastMessageAt;
        private LocalDateTime createdAt;

        public static SessionInfo from(ChatSession session, long unreadCount) {
            String applicantEmail = session.getResume() != null && session.getResume().getApplicant() != null
                    ? session.getResume().getApplicant().getEmail()
                    : null;
            String applicantName = session.getResume() != null && session.getResume().getApplicant() != null
                    ? session.getResume().getApplicant().getName()
                    : null;

            return new SessionInfo(
                    session.getSessionToken(),
                    session.getResume().getResumeSlug(),
                    session.getResume().getTitle(),
                    applicantEmail,
                    applicantName,
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

    /**
     * 채용담당자 세션 진입 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnterSessionRequest {
        @NotBlank(message = "채용담당자 이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String recruiterEmail;

        @NotBlank(message = "채용담당자 이름은 필수입니다")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
        private String recruiterName;

        @NotBlank(message = "회사명은 필수입니다")
        @Size(min = 2, max = 100, message = "회사명은 2자 이상 100자 이하로 입력해주세요")
        private String recruiterCompany;
    }

    /**
     * 채용담당자 세션 진입 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class EnterSessionResponse {
        private String sessionToken;
        private UUID resumeSlug;
        private String resumeTitle;
        private String recruiterEmail;
        private String recruiterName;
        private String recruiterCompany;
        private long totalMessages;
        private LocalDateTime lastMessageAt;
        private LocalDateTime createdAt;

        public static EnterSessionResponse from(ChatSession session) {
            return new EnterSessionResponse(
                    session.getSessionToken(),
                    session.getResume().getResumeSlug(),
                    session.getResume().getTitle(),
                    session.getRecruiterEmail(),
                    session.getRecruiterName(),
                    session.getRecruiterCompany(),
                    session.getTotalMessages(),
                    session.getLastMessageAt(),
                    session.getCreatedAt()
            );
        }
    }

    /**
     * 채용담당자용 메시지 전송 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruiterSendMessageRequest {
        @NotBlank(message = "메시지 내용은 필수입니다")
        @Size(min = 1, max = 1000, message = "메시지는 1자 이상 1000자 이하로 입력해주세요")
        private String message;
    }

    /**
     * 채용담당자용 메시지 전송 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class RecruiterSendMessageResponse {
        private String sessionToken;
        private UUID messageId;
        private String message;
        private LocalDateTime sentAt;

        public static RecruiterSendMessageResponse from(ChatSession session, ChatMessage message) {
            return new RecruiterSendMessageResponse(
                    session.getSessionToken(),
                    message.getMessageId(),
                    message.getContent(),
                    message.getCreatedAt()
            );
        }
    }

    /**
     * 페이지네이션 메시지 조회 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class PagedMessagesResponse {
        private List<MessageInfo> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 증분 조회 메시지 응답 DTO (since timestamp or messageId)
     */
    @Getter
    @AllArgsConstructor
    public static class IncrementalMessagesResponse {
        private List<MessageInfo> messages;
        private int count;
    }

    /**
     * WebSocket 채팅 메시지 DTO
     *
     * <p>WebSocket을 통해 브로드캐스트되는 실시간 채팅 메시지 형식입니다.</p>
     *
     * <h3>브로드캐스트 Destination</h3>
     * <ul>
     *   <li>클라이언트 구독: {@code /topic/session/{sessionToken}}</li>
     *   <li>클라이언트 전송: {@code /app/chat/{sessionToken}}</li>
     * </ul>
     *
     * <h3>필드 설명</h3>
     * <ul>
     *   <li><b>messageId</b>: 메시지 고유 식별자 (UUID)</li>
     *   <li><b>sessionToken</b>: 채팅 세션 토큰</li>
     *   <li><b>senderType</b>: 발신자 타입 (APPLICANT 또는 RECRUITER)</li>
     *   <li><b>messageType</b>: 메시지 타입 (TEXT, IMAGE, FILE, SYSTEM)</li>
     *   <li><b>content</b>: 메시지 내용</li>
     *   <li><b>sentAt</b>: 전송 시각 (서버 시간 기준)</li>
     * </ul>
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebSocketChatMessage {
        /**
         * 메시지 고유 식별자 (UUID)
         */
        private UUID messageId;

        /**
         * 채팅 세션 토큰
         */
        @NotBlank(message = "세션 토큰은 필수입니다")
        private String sessionToken;

        /**
         * 발신자 타입 (APPLICANT 또는 RECRUITER)
         */
        @NotNull(message = "발신자 타입은 필수입니다")
        private SenderType senderType;

        /**
         * 메시지 타입 (TEXT, IMAGE, FILE, SYSTEM)
         * 기본값: TEXT
         */
        @NotNull(message = "메시지 타입은 필수입니다")
        private MessageType messageType;

        /**
         * 메시지 내용
         */
        @NotBlank(message = "메시지 내용은 필수입니다")
        @Size(min = 1, max = 1000, message = "메시지는 1자 이상 1000자 이하로 입력해주세요")
        private String content;

        /**
         * 전송 시각 (서버 시간 기준)
         */
        private LocalDateTime sentAt;

        /**
         * ChatMessage 엔티티로부터 WebSocketChatMessage 생성
         */
        public static WebSocketChatMessage from(String sessionToken, ChatMessage message) {
            return WebSocketChatMessage.builder()
                    .messageId(message.getMessageId())
                    .sessionToken(sessionToken)
                    .senderType(message.getSenderType())
                    .messageType(message.getMessageType())
                    .content(message.getContent())
                    .sentAt(message.getCreatedAt())
                    .build();
        }
    }

    /**
     * 입력 중 표시(Typing Indicator) 이벤트
     *
     * <p>사용자가 메시지를 입력 중일 때 다른 사용자에게 실시간으로 알리기 위한 DTO입니다.</p>
     *
     * <h3>브로드캐스트 패턴</h3>
     * <ul>
     *   <li>Destination: {@code /topic/session/{sessionToken}/typing}</li>
     *   <li>클라이언트가 입력 시작 시: typing = true 전송</li>
     *   <li>클라이언트가 입력 중단 시: typing = false 전송</li>
     *   <li>자동 타임아웃: 3초간 새 입력 없으면 typing = false로 간주</li>
     * </ul>
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypingEvent {
        /**
         * 채팅 세션 토큰
         */
        @NotBlank(message = "세션 토큰은 필수입니다")
        private String sessionToken;

        /**
         * 입력 중인 사용자의 표시 이름
         * - 지원자: 이메일 또는 이름
         * - 채용담당자: 채용담당자 이름 또는 회사명
         */
        @NotBlank(message = "사용자 이름은 필수입니다")
        private String senderName;

        /**
         * 발신자 타입 (APPLICANT 또는 RECRUITER)
         */
        @NotNull(message = "발신자 타입은 필수입니다")
        private SenderType senderType;

        /**
         * 입력 중 여부
         * - true: 입력 중
         * - false: 입력 중단
         */
        @NotNull(message = "입력 상태는 필수입니다")
        private Boolean typing;

        /**
         * 이벤트 발생 시각
         */
        private LocalDateTime timestamp;
    }
}
