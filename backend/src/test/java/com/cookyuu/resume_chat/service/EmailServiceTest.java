package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService 테스트")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Mock
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        // ReflectionTestUtils로 @Value 필드 주입
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@resumechat.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Resume Chat");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(emailService, "notificationDelayMinutes", 5);
    }

    @Test
    @DisplayName("sendNewMessageEmail_새메시지_이메일발송_성공")
    void sendNewMessageEmail_success() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String recipientEmail = "test@example.com";
        String recipientName = "테스터";
        String senderName = "채용담당자";
        SenderType senderType = SenderType.RECRUITER;
        String sessionToken = "test-session-token";
        int unreadCount = 3;
        String messagePreview = "안녕하세요, 이력서 검토했습니다.";

        // When
        emailService.sendNewMessageEmail(
                recipientEmail,
                recipientName,
                senderName,
                senderType,
                sessionToken,
                unreadCount,
                messagePreview
        );

        // Then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendNewSessionNotification_신규세션_알림발송_성공")
    void sendNewSessionNotification_success() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Applicant applicant = Applicant.createNewApplicant(
                "applicant@example.com",
                "지원자",
                "password123"
        );

        Resume resume = Resume.createNewResume(
                applicant,
                "백엔드 개발자 이력서",
                "5년차 백엔드 개발자입니다.",
                "/uploads/resume.pdf",
                "resume.pdf"
        );

        ChatSession session = ChatSession.createNewSession(
                resume,
                "김채용",
                "recruiter@company.com",
                "테크컴퍼니"
        );

        String firstMessage = "안녕하세요, 귀하의 이력서를 보고 연락드립니다.";

        // When
        emailService.sendNewSessionNotification(session, firstMessage);

        // Then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("scheduleNewMessageNotification_예약_생성_확인")
    void scheduleNewMessageNotification_createsSchedule() {
        // Given
        String sessionToken = "test-session-token";
        String recipientEmail = "test@example.com";
        String recipientName = "테스터";
        String senderName = "채용담당자";
        SenderType senderType = SenderType.RECRUITER;
        String messagePreview = "첫 번째 메시지";

        // When - 알림 예약 (실제로는 5분 후 발송)
        emailService.scheduleNewMessageNotification(
                sessionToken,
                recipientEmail,
                recipientName,
                senderName,
                senderType,
                messagePreview
        );

        // Then - 예약이 생성되고 즉시 이메일이 발송되지 않음
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("cancelNotification_존재하지않는_알림_안전하게처리")
    void cancelNotification_nonExistingNotification_handledSafely() {
        // When & Then (예외가 발생하지 않아야 함)
        emailService.cancelNotification("non-existing-session-token");

        // 이메일이 발송되지 않아야 함
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendNewMessageEmail_이메일발송실패_예외처리")
    void sendNewMessageEmail_sendFailure_handlesException() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("메일 서버 오류"))
                .when(mailSender).send(any(MimeMessage.class));

        // When & Then (예외가 외부로 전파되지 않아야 함 - 로그만 남김)
        emailService.sendNewMessageEmail(
                "test@example.com",
                "테스터",
                "채용담당자",
                SenderType.RECRUITER,
                "session-token",
                1,
                "메시지"
        );

        // 발송 시도는 했지만 실패 (예외가 catch되어 정상 종료됨)
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
