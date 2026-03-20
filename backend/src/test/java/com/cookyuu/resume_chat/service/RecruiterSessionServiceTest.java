package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
import com.cookyuu.resume_chat.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecruiterSessionService 테스트")
class RecruiterSessionServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @InjectMocks
    private RecruiterSessionService recruiterSessionService;

    private ChatSession chatSession;
    private String sessionToken;

    @BeforeEach
    void setUp() {
        sessionToken = UUID.randomUUID().toString();

        Applicant applicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("applicant@example.com")
                .password("password123")
                .name("지원자")
                .build();

        Resume resume = Resume.builder()
                .resumeSlug(UUID.randomUUID())
                .title("백엔드 개발자")
                .applicant(applicant)
                .build();

        chatSession = ChatSession.createNewSession(
                resume,
                "김채용",
                "recruiter@company.com",
                "테스트회사"
        );
    }

    @Test
    @DisplayName("validateAndGetSession_유효한_sessionToken_세션_반환")
    void validateAndGetSession_validToken_returnsSession() {
        // Given
        given(chatSessionRepository.findBySessionToken(sessionToken))
                .willReturn(Optional.of(chatSession));

        // When
        ChatSession result = recruiterSessionService.validateAndGetSession(sessionToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecruiterEmail()).isEqualTo("recruiter@company.com");
    }

    @Test
    @DisplayName("validateAndGetSession_존재하지않는_sessionToken_예외발생")
    void validateAndGetSession_invalidToken_throwsException() {
        // Given
        given(chatSessionRepository.findBySessionToken(anyString()))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> recruiterSessionService.validateAndGetSession(sessionToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    @DisplayName("validateRecruiterAccess_일치하는_이메일_성공")
    void validateRecruiterAccess_matchingEmail_success() {
        // Given
        given(chatSessionRepository.findBySessionToken(sessionToken))
                .willReturn(Optional.of(chatSession));

        // When & Then (예외가 발생하지 않아야 함)
        recruiterSessionService.validateRecruiterAccess(sessionToken, "recruiter@company.com");
    }

    @Test
    @DisplayName("validateRecruiterAccess_불일치하는_이메일_예외발생")
    void validateRecruiterAccess_mismatchedEmail_throwsException() {
        // Given
        given(chatSessionRepository.findBySessionToken(sessionToken))
                .willReturn(Optional.of(chatSession));

        // When & Then
        assertThatThrownBy(() ->
                recruiterSessionService.validateRecruiterAccess(sessionToken, "wrong@company.com"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("existsSessionToken_존재하는_토큰_true_반환")
    void existsSessionToken_existingToken_returnsTrue() {
        // Given
        given(chatSessionRepository.findBySessionToken(sessionToken))
                .willReturn(Optional.of(chatSession));

        // When
        boolean result = recruiterSessionService.existsSessionToken(sessionToken);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsSessionToken_존재하지않는_토큰_false_반환")
    void existsSessionToken_nonExistingToken_returnsFalse() {
        // Given
        given(chatSessionRepository.findBySessionToken(sessionToken))
                .willReturn(Optional.empty());

        // When
        boolean result = recruiterSessionService.existsSessionToken(sessionToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("activateSession_세션_활성화_성공")
    void activateSession_success() {
        // When & Then (예외가 발생하지 않아야 함)
        recruiterSessionService.activateSession(sessionToken);
    }

    @Test
    @DisplayName("deactivateSession_세션_비활성화_성공")
    void deactivateSession_success() {
        // Given
        recruiterSessionService.activateSession(sessionToken);

        // When & Then (예외가 발생하지 않아야 함)
        recruiterSessionService.deactivateSession(sessionToken);
    }
}
