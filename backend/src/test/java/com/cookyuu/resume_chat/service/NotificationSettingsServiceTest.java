package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.NotificationSettings;
import com.cookyuu.resume_chat.dto.NotificationSettingsDto;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.repository.NotificationSettingsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingsService 테스트")
class NotificationSettingsServiceTest {

    @Mock
    private NotificationSettingsRepository notificationSettingsRepository;

    @Mock
    private ApplicantRepository applicantRepository;

    @InjectMocks
    private NotificationSettingsService notificationSettingsService;

    @Test
    @DisplayName("알림 설정 조회 - 기존 설정이 있는 경우")
    void getSettings_ExistingSettings_Success() {
        // given
        UUID applicantUuid = UUID.randomUUID();
        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(applicantUuid)
                .email("test@example.com")
                .name("테스트")
                .password("encoded")
                .build();

        NotificationSettings settings = NotificationSettings.builder()
                .id(1L)
                .applicant(applicant)
                .emailNewMessage(true)
                .emailNewSession(false)
                .pushNewMessage(true)
                .build();

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.of(applicant));
        given(notificationSettingsRepository.findByApplicant(applicant)).willReturn(Optional.of(settings));

        // when
        NotificationSettingsDto.Response response = notificationSettingsService.getSettings(applicantUuid);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmailNewMessage()).isTrue();
        assertThat(response.getEmailNewSession()).isFalse();
        assertThat(response.getPushNewMessage()).isTrue();

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(notificationSettingsRepository, times(1)).findByApplicant(applicant);
        verify(notificationSettingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("알림 설정 조회 - 설정이 없는 경우 기본 설정 생성")
    void getSettings_NoSettings_CreateDefault() {
        // given
        UUID applicantUuid = UUID.randomUUID();
        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(applicantUuid)
                .email("test@example.com")
                .name("테스트")
                .password("encoded")
                .build();

        NotificationSettings defaultSettings = NotificationSettings.createDefaultSettings(applicant);

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.of(applicant));
        given(notificationSettingsRepository.findByApplicant(applicant)).willReturn(Optional.empty());
        given(notificationSettingsRepository.save(any(NotificationSettings.class))).willReturn(defaultSettings);

        // when
        NotificationSettingsDto.Response response = notificationSettingsService.getSettings(applicantUuid);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmailNewMessage()).isTrue();
        assertThat(response.getEmailNewSession()).isTrue();
        assertThat(response.getPushNewMessage()).isFalse();

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(notificationSettingsRepository, times(1)).findByApplicant(applicant);
        verify(notificationSettingsRepository, times(1)).save(any(NotificationSettings.class));
    }

    @Test
    @DisplayName("알림 설정 조회 - 지원자를 찾을 수 없는 경우")
    void getSettings_ApplicantNotFound_ThrowsException() {
        // given
        UUID applicantUuid = UUID.randomUUID();
        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationSettingsService.getSettings(applicantUuid))
                .isInstanceOf(BusinessException.class);

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(notificationSettingsRepository, never()).findByApplicant(any());
    }

    @Test
    @DisplayName("알림 설정 업데이트 - 기존 설정 업데이트")
    void updateSettings_ExistingSettings_Success() {
        // given
        UUID applicantUuid = UUID.randomUUID();
        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(applicantUuid)
                .email("test@example.com")
                .name("테스트")
                .password("encoded")
                .build();

        NotificationSettings settings = NotificationSettings.builder()
                .id(1L)
                .applicant(applicant)
                .emailNewMessage(true)
                .emailNewSession(true)
                .pushNewMessage(false)
                .build();

        NotificationSettingsDto.UpdateRequest request = NotificationSettingsDto.UpdateRequest.builder()
                .emailNewMessage(false)
                .emailNewSession(true)
                .pushNewMessage(true)
                .build();

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.of(applicant));
        given(notificationSettingsRepository.findByApplicant(applicant)).willReturn(Optional.of(settings));

        // when
        NotificationSettingsDto.Response response = notificationSettingsService.updateSettings(applicantUuid, request);

        // then
        assertThat(response).isNotNull();
        assertThat(settings.getEmailNewMessage()).isFalse();
        assertThat(settings.getEmailNewSession()).isTrue();
        assertThat(settings.getPushNewMessage()).isTrue();

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(notificationSettingsRepository, times(1)).findByApplicant(applicant);
    }

    @Test
    @DisplayName("알림 설정 업데이트 - 설정이 없는 경우 생성 후 업데이트")
    void updateSettings_NoSettings_CreateAndUpdate() {
        // given
        UUID applicantUuid = UUID.randomUUID();
        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(applicantUuid)
                .email("test@example.com")
                .name("테스트")
                .password("encoded")
                .build();

        NotificationSettings defaultSettings = NotificationSettings.createDefaultSettings(applicant);

        NotificationSettingsDto.UpdateRequest request = NotificationSettingsDto.UpdateRequest.builder()
                .emailNewMessage(false)
                .emailNewSession(false)
                .pushNewMessage(true)
                .build();

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.of(applicant));
        given(notificationSettingsRepository.findByApplicant(applicant)).willReturn(Optional.empty());
        given(notificationSettingsRepository.save(any(NotificationSettings.class))).willReturn(defaultSettings);

        // when
        NotificationSettingsDto.Response response = notificationSettingsService.updateSettings(applicantUuid, request);

        // then
        assertThat(response).isNotNull();

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(notificationSettingsRepository, times(1)).findByApplicant(applicant);
        verify(notificationSettingsRepository, times(1)).save(any(NotificationSettings.class));
    }

    @Test
    @DisplayName("알림 활성화 확인 - 설정이 있는 경우")
    void isNotificationEnabled_WithSettings_ReturnsCorrectValue() {
        // given
        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .name("테스트")
                .password("encoded")
                .build();

        NotificationSettings settings = NotificationSettings.builder()
                .id(1L)
                .applicant(applicant)
                .emailNewMessage(true)
                .emailNewSession(false)
                .pushNewMessage(true)
                .build();

        given(notificationSettingsRepository.findByApplicant(applicant)).willReturn(Optional.of(settings));

        // when & then
        assertThat(notificationSettingsService.isNotificationEnabled(applicant, "emailNewMessage")).isTrue();
        assertThat(notificationSettingsService.isNotificationEnabled(applicant, "emailNewSession")).isFalse();
        assertThat(notificationSettingsService.isNotificationEnabled(applicant, "pushNewMessage")).isTrue();
    }

    @Test
    @DisplayName("알림 활성화 확인 - 설정이 없는 경우 기본값 반환")
    void isNotificationEnabled_NoSettings_ReturnsDefaultValue() {
        // given
        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .name("테스트")
                .password("encoded")
                .build();

        given(notificationSettingsRepository.findByApplicant(applicant)).willReturn(Optional.empty());

        // when & then
        assertThat(notificationSettingsService.isNotificationEnabled(applicant, "emailNewMessage")).isTrue();
        assertThat(notificationSettingsService.isNotificationEnabled(applicant, "emailNewSession")).isTrue();
        assertThat(notificationSettingsService.isNotificationEnabled(applicant, "pushNewMessage")).isFalse();
    }
}
