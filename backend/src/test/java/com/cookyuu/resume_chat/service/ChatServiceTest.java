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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    }
}
