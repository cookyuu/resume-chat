package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.domain.ChatSession;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

/**
 * 이메일 알림 서비스
 *
 * <h3>알림 발송 정책</h3>
 * <ul>
 *   <li>읽지 않은 메시지 최초 발생 → 5분 타이머 시작</li>
 *   <li>5분 이내 읽으면 → 타이머 취소, 알림 발송 안 함</li>
 *   <li>5분 경과 후 → 1회만 알림 발송 (여러 메시지 포함)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.notification-delay-minutes}")
    private int notificationDelayMinutes;

    // 세션별 지연 알림 관리 (sessionToken -> PendingNotification)
    private final ConcurrentHashMap<String, PendingNotification> pendingNotifications = new ConcurrentHashMap<>();

    // 스케줄링용 Executor
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 새 메시지 알림 스케줄링
     *
     * <p>읽지 않은 메시지가 발생하면 5분 후 알림 발송을 예약합니다.
     * 이미 예약된 알림이 있으면 메시지 수만 증가시킵니다.</p>
     *
     * @param sessionToken 채팅 세션 토큰
     * @param recipientEmail 수신자 이메일
     * @param recipientName 수신자 이름
     * @param senderName 발신자 이름
     * @param senderType 발신자 타입
     * @param messagePreview 메시지 미리보기
     */
    public void scheduleNewMessageNotification(
            String sessionToken,
            String recipientEmail,
            String recipientName,
            String senderName,
            SenderType senderType,
            String messagePreview
    ) {
        pendingNotifications.compute(sessionToken, (key, existing) -> {
            if (existing != null && !existing.future.isDone()) {
                // 이미 예약된 알림이 있으면 메시지 수만 증가
                existing.incrementMessageCount();
                existing.updateLastMessage(messagePreview);
                log.debug("기존 알림에 메시지 추가 - sessionToken: {}, 총 메시지 수: {}",
                        sessionToken, existing.getUnreadCount());
                return existing;
            }

            // 새로운 알림 예약
            PendingNotification notification = new PendingNotification(
                    sessionToken,
                    recipientEmail,
                    recipientName,
                    senderName,
                    senderType,
                    messagePreview
            );

            // 5분 후 실행되는 작업 스케줄링
            ScheduledFuture<?> future = scheduler.schedule(
                    () -> sendDelayedNotification(notification),
                    notificationDelayMinutes,
                    TimeUnit.MINUTES
            );

            notification.setFuture(future);

            log.info("새 메시지 알림 예약 - sessionToken: {}, 수신자: {}, {}분 후 발송",
                    sessionToken, recipientEmail, notificationDelayMinutes);

            return notification;
        });
    }

    /**
     * 알림 취소 (메시지 읽음 처리 시 호출)
     *
     * @param sessionToken 채팅 세션 토큰
     */
    public void cancelNotification(String sessionToken) {
        PendingNotification notification = pendingNotifications.remove(sessionToken);
        if (notification != null && !notification.future.isDone()) {
            notification.future.cancel(false);
            log.info("알림 취소됨 - sessionToken: {}, 메시지가 읽혔습니다", sessionToken);
        }
    }

    /**
     * 지연된 알림 실제 발송
     */
    private void sendDelayedNotification(PendingNotification notification) {
        try {
            // 발송 전 pendingNotifications에서 제거
            pendingNotifications.remove(notification.sessionToken);

            // 실제 이메일 발송
            sendNewMessageEmail(
                    notification.recipientEmail,
                    notification.recipientName,
                    notification.senderName,
                    notification.senderType,
                    notification.sessionToken,
                    notification.getUnreadCount(),
                    notification.getLastMessage()
            );

            log.info("지연 알림 발송 완료 - sessionToken: {}, 수신자: {}, 메시지 수: {}",
                    notification.sessionToken, notification.recipientEmail, notification.getUnreadCount());

        } catch (Exception e) {
            log.error("지연 알림 발송 실패 - sessionToken: {}, error: {}",
                    notification.sessionToken, e.getMessage(), e);
        }
    }

    /**
     * 새 메시지 알림 이메일 발송
     *
     * @param recipientEmail 수신자 이메일
     * @param recipientName 수신자 이름
     * @param senderName 발신자 이름
     * @param senderType 발신자 타입
     * @param sessionToken 채팅 세션 토큰
     * @param unreadCount 읽지 않은 메시지 수
     * @param messagePreview 메시지 미리보기
     */
    @Async("emailTaskExecutor")
    public void sendNewMessageEmail(
            String recipientEmail,
            String recipientName,
            String senderName,
            SenderType senderType,
            String sessionToken,
            int unreadCount,
            String messagePreview
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject(String.format("[Resume Chat] %s님이 새 메시지를 보냈습니다", senderName));

            String htmlContent = buildNewMessageHtml(
                    recipientName,
                    senderName,
                    senderType,
                    sessionToken,
                    unreadCount,
                    messagePreview
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("새 메시지 알림 이메일 발송 성공 - 수신자: {}, 발신자: {}", recipientEmail, senderName);

        } catch (Exception e) {
            log.error("새 메시지 알림 이메일 발송 실패 - 수신자: {}, error: {}", recipientEmail, e.getMessage(), e);
        }
    }

    /**
     * 신규 세션 생성 알림 이메일 발송
     *
     * @param session 채팅 세션
     * @param firstMessage 첫 메시지 내용
     */
    @Async("emailTaskExecutor")
    public void sendNewSessionNotification(ChatSession session, String firstMessage) {
        try {
            String recipientEmail = session.getResume().getApplicant().getEmail();
            String recipientName = session.getResume().getApplicant().getName();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject(String.format("[Resume Chat] %s에서 채팅 요청이 도착했습니다", session.getRecruiterCompany()));

            String htmlContent = buildNewSessionHtml(
                    recipientName,
                    session.getRecruiterName(),
                    session.getRecruiterCompany(),
                    session.getRecruiterEmail(),
                    session.getResume().getTitle(),
                    session.getSessionToken(),
                    firstMessage
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("신규 세션 알림 이메일 발송 성공 - 수신자: {}, 회사: {}",
                    recipientEmail, session.getRecruiterCompany());

        } catch (Exception e) {
            log.error("신규 세션 알림 이메일 발송 실패 - sessionToken: {}, error: {}",
                    session.getSessionToken(), e.getMessage(), e);
        }
    }

    /**
     * 새 메시지 HTML 이메일 생성
     */
    private String buildNewMessageHtml(
            String recipientName,
            String senderName,
            SenderType senderType,
            String sessionToken,
            int unreadCount,
            String messagePreview
    ) {
        String chatUrl = frontendUrl + "/chat/" + sessionToken;
        String senderTypeKorean = senderType == SenderType.RECRUITER ? "채용담당자" : "지원자";
        String messageCountText = unreadCount > 1 ?
                String.format("외 %d개의 메시지", unreadCount - 1) : "";

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #f5f5f5;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f5f5; padding: 40px 0;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="padding: 40px 40px 20px; text-align: center; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 8px 8px 0 0;">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 600;">Resume Chat</h1>
                                            <p style="margin: 10px 0 0; color: #ffffff; font-size: 14px; opacity: 0.9;">새 메시지가 도착했습니다</p>
                                        </td>
                                    </tr>

                                    <!-- Content -->
                                    <tr>
                                        <td style="padding: 40px;">
                                            <p style="margin: 0 0 20px; font-size: 16px; line-height: 1.6; color: #333333;">
                                                안녕하세요 <strong>%s</strong>님,
                                            </p>
                                            <p style="margin: 0 0 30px; font-size: 16px; line-height: 1.6; color: #333333;">
                                                <strong>%s</strong>(%s)님이 메시지를 보냈습니다.
                                            </p>

                                            <!-- Message Preview -->
                                            <div style="background-color: #f8f9fa; border-left: 4px solid #667eea; padding: 20px; margin: 0 0 30px; border-radius: 4px;">
                                                <p style="margin: 0; font-size: 14px; color: #666666; font-weight: 600; margin-bottom: 8px;">메시지 미리보기</p>
                                                <p style="margin: 0; font-size: 15px; line-height: 1.6; color: #333333;">%s</p>
                                                <p style="margin: 8px 0 0; font-size: 13px; color: #999999;">%s</p>
                                            </div>

                                            <!-- CTA Button -->
                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" style="display: inline-block; padding: 14px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);">
                                                            채팅 확인하기
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding: 30px 40px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; text-align: center;">
                                            <p style="margin: 0 0 8px; font-size: 13px; color: #999999;">
                                                이 이메일은 Resume Chat에서 자동으로 발송되었습니다.
                                            </p>
                                            <p style="margin: 0; font-size: 13px; color: #999999;">
                                                © %d Resume Chat. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                recipientName,
                senderName,
                senderTypeKorean,
                messagePreview,
                messageCountText,
                chatUrl,
                LocalDateTime.now().getYear()
        );
    }

    /**
     * 신규 세션 HTML 이메일 생성
     */
    private String buildNewSessionHtml(
            String recipientName,
            String recruiterName,
            String recruiterCompany,
            String recruiterEmail,
            String resumeTitle,
            String sessionToken,
            String firstMessage
    ) {
        String chatUrl = frontendUrl + "/chat/" + sessionToken;

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #f5f5f5;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f5f5; padding: 40px 0;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="padding: 40px 40px 20px; text-align: center; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 8px 8px 0 0;">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 600;">Resume Chat</h1>
                                            <p style="margin: 10px 0 0; color: #ffffff; font-size: 14px; opacity: 0.9;">🎉 새로운 채팅 요청이 도착했습니다</p>
                                        </td>
                                    </tr>

                                    <!-- Content -->
                                    <tr>
                                        <td style="padding: 40px;">
                                            <p style="margin: 0 0 20px; font-size: 16px; line-height: 1.6; color: #333333;">
                                                안녕하세요 <strong>%s</strong>님,
                                            </p>
                                            <p style="margin: 0 0 30px; font-size: 16px; line-height: 1.6; color: #333333;">
                                                <strong>%s</strong>에서 회원님의 이력서에 관심을 표했습니다.
                                            </p>

                                            <!-- Company Info -->
                                            <div style="background-color: #f8f9fa; padding: 24px; margin: 0 0 24px; border-radius: 8px; border: 1px solid #e9ecef;">
                                                <table width="100%%" cellpadding="0" cellspacing="0">
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #666666; width: 100px;">회사</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #333333; font-weight: 600;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #666666;">담당자</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #333333; font-weight: 600;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #666666;">이메일</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #333333;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #666666;">이력서</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #333333; font-weight: 600;">%s</td>
                                                    </tr>
                                                </table>
                                            </div>

                                            <!-- First Message -->
                                            <div style="background-color: #f8f9fa; border-left: 4px solid #667eea; padding: 20px; margin: 0 0 30px; border-radius: 4px;">
                                                <p style="margin: 0; font-size: 14px; color: #666666; font-weight: 600; margin-bottom: 8px;">첫 메시지</p>
                                                <p style="margin: 0; font-size: 15px; line-height: 1.6; color: #333333;">%s</p>
                                            </div>

                                            <!-- CTA Button -->
                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" style="display: inline-block; padding: 14px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);">
                                                            채팅 시작하기
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding: 30px 40px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; text-align: center;">
                                            <p style="margin: 0 0 8px; font-size: 13px; color: #999999;">
                                                이 이메일은 Resume Chat에서 자동으로 발송되었습니다.
                                            </p>
                                            <p style="margin: 0; font-size: 13px; color: #999999;">
                                                © %d Resume Chat. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                recipientName,
                recruiterCompany,
                recruiterCompany,
                recruiterName,
                recruiterEmail,
                resumeTitle,
                firstMessage,
                chatUrl,
                LocalDateTime.now().getYear()
        );
    }

    /**
     * 지연 알림 정보를 담는 내부 클래스
     */
    private static class PendingNotification {
        private final String sessionToken;
        private final String recipientEmail;
        private final String recipientName;
        private final String senderName;
        private final SenderType senderType;
        private String lastMessage;
        private int unreadCount;
        private ScheduledFuture<?> future;

        public PendingNotification(
                String sessionToken,
                String recipientEmail,
                String recipientName,
                String senderName,
                SenderType senderType,
                String lastMessage
        ) {
            this.sessionToken = sessionToken;
            this.recipientEmail = recipientEmail;
            this.recipientName = recipientName;
            this.senderName = senderName;
            this.senderType = senderType;
            this.lastMessage = lastMessage;
            this.unreadCount = 1;
        }

        public void incrementMessageCount() {
            this.unreadCount++;
        }

        public void updateLastMessage(String message) {
            this.lastMessage = message;
        }

        public void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        public int getUnreadCount() {
            return unreadCount;
        }

        public String getLastMessage() {
            return lastMessage;
        }
    }
}
