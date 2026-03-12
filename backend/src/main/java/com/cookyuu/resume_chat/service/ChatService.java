package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.dto.ChatDto;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.repository.ChatMessageRepository;
import com.cookyuu.resume_chat.repository.ChatSessionRepository;
import com.cookyuu.resume_chat.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicantRepository applicantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatDto.SendMessageResponse sendMessage(UUID resumeSlug, ChatDto.SendMessageRequest request) {
        Resume resume = resumeRepository.findByResumeSlug(resumeSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ChatSession session = chatSessionRepository.findByResumeAndRecruiterEmail(resume, request.getRecruiterEmail())
                .orElseGet(() -> {
                    ChatSession newSession = ChatSession.createNewSession(
                            resume,
                            request.getRecruiterName(),
                            request.getRecruiterEmail(),
                            request.getRecruiterCompany()
                    );
                    return chatSessionRepository.save(newSession);
                });

        ChatMessage message = ChatMessage.createMessage(session, SenderType.RECRUITER, request.getMessage());
        chatMessageRepository.save(message);

        session.incrementMessageCount();

        // WebSocket 브로드캐스트
        broadcastMessage(session.getSessionToken(), message);

        log.info("채팅 메시지 전송 완료: sessionToken={}, messageId={}, recruiterEmail={}",
                session.getSessionToken(), message.getMessageId(), request.getRecruiterEmail());

        return ChatDto.SendMessageResponse.from(session, message);
    }

    @Transactional(readOnly = true)
    public ChatDto.ResumeChatsResponse getResumeChats(UUID applicantUuid, UUID resumeSlug) {
        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        Resume resume = resumeRepository.findByResumeSlug(resumeSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        if (!resume.getApplicant().getId().equals(applicant.getId())) {
            throw new BusinessException(ErrorCode.RESUME_ACCESS_DENIED);
        }

        List<ChatSession> sessions = chatSessionRepository.findByResume(resume);

        List<ChatDto.SessionInfo> sessionInfos = sessions.stream()
                .map(session -> {
                    long unreadCount = chatMessageRepository.countBySessionAndReadStatusFalse(session);
                    return ChatDto.SessionInfo.from(session, unreadCount);
                })
                .collect(Collectors.toList());

        log.info("이력서 채팅 세션 조회 완료: applicantUuid={}, resumeSlug={}, sessionCount={}",
                applicantUuid, resumeSlug, sessions.size());

        return new ChatDto.ResumeChatsResponse(
                resume.getResumeSlug(),
                resume.getTitle(),
                sessionInfos
        );
    }

    @Transactional
    public ChatDto.ChatDetailResponse getSessionMessages(UUID applicantUuid, String sessionToken) {
        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getResume().getApplicant().getId().equals(applicant.getId())) {
            throw new BusinessException(ErrorCode.RESUME_ACCESS_DENIED);
        }

        long unreadCount = chatMessageRepository.countBySessionAndReadStatusFalse(session);

        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);

        messages.forEach(message -> {
            if (!message.isReadStatus()) {
                message.markAsRead();
            }
        });

        log.info("채팅 메시지 조회 완료: applicantUuid={}, sessionToken={}, messageCount={}, unreadCount={}",
                applicantUuid, sessionToken, messages.size(), unreadCount);

        return ChatDto.ChatDetailResponse.of(session, messages, unreadCount);
    }

    @Transactional
    public ChatDto.ApplicantSendMessageResponse sendMessageByApplicant(
            UUID applicantUuid,
            String sessionToken,
            ChatDto.ApplicantSendMessageRequest request) {

        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getResume().getApplicant().getId().equals(applicant.getId())) {
            throw new BusinessException(ErrorCode.RESUME_ACCESS_DENIED);
        }

        ChatMessage message = ChatMessage.createMessage(session, SenderType.APPLICANT, request.getMessage());
        chatMessageRepository.save(message);

        session.incrementMessageCount();

        // WebSocket 브로드캐스트
        broadcastMessage(session.getSessionToken(), message);

        log.info("지원자 채팅 메시지 전송 완료: sessionToken={}, messageId={}, applicantUuid={}",
                session.getSessionToken(), message.getMessageId(), applicantUuid);

        return ChatDto.ApplicantSendMessageResponse.from(session, message);
    }

    @Transactional
    public ChatDto.EnterSessionResponse enterChatSession(UUID resumeSlug, ChatDto.EnterSessionRequest request) {
        Resume resume = resumeRepository.findByResumeSlug(resumeSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ChatSession session = chatSessionRepository.findByResumeAndRecruiterEmail(resume, request.getRecruiterEmail())
                .orElseGet(() -> {
                    ChatSession newSession = ChatSession.createNewSession(
                            resume,
                            request.getRecruiterName(),
                            request.getRecruiterEmail(),
                            request.getRecruiterCompany()
                    );
                    ChatSession saved = chatSessionRepository.save(newSession);
                    log.info("새 채팅 세션 생성: sessionToken={}, recruiterEmail={}, resumeSlug={}",
                            saved.getSessionToken(), request.getRecruiterEmail(), resumeSlug);
                    return saved;
                });

        log.info("채용담당자 세션 진입: sessionToken={}, recruiterEmail={}, isNewSession={}",
                session.getSessionToken(), request.getRecruiterEmail(), session.getTotalMessages() == 0);

        return ChatDto.EnterSessionResponse.from(session);
    }

    @Transactional(readOnly = true)
    public ChatDto.ChatDetailResponse getRecruiterSessionMessages(String sessionToken) {
        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        long unreadCount = chatMessageRepository.countBySessionAndReadStatusFalse(session);

        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);

        log.info("채용담당자 메시지 조회: sessionToken={}, messageCount={}", sessionToken, messages.size());

        return ChatDto.ChatDetailResponse.of(session, messages, unreadCount);
    }

    @Transactional
    public ChatDto.RecruiterSendMessageResponse sendRecruiterMessage(
            String sessionToken,
            ChatDto.RecruiterSendMessageRequest request) {

        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        ChatMessage message = ChatMessage.createMessage(session, SenderType.RECRUITER, request.getMessage());
        chatMessageRepository.save(message);

        session.incrementMessageCount();

        // WebSocket 브로드캐스트
        broadcastMessage(sessionToken, message);

        log.info("채용담당자 메시지 전송 완료: sessionToken={}, messageId={}",
                sessionToken, message.getMessageId());

        return ChatDto.RecruiterSendMessageResponse.from(session, message);
    }

    /**
     * 세션 토큰으로 세션 조회 (WebSocket용)
     */
    @Transactional(readOnly = true)
    public ChatSession getSessionByToken(String sessionToken) {
        return chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    /**
     * 메시지 저장 (WebSocket용)
     */
    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    /**
     * WebSocket 메시지 저장 및 세션 업데이트
     *
     * @param sessionToken 세션 토큰
     * @param senderType 발신자 타입
     * @param content 메시지 내용
     * @param applicantUuid 지원자 UUID (지원자인 경우에만 제공, 채용담당자는 null)
     * @return 저장된 메시지
     */
    @Transactional
    public ChatMessage saveMessageAndUpdateSession(String sessionToken, SenderType senderType, String content, UUID applicantUuid) {
        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 지원자인 경우 권한 검증
        if (senderType == SenderType.APPLICANT && applicantUuid != null) {
            if (!session.getResume().getApplicant().getUuid().equals(applicantUuid)) {
                log.error("지원자의 세션 접근 권한 없음 - applicantUuid: {}, sessionToken: {}",
                        applicantUuid, sessionToken);
                throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
            }
        }

        ChatMessage message = ChatMessage.createMessage(session, senderType, content);
        chatMessageRepository.save(message);

        session.incrementMessageCount();

        log.info("WebSocket 메시지 저장 완료 - sessionToken: {}, messageId: {}, senderType: {}",
                sessionToken, message.getMessageId(), senderType);

        return message;
    }

    /**
     * WebSocket으로 메시지 브로드캐스트
     *
     * <p>저장된 메시지를 WebSocket을 통해 해당 세션의 모든 클라이언트에게 브로드캐스트합니다.</p>
     *
     * <h3>브로드캐스트 Destination 패턴</h3>
     * <ul>
     *   <li>Pattern: {@code /topic/session/{sessionToken}}</li>
     *   <li>Example: {@code /topic/session/abc123-def456}</li>
     * </ul>
     *
     * <h3>메시지 형식</h3>
     * <ul>
     *   <li>DTO: {@link ChatDto.WebSocketChatMessage}</li>
     *   <li>필드: messageId, sessionToken, senderType, messageType, content, sentAt</li>
     * </ul>
     *
     * @param sessionToken 세션 토큰
     * @param message 저장된 메시지
     */
    private void broadcastMessage(String sessionToken, ChatMessage message) {
        ChatDto.WebSocketChatMessage wsMessage = ChatDto.WebSocketChatMessage.from(sessionToken, message);

        String destination = "/topic/session/" + sessionToken;
        messagingTemplate.convertAndSend(destination, wsMessage);

        log.info("WebSocket 메시지 브로드캐스트 완료 - destination: {}, messageId: {}, messageType: {}",
                destination, message.getMessageId(), message.getMessageType());
    }

    /**
     * 페이지네이션으로 메시지 조회 (채용담당자용)
     *
     * @param sessionToken 세션 토큰
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (최대 100)
     * @param sortDirection 정렬 방향 (asc 또는 desc)
     * @return 페이지네이션된 메시지 목록
     */
    @Transactional(readOnly = true)
    public ChatDto.PagedMessagesResponse getSessionMessagesPaged(
            String sessionToken,
            int page,
            int size,
            String sortDirection) {

        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // size 제한 (최대 100)
        int validatedSize = Math.min(size, 100);

        // Pageable 생성
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, validatedSize, sort);

        // 페이지네이션 조회
        Page<ChatMessage> messagePage = sortDirection.equalsIgnoreCase("asc")
                ? chatMessageRepository.findBySessionOrderByCreatedAtAsc(session, pageable)
                : chatMessageRepository.findBySessionOrderByCreatedAtDesc(session, pageable);

        List<ChatDto.MessageInfo> messageInfos = messagePage.getContent().stream()
                .map(ChatDto.MessageInfo::from)
                .collect(Collectors.toList());

        log.info("페이지네이션 메시지 조회 완료: sessionToken={}, page={}, size={}, totalElements={}",
                sessionToken, page, validatedSize, messagePage.getTotalElements());

        return new ChatDto.PagedMessagesResponse(
                messageInfos,
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                messagePage.getTotalPages(),
                messagePage.hasNext(),
                messagePage.hasPrevious()
        );
    }

    /**
     * 페이지네이션으로 메시지 조회 (지원자용)
     *
     * @param applicantUuid 지원자 UUID
     * @param sessionToken 세션 토큰
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (최대 100)
     * @param sortDirection 정렬 방향 (asc 또는 desc)
     * @return 페이지네이션된 메시지 목록
     */
    @Transactional(readOnly = true)
    public ChatDto.PagedMessagesResponse getApplicantSessionMessagesPaged(
            UUID applicantUuid,
            String sessionToken,
            int page,
            int size,
            String sortDirection) {

        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 권한 검증
        if (!session.getResume().getApplicant().getId().equals(applicant.getId())) {
            throw new BusinessException(ErrorCode.RESUME_ACCESS_DENIED);
        }

        // size 제한 (최대 100)
        int validatedSize = Math.min(size, 100);

        // Pageable 생성
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, validatedSize, sort);

        // 페이지네이션 조회
        Page<ChatMessage> messagePage = sortDirection.equalsIgnoreCase("asc")
                ? chatMessageRepository.findBySessionOrderByCreatedAtAsc(session, pageable)
                : chatMessageRepository.findBySessionOrderByCreatedAtDesc(session, pageable);

        List<ChatDto.MessageInfo> messageInfos = messagePage.getContent().stream()
                .map(ChatDto.MessageInfo::from)
                .collect(Collectors.toList());

        log.info("지원자 페이지네이션 메시지 조회 완료: applicantUuid={}, sessionToken={}, page={}, size={}, totalElements={}",
                applicantUuid, sessionToken, page, validatedSize, messagePage.getTotalElements());

        return new ChatDto.PagedMessagesResponse(
                messageInfos,
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                messagePage.getTotalPages(),
                messagePage.hasNext(),
                messagePage.hasPrevious()
        );
    }

    /**
     * 특정 시간 이후 메시지 조회 (증분 조회 - timestamp 기반)
     *
     * @param sessionToken 세션 토큰
     * @param timestamp 기준 시간
     * @return 해당 시간 이후의 메시지 목록
     */
    @Transactional(readOnly = true)
    public ChatDto.IncrementalMessagesResponse getMessagesSinceTimestamp(
            String sessionToken,
            LocalDateTime timestamp) {

        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        List<ChatMessage> messages = chatMessageRepository
                .findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(session, timestamp);

        List<ChatDto.MessageInfo> messageInfos = messages.stream()
                .map(ChatDto.MessageInfo::from)
                .collect(Collectors.toList());

        log.info("증분 조회 (timestamp) 완료: sessionToken={}, timestamp={}, messageCount={}",
                sessionToken, timestamp, messages.size());

        return new ChatDto.IncrementalMessagesResponse(messageInfos, messageInfos.size());
    }

    /**
     * 특정 메시지 ID 이후 메시지 조회 (증분 조회 - messageId 기반)
     *
     * @param sessionToken 세션 토큰
     * @param lastMessageId 마지막 메시지 ID (내부 Long ID)
     * @return 해당 ID 이후의 메시지 목록
     */
    @Transactional(readOnly = true)
    public ChatDto.IncrementalMessagesResponse getMessagesSinceMessageId(
            String sessionToken,
            Long lastMessageId) {

        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        List<ChatMessage> messages = chatMessageRepository
                .findBySessionAndIdGreaterThanOrderByCreatedAtAsc(session, lastMessageId);

        List<ChatDto.MessageInfo> messageInfos = messages.stream()
                .map(ChatDto.MessageInfo::from)
                .collect(Collectors.toList());

        log.info("증분 조회 (messageId) 완료: sessionToken={}, lastMessageId={}, messageCount={}",
                sessionToken, lastMessageId, messages.size());

        return new ChatDto.IncrementalMessagesResponse(messageInfos, messageInfos.size());
    }

    /**
     * 지원자용 증분 조회 (timestamp 기반)
     *
     * @param applicantUuid 지원자 UUID
     * @param sessionToken 세션 토큰
     * @param timestamp 기준 시간
     * @return 해당 시간 이후의 메시지 목록
     */
    @Transactional(readOnly = true)
    public ChatDto.IncrementalMessagesResponse getApplicantMessagesSinceTimestamp(
            UUID applicantUuid,
            String sessionToken,
            LocalDateTime timestamp) {

        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 권한 검증
        if (!session.getResume().getApplicant().getId().equals(applicant.getId())) {
            throw new BusinessException(ErrorCode.RESUME_ACCESS_DENIED);
        }

        List<ChatMessage> messages = chatMessageRepository
                .findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(session, timestamp);

        List<ChatDto.MessageInfo> messageInfos = messages.stream()
                .map(ChatDto.MessageInfo::from)
                .collect(Collectors.toList());

        log.info("지원자 증분 조회 (timestamp) 완료: applicantUuid={}, sessionToken={}, timestamp={}, messageCount={}",
                applicantUuid, sessionToken, timestamp, messages.size());

        return new ChatDto.IncrementalMessagesResponse(messageInfos, messageInfos.size());
    }

    /**
     * 지원자용 증분 조회 (messageId 기반)
     *
     * @param applicantUuid 지원자 UUID
     * @param sessionToken 세션 토큰
     * @param lastMessageId 마지막 메시지 ID
     * @return 해당 ID 이후의 메시지 목록
     */
    @Transactional(readOnly = true)
    public ChatDto.IncrementalMessagesResponse getApplicantMessagesSinceMessageId(
            UUID applicantUuid,
            String sessionToken,
            Long lastMessageId) {

        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 권한 검증
        if (!session.getResume().getApplicant().getId().equals(applicant.getId())) {
            throw new BusinessException(ErrorCode.RESUME_ACCESS_DENIED);
        }

        List<ChatMessage> messages = chatMessageRepository
                .findBySessionAndIdGreaterThanOrderByCreatedAtAsc(session, lastMessageId);

        List<ChatDto.MessageInfo> messageInfos = messages.stream()
                .map(ChatDto.MessageInfo::from)
                .collect(Collectors.toList());

        log.info("지원자 증분 조회 (messageId) 완료: applicantUuid={}, sessionToken={}, lastMessageId={}, messageCount={}",
                applicantUuid, sessionToken, lastMessageId, messages.size());

        return new ChatDto.IncrementalMessagesResponse(messageInfos, messageInfos.size());
    }
}
