package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 알림 설정 Repository
 */
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    /**
     * 지원자로 알림 설정 조회
     *
     * @param applicant 지원자
     * @return 알림 설정 (Optional)
     */
    Optional<NotificationSettings> findByApplicant(Applicant applicant);

    /**
     * 지원자 ID로 알림 설정 존재 여부 확인
     *
     * @param applicant 지원자
     * @return 존재 여부
     */
    boolean existsByApplicant(Applicant applicant);
}
