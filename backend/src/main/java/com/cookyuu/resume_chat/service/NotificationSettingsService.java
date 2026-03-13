package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.NotificationSettings;
import com.cookyuu.resume_chat.dto.NotificationSettingsDto;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 알림 설정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository notificationSettingsRepository;
    private final ApplicantRepository applicantRepository;

    /**
     * 알림 설정 조회
     *
     * <p>알림 설정이 없으면 기본 설정을 생성하여 반환합니다.</p>
     *
     * @param applicantUuid 지원자 UUID
     * @return 알림 설정 응답 DTO
     * @throws BusinessException 지원자를 찾을 수 없는 경우
     */
    @Transactional
    public NotificationSettingsDto.Response getSettings(UUID applicantUuid) {
        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        NotificationSettings settings = notificationSettingsRepository.findByApplicant(applicant)
                .orElseGet(() -> {
                    NotificationSettings defaultSettings = NotificationSettings.createDefaultSettings(applicant);
                    NotificationSettings saved = notificationSettingsRepository.save(defaultSettings);
                    log.info("기본 알림 설정 생성 완료 - applicantUuid: {}", applicantUuid);
                    return saved;
                });

        log.info("알림 설정 조회 완료 - applicantUuid: {}, emailNewMessage: {}, emailNewSession: {}, pushNewMessage: {}",
                applicantUuid, settings.getEmailNewMessage(), settings.getEmailNewSession(), settings.getPushNewMessage());

        return NotificationSettingsDto.Response.from(settings);
    }

    /**
     * 알림 설정 업데이트
     *
     * @param applicantUuid 지원자 UUID
     * @param request 업데이트 요청 DTO
     * @return 업데이트된 알림 설정 응답 DTO
     * @throws BusinessException 지원자를 찾을 수 없는 경우
     */
    @Transactional
    public NotificationSettingsDto.Response updateSettings(UUID applicantUuid, NotificationSettingsDto.UpdateRequest request) {
        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        NotificationSettings settings = notificationSettingsRepository.findByApplicant(applicant)
                .orElseGet(() -> {
                    NotificationSettings defaultSettings = NotificationSettings.createDefaultSettings(applicant);
                    return notificationSettingsRepository.save(defaultSettings);
                });

        settings.updateSettings(
                request.getEmailNewMessage(),
                request.getEmailNewSession(),
                request.getPushNewMessage()
        );

        log.info("알림 설정 업데이트 완료 - applicantUuid: {}, emailNewMessage: {}, emailNewSession: {}, pushNewMessage: {}",
                applicantUuid, settings.getEmailNewMessage(), settings.getEmailNewSession(), settings.getPushNewMessage());

        return NotificationSettingsDto.Response.from(settings);
    }

    /**
     * 특정 알림 타입이 활성화되어 있는지 확인
     *
     * @param applicant 지원자
     * @param notificationType 알림 타입 (emailNewMessage, emailNewSession, pushNewMessage)
     * @return 활성화 여부
     */
    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(Applicant applicant, String notificationType) {
        NotificationSettings settings = notificationSettingsRepository.findByApplicant(applicant)
                .orElse(null);

        if (settings == null) {
            // 설정이 없으면 기본값 반환
            return switch (notificationType) {
                case "emailNewMessage", "emailNewSession" -> true;
                case "pushNewMessage" -> false;
                default -> false;
            };
        }

        return switch (notificationType) {
            case "emailNewMessage" -> settings.getEmailNewMessage();
            case "emailNewSession" -> settings.getEmailNewSession();
            case "pushNewMessage" -> settings.getPushNewMessage();
            default -> false;
        };
    }
}
