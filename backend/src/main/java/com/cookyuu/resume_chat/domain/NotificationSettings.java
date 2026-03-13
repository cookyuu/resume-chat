package com.cookyuu.resume_chat.domain;

import com.cookyuu.resume_chat.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 설정 엔티티
 *
 * <p>지원자별 알림 수신 설정을 관리합니다.</p>
 *
 * <h3>지원하는 알림 타입</h3>
 * <ul>
 *   <li>이메일 알림 - 새 메시지</li>
 *   <li>이메일 알림 - 새 세션</li>
 *   <li>브라우저 푸시 알림 - 새 메시지</li>
 * </ul>
 *
 * <h3>기본 설정</h3>
 * <ul>
 *   <li>emailNewMessage: true (활성화)</li>
 *   <li>emailNewSession: true (활성화)</li>
 *   <li>pushNewMessage: false (비활성화)</li>
 * </ul>
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "rc_notification_settings")
public class NotificationSettings extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 지원자 (1:1 관계)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false, unique = true)
    private Applicant applicant;

    /**
     * 이메일 알림 - 새 메시지
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailNewMessage = true;

    /**
     * 이메일 알림 - 새 세션
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailNewSession = true;

    /**
     * 브라우저 푸시 알림 - 새 메시지
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean pushNewMessage = false;

    /**
     * 기본 설정으로 새로운 NotificationSettings 생성
     *
     * @param applicant 지원자
     * @return 기본 설정이 적용된 NotificationSettings
     */
    public static NotificationSettings createDefaultSettings(Applicant applicant) {
        return NotificationSettings.builder()
                .applicant(applicant)
                .emailNewMessage(true)
                .emailNewSession(true)
                .pushNewMessage(false)
                .build();
    }

    /**
     * 알림 설정 업데이트
     *
     * @param emailNewMessage 이메일 알림 - 새 메시지
     * @param emailNewSession 이메일 알림 - 새 세션
     * @param pushNewMessage 브라우저 푸시 알림 - 새 메시지
     */
    public void updateSettings(Boolean emailNewMessage, Boolean emailNewSession, Boolean pushNewMessage) {
        if (emailNewMessage != null) {
            this.emailNewMessage = emailNewMessage;
        }
        if (emailNewSession != null) {
            this.emailNewSession = emailNewSession;
        }
        if (pushNewMessage != null) {
            this.pushNewMessage = pushNewMessage;
        }
    }
}
