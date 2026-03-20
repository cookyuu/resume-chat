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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 테스트")
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Nested
    @DisplayName("sendMessageByApplicant - 지원자 메시지 전송")
    class SendMessageByApplicant {

        @Test
        @DisplayName("성공: 유효한 요청으로 메시지 저장")
        void success_validRequest_savesMessage() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";
            String messageContent = "안녕하세요, 연락 주셔서 감사합니다.";

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(applicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);
            when(session.getSessionToken()).thenReturn(sessionToken);

            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(messageContent);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            // When
            chatService.sendMessageByApplicant(applicantUuid, sessionToken, request);

            // Then
            verify(applicantRepository, times(1)).findByUuid(applicantUuid);
            verify(chatSessionRepository, times(1)).findBySessionToken(sessionToken);
            verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
            verify(session, times(1)).incrementMessageCount();
        }

        @Test
        @DisplayName("성공: 메시지가 APPLICANT 타입으로 저장됨")
        void success_messageTypeIsApplicant() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";
            String messageContent = "테스트 메시지";

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(applicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);
            when(session.getSessionToken()).thenReturn(sessionToken);

            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(messageContent);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

            // When
            chatService.sendMessageByApplicant(applicantUuid, sessionToken, request);

            // Then
            verify(chatMessageRepository).save(messageCaptor.capture());
            ChatMessage savedMessage = messageCaptor.getValue();

            assertThat(savedMessage.getSenderType()).isEqualTo(SenderType.APPLICANT);
            assertThat(savedMessage.getContent()).isEqualTo(messageContent);
            assertThat(savedMessage.getSession()).isEqualTo(session);
        }

        @Test
        @DisplayName("실패: 지원자를 찾을 수 없는 경우 BusinessException 발생")
        void fail_applicantNotFound_throwsBusinessException() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";
            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest("테스트 메시지");

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.sendMessageByApplicant(applicantUuid, sessionToken, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.APPLICANT_NOT_FOUND.getMessage())
                    .satisfies(ex -> {
                        BusinessException businessException = (BusinessException) ex;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.APPLICANT_NOT_FOUND);
                    });

            verify(applicantRepository, times(1)).findByUuid(applicantUuid);
            verify(chatSessionRepository, never()).findBySessionToken(anyString());
            verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("실패: 세션을 찾을 수 없는 경우 BusinessException 발생")
        void fail_sessionNotFound_throwsBusinessException() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "non-existent-token";
            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest("테스트 메시지");

            Applicant applicant = mock(Applicant.class);
            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.sendMessageByApplicant(applicantUuid, sessionToken, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SESSION_NOT_FOUND.getMessage())
                    .satisfies(ex -> {
                        BusinessException businessException = (BusinessException) ex;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.SESSION_NOT_FOUND);
                    });

            verify(applicantRepository, times(1)).findByUuid(applicantUuid);
            verify(chatSessionRepository, times(1)).findBySessionToken(sessionToken);
            verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("실패: 다른 지원자의 세션에 접근 시 BusinessException 발생")
        void fail_unauthorizedAccess_throwsBusinessException() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";
            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest("테스트 메시지");

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Applicant otherApplicant = mock(Applicant.class);
            when(otherApplicant.getId()).thenReturn(2L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(otherApplicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            // When & Then
            assertThatThrownBy(() -> chatService.sendMessageByApplicant(applicantUuid, sessionToken, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.RESUME_ACCESS_DENIED.getMessage())
                    .satisfies(ex -> {
                        BusinessException businessException = (BusinessException) ex;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.RESUME_ACCESS_DENIED);
                    });

            verify(applicantRepository, times(1)).findByUuid(applicantUuid);
            verify(chatSessionRepository, times(1)).findBySessionToken(sessionToken);
            verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("검증: 세션의 메시지 카운트가 증가함")
        void verify_incrementsMessageCount() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";
            String messageContent = "테스트 메시지";

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(applicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);
            when(session.getSessionToken()).thenReturn(sessionToken);

            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(messageContent);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            // When
            chatService.sendMessageByApplicant(applicantUuid, sessionToken, request);

            // Then
            verify(session, times(1)).incrementMessageCount();
        }

        @Test
        @DisplayName("검증: 응답에 세션 토큰과 메시지 정보가 포함됨")
        void verify_responseContainsCorrectData() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";
            String messageContent = "테스트 메시지";

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(applicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);
            when(session.getSessionToken()).thenReturn(sessionToken);

            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(messageContent);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

            // When
            chatService.sendMessageByApplicant(applicantUuid, sessionToken, request);

            // Then
            verify(chatMessageRepository).save(messageCaptor.capture());
            ChatMessage savedMessage = messageCaptor.getValue();

            assertThat(savedMessage).isNotNull();
            assertThat(savedMessage.getContent()).isEqualTo(messageContent);
            assertThat(savedMessage.getSession()).isEqualTo(session);
            assertThat(savedMessage.getSenderType()).isEqualTo(SenderType.APPLICANT);
        }

        @Test
        @DisplayName("검증: WebSocket 브로드캐스트가 호출됨")
        void verify_broadcastsMessageViaWebSocket() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";
            String messageContent = "테스트 메시지";

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(applicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);
            when(session.getSessionToken()).thenReturn(sessionToken);

            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(messageContent);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            // When
            chatService.sendMessageByApplicant(applicantUuid, sessionToken, request);

            // Then
            ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
            verify(messagingTemplate, times(1)).convertAndSend(
                    destinationCaptor.capture(),
                    any(Object.class)
            );
            assertThat(destinationCaptor.getValue()).isEqualTo("/topic/session/" + sessionToken);
        }
    }

    @Nested
    @DisplayName("sendRecruiterMessage - 채용담당자 메시지 전송")
    class SendRecruiterMessage {

        @Test
        @DisplayName("성공: 유효한 요청으로 메시지 저장 및 브로드캐스트")
        void success_validRequest_savesMessageAndBroadcasts() {
            // Given
            String sessionToken = "test-session-token";
            String messageContent = "안녕하세요, 면접 일정을 조율하고 싶습니다.";

            ChatSession session = mock(ChatSession.class);
            when(session.getSessionToken()).thenReturn(sessionToken);

            ChatDto.RecruiterSendMessageRequest request = new ChatDto.RecruiterSendMessageRequest(messageContent);

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            // When
            chatService.sendRecruiterMessage(sessionToken, request);

            // Then
            verify(chatSessionRepository, times(1)).findBySessionToken(sessionToken);
            verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
            verify(session, times(1)).incrementMessageCount();
            verify(messagingTemplate, times(1)).convertAndSend(
                    eq("/topic/session/" + sessionToken),
                    any(Object.class)
            );
        }

        @Test
        @DisplayName("성공: 메시지가 RECRUITER 타입으로 저장됨")
        void success_messageTypeIsRecruiter() {
            // Given
            String sessionToken = "test-session-token";
            String messageContent = "채용 공고를 확인하셨나요?";

            ChatSession session = mock(ChatSession.class);
            when(session.getSessionToken()).thenReturn(sessionToken);

            ChatDto.RecruiterSendMessageRequest request = new ChatDto.RecruiterSendMessageRequest(messageContent);

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

            // When
            chatService.sendRecruiterMessage(sessionToken, request);

            // Then
            verify(chatMessageRepository).save(messageCaptor.capture());
            ChatMessage savedMessage = messageCaptor.getValue();

            assertThat(savedMessage.getSenderType()).isEqualTo(SenderType.RECRUITER);
            assertThat(savedMessage.getContent()).isEqualTo(messageContent);
            assertThat(savedMessage.getSession()).isEqualTo(session);
        }

        @Test
        @DisplayName("실패: 세션을 찾을 수 없는 경우 BusinessException 발생")
        void fail_sessionNotFound_throwsBusinessException() {
            // Given
            String sessionToken = "non-existent-token";
            ChatDto.RecruiterSendMessageRequest request = new ChatDto.RecruiterSendMessageRequest("테스트 메시지");

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.sendRecruiterMessage(sessionToken, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SESSION_NOT_FOUND.getMessage())
                    .satisfies(ex -> {
                        BusinessException businessException = (BusinessException) ex;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.SESSION_NOT_FOUND);
                    });

            verify(chatSessionRepository, times(1)).findBySessionToken(sessionToken);
            verify(chatMessageRepository, never()).save(any(ChatMessage.class));
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("검증: WebSocket 브로드캐스트 destination이 올바름")
        void verify_correctBroadcastDestination() {
            // Given
            String sessionToken = "test-session-token-123";
            String messageContent = "테스트 메시지";

            ChatSession session = mock(ChatSession.class);
            when(session.getSessionToken()).thenReturn(sessionToken);

            ChatDto.RecruiterSendMessageRequest request = new ChatDto.RecruiterSendMessageRequest(messageContent);

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);

            // When
            chatService.sendRecruiterMessage(sessionToken, request);

            // Then
            verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), any(Object.class));
            String destination = destinationCaptor.getValue();

            assertThat(destination).isEqualTo("/topic/session/" + sessionToken);
        }
    }

    @Nested
    @DisplayName("getSessionMessagesPaged - 페이지네이션 메시지 조회 (채용담당자용)")
    class GetSessionMessagesPaged {

        @Test
        @DisplayName("성공: 페이지네이션으로 메시지 조회 (DESC 정렬)")
        void success_pagedMessages_descSort() {
            // Given
            String sessionToken = "test-session-token";
            int page = 0;
            int size = 2;
            String sortDirection = "desc";

            ChatSession session = mock(ChatSession.class);
            ChatMessage message1 = mock(ChatMessage.class);
            ChatMessage message2 = mock(ChatMessage.class);

            when(message1.getMessageId()).thenReturn(UUID.randomUUID());
            when(message1.getContent()).thenReturn("메시지 2");
            when(message1.getSenderType()).thenReturn(SenderType.RECRUITER);
            when(message1.isReadStatus()).thenReturn(true);
            when(message1.getCreatedAt()).thenReturn(LocalDateTime.now());

            when(message2.getMessageId()).thenReturn(UUID.randomUUID());
            when(message2.getContent()).thenReturn("메시지 1");
            when(message2.getSenderType()).thenReturn(SenderType.APPLICANT);
            when(message2.isReadStatus()).thenReturn(false);
            when(message2.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(1));

            List<ChatMessage> messages = Arrays.asList(message1, message2);
            Page<ChatMessage> messagePage = new PageImpl<>(messages, PageRequest.of(page, size), 5);

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
            when(chatMessageRepository.findBySessionOrderByCreatedAtDesc(eq(session), any(Pageable.class)))
                    .thenReturn(messagePage);

            // When
            ChatDto.PagedMessagesResponse response = chatService.getSessionMessagesPaged(sessionToken, page, size, sortDirection);

            // Then
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(2);
            assertThat(response.getTotalElements()).isEqualTo(5);
            assertThat(response.getTotalPages()).isEqualTo(3);
            assertThat(response.isHasNext()).isTrue();
            assertThat(response.isHasPrevious()).isFalse();

            verify(chatSessionRepository, times(1)).findBySessionToken(sessionToken);
            verify(chatMessageRepository, times(1)).findBySessionOrderByCreatedAtDesc(eq(session), any(Pageable.class));
        }

        @Test
        @DisplayName("성공: size 제한 적용 (최대 100)")
        void success_sizeLimitApplied() {
            // Given
            String sessionToken = "test-session-token";
            int requestedSize = 150; // 최대값 초과

            ChatSession session = mock(ChatSession.class);
            Page<ChatMessage> messagePage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 100), 0);

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
            when(chatMessageRepository.findBySessionOrderByCreatedAtAsc(eq(session), any(Pageable.class)))
                    .thenReturn(messagePage);

            // When
            ChatDto.PagedMessagesResponse response = chatService.getSessionMessagesPaged(sessionToken, 0, requestedSize, "asc");

            // Then
            assertThat(response.getSize()).isEqualTo(100); // 100으로 제한되어야 함

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(chatMessageRepository).findBySessionOrderByCreatedAtAsc(eq(session), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 세션")
        void fail_sessionNotFound() {
            // Given
            String sessionToken = "invalid-token";

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.getSessionMessagesPaged(sessionToken, 0, 20, "asc"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getApplicantSessionMessagesPaged - 페이지네이션 메시지 조회 (지원자용)")
    class GetApplicantSessionMessagesPaged {

        @Test
        @DisplayName("성공: 지원자가 자신의 세션 메시지 조회")
        void success_applicantOwnSession() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(applicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);

            Page<ChatMessage> messagePage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
            when(chatMessageRepository.findBySessionOrderByCreatedAtAsc(eq(session), any(Pageable.class)))
                    .thenReturn(messagePage);

            // When
            ChatDto.PagedMessagesResponse response = chatService.getApplicantSessionMessagesPaged(
                    applicantUuid, sessionToken, 0, 20, "asc"
            );

            // Then
            assertThat(response).isNotNull();
            verify(applicantRepository, times(1)).findByUuid(applicantUuid);
            verify(chatSessionRepository, times(1)).findBySessionToken(sessionToken);
        }

        @Test
        @DisplayName("실패: 다른 지원자의 세션 접근")
        void fail_accessDenied() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Applicant otherApplicant = mock(Applicant.class);
            when(otherApplicant.getId()).thenReturn(2L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(otherApplicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            // When & Then
            assertThatThrownBy(() -> chatService.getApplicantSessionMessagesPaged(
                    applicantUuid, sessionToken, 0, 20, "asc"
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESUME_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("getMessagesSinceTimestamp - 증분 조회 (timestamp 기반)")
    class GetMessagesSinceTimestamp {

        @Test
        @DisplayName("성공: timestamp 이후 메시지 조회")
        void success_messagesSinceTimestamp() {
            // Given
            String sessionToken = "test-session-token";
            LocalDateTime timestamp = LocalDateTime.now().minusHours(1);

            ChatSession session = mock(ChatSession.class);
            ChatMessage message1 = mock(ChatMessage.class);
            ChatMessage message2 = mock(ChatMessage.class);

            when(message1.getMessageId()).thenReturn(UUID.randomUUID());
            when(message1.getContent()).thenReturn("새 메시지 1");
            when(message1.getSenderType()).thenReturn(SenderType.RECRUITER);
            when(message1.isReadStatus()).thenReturn(false);
            when(message1.getCreatedAt()).thenReturn(LocalDateTime.now());

            when(message2.getMessageId()).thenReturn(UUID.randomUUID());
            when(message2.getContent()).thenReturn("새 메시지 2");
            when(message2.getSenderType()).thenReturn(SenderType.APPLICANT);
            when(message2.isReadStatus()).thenReturn(false);
            when(message2.getCreatedAt()).thenReturn(LocalDateTime.now());

            List<ChatMessage> messages = Arrays.asList(message1, message2);

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
            when(chatMessageRepository.findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(session, timestamp))
                    .thenReturn(messages);

            // When
            ChatDto.IncrementalMessagesResponse response = chatService.getMessagesSinceTimestamp(sessionToken, timestamp);

            // Then
            assertThat(response.getMessages()).hasSize(2);
            assertThat(response.getCount()).isEqualTo(2);

            verify(chatSessionRepository, times(1)).findBySessionToken(sessionToken);
            verify(chatMessageRepository, times(1))
                    .findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(session, timestamp);
        }

        @Test
        @DisplayName("성공: 새 메시지가 없는 경우")
        void success_noNewMessages() {
            // Given
            String sessionToken = "test-session-token";
            LocalDateTime timestamp = LocalDateTime.now();

            ChatSession session = mock(ChatSession.class);

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
            when(chatMessageRepository.findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(session, timestamp))
                    .thenReturn(Collections.emptyList());

            // When
            ChatDto.IncrementalMessagesResponse response = chatService.getMessagesSinceTimestamp(sessionToken, timestamp);

            // Then
            assertThat(response.getMessages()).isEmpty();
            assertThat(response.getCount()).isZero();
        }
    }

    @Nested
    @DisplayName("getMessagesSinceMessageId - 증분 조회 (messageId 기반)")
    class GetMessagesSinceMessageId {

        @Test
        @DisplayName("성공: messageId 이후 메시지 조회")
        void success_messagesSinceMessageId() {
            // Given
            String sessionToken = "test-session-token";
            Long lastMessageId = 100L;

            ChatSession session = mock(ChatSession.class);
            ChatMessage message = mock(ChatMessage.class);

            when(message.getMessageId()).thenReturn(UUID.randomUUID());
            when(message.getContent()).thenReturn("새 메시지");
            when(message.getSenderType()).thenReturn(SenderType.RECRUITER);
            when(message.isReadStatus()).thenReturn(false);
            when(message.getCreatedAt()).thenReturn(LocalDateTime.now());

            List<ChatMessage> messages = Collections.singletonList(message);

            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
            when(chatMessageRepository.findBySessionAndIdGreaterThanOrderByCreatedAtAsc(session, lastMessageId))
                    .thenReturn(messages);

            // When
            ChatDto.IncrementalMessagesResponse response = chatService.getMessagesSinceMessageId(sessionToken, lastMessageId);

            // Then
            assertThat(response.getMessages()).hasSize(1);
            assertThat(response.getCount()).isEqualTo(1);

            verify(chatMessageRepository, times(1))
                    .findBySessionAndIdGreaterThanOrderByCreatedAtAsc(session, lastMessageId);
        }
    }

    @Nested
    @DisplayName("getApplicantMessagesSinceTimestamp - 지원자용 증분 조회")
    class GetApplicantMessagesSinceTimestamp {

        @Test
        @DisplayName("성공: 지원자가 자신의 세션에서 증분 조회")
        void success_applicantIncrementalQuery() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";
            LocalDateTime timestamp = LocalDateTime.now().minusHours(1);

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(applicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
            when(chatMessageRepository.findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(session, timestamp))
                    .thenReturn(Collections.emptyList());

            // When
            ChatDto.IncrementalMessagesResponse response = chatService.getApplicantMessagesSinceTimestamp(
                    applicantUuid, sessionToken, timestamp
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessages()).isEmpty();
        }

        @Test
        @DisplayName("실패: 다른 지원자의 세션 접근")
        void fail_accessDenied() {
            // Given
            UUID applicantUuid = UUID.randomUUID();
            String sessionToken = "test-session-token";
            LocalDateTime timestamp = LocalDateTime.now();

            Applicant applicant = mock(Applicant.class);
            when(applicant.getId()).thenReturn(1L);

            Applicant otherApplicant = mock(Applicant.class);
            when(otherApplicant.getId()).thenReturn(2L);

            Resume resume = mock(Resume.class);
            when(resume.getApplicant()).thenReturn(otherApplicant);

            ChatSession session = mock(ChatSession.class);
            when(session.getResume()).thenReturn(resume);

            when(applicantRepository.findByUuid(applicantUuid)).thenReturn(Optional.of(applicant));
            when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

            // When & Then
            assertThatThrownBy(() -> chatService.getApplicantMessagesSinceTimestamp(
                    applicantUuid, sessionToken, timestamp
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESUME_ACCESS_DENIED);
        }
    }
}
