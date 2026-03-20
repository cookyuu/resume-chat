package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
import com.cookyuu.resume_chat.repository.ChatMessageRepository;
import com.cookyuu.resume_chat.repository.ChatSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService WebSocket 메서드 테스트")
class ChatServiceWebSocketTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("getSessionByToken - 성공")
    void getSessionByToken_success() {
        // Given
        String sessionToken = UUID.randomUUID().toString();
        Applicant applicant = Applicant.createNewApplicant("test@example.com", "홍길동", "password");
        Resume resume = Resume.createNewResume(applicant, "이력서", "설명", "file.pdf", "resume.pdf");
        ChatSession session = ChatSession.createNewSession(resume, "채용담당자", "recruiter@example.com", "회사명");

        when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

        // When
        ChatSession result = chatService.getSessionByToken(sessionToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecruiterEmail()).isEqualTo("recruiter@example.com");
        verify(chatSessionRepository).findBySessionToken(sessionToken);
    }

    @Test
    @DisplayName("getSessionByToken - 세션 없음 예외")
    void getSessionByToken_sessionNotFound_throwsException() {
        // Given
        String sessionToken = UUID.randomUUID().toString();
        when(chatSessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.getSessionByToken(sessionToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("채팅 세션을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("saveMessage - 성공")
    void saveMessage_success() {
        // Given
        Applicant applicant = Applicant.createNewApplicant("test@example.com", "홍길동", "password");
        Resume resume = Resume.createNewResume(applicant, "이력서", "설명", "file.pdf", "resume.pdf");
        ChatSession session = ChatSession.createNewSession(resume, "채용담당자", "recruiter@example.com", "회사명");
        ChatMessage message = ChatMessage.createMessage(session, SenderType.RECRUITER, "안녕하세요");

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(message);

        // When
        ChatMessage result = chatService.saveMessage(message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("안녕하세요");
        assertThat(result.getSenderType()).isEqualTo(SenderType.RECRUITER);
        verify(chatMessageRepository).save(message);
    }
}
