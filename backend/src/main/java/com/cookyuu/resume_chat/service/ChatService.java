package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.dto.ChatDto;
import com.cookyuu.resume_chat.entity.Applicant;
import com.cookyuu.resume_chat.entity.ChatMessage;
import com.cookyuu.resume_chat.entity.ChatSession;
import com.cookyuu.resume_chat.entity.Resume;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.repository.ChatMessageRepository;
import com.cookyuu.resume_chat.repository.ChatSessionRepository;
import com.cookyuu.resume_chat.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        log.info("채용담당자 메시지 전송 완료: sessionToken={}, messageId={}",
                sessionToken, message.getMessageId());

        return ChatDto.RecruiterSendMessageResponse.from(session, message);
    }
}
