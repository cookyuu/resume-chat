package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.command.ApplicantCommand;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.entity.Applicant;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicantService 테스트")
class ApplicantServiceTest {

    @InjectMocks
    private ApplicantService applicantService;

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("joinApplicant - 회원가입")
    class JoinApplicant {

        @Test
        @DisplayName("성공: 유효한 정보로 회원가입 시 엔티티 저장")
        void success_validCommand_savesApplicant() {
            // Given
            ApplicantCommand.Create command = new ApplicantCommand.Create(
                    "test@example.com",
                    "password123",
                    "홍길동"
            );
            when(applicantRepository.existsByEmail(command.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(command.getPassword())).thenReturn("encodedPassword123");

            // When
            applicantService.joinApplicant(command);

            // Then
            verify(applicantRepository, times(1)).existsByEmail(command.getEmail());
            verify(passwordEncoder, times(1)).encode(command.getPassword());
            verify(applicantRepository, times(1)).save(any(Applicant.class));
        }

        @Test
        @DisplayName("성공: 비밀번호가 암호화되어 저장됨")
        void success_passwordEncoded() {
            // Given
            ApplicantCommand.Create command = new ApplicantCommand.Create(
                    "test@example.com",
                    "password123",
                    "홍길동"
            );
            String encodedPassword = "encodedPassword123";
            when(applicantRepository.existsByEmail(command.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(command.getPassword())).thenReturn(encodedPassword);

            ArgumentCaptor<Applicant> applicantCaptor = ArgumentCaptor.forClass(Applicant.class);

            // When
            applicantService.joinApplicant(command);

            // Then
            verify(applicantRepository).save(applicantCaptor.capture());
            Applicant savedApplicant = applicantCaptor.getValue();

            assertThat(savedApplicant.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedApplicant.getEmail()).isEqualTo(command.getEmail());
            assertThat(savedApplicant.getName()).isEqualTo(command.getName());
        }

        @Test
        @DisplayName("성공: ArgumentCaptor로 저장되는 엔티티 필드 검증")
        void success_verifySavedEntityFields() {
            // Given
            ApplicantCommand.Create command = new ApplicantCommand.Create(
                    "test@example.com",
                    "password123",
                    "홍길동"
            );
            when(applicantRepository.existsByEmail(command.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(command.getPassword())).thenReturn("encodedPassword");

            ArgumentCaptor<Applicant> applicantCaptor = ArgumentCaptor.forClass(Applicant.class);

            // When
            applicantService.joinApplicant(command);

            // Then
            verify(applicantRepository).save(applicantCaptor.capture());
            Applicant savedApplicant = applicantCaptor.getValue();

            assertThat(savedApplicant).isNotNull();
            assertThat(savedApplicant.getEmail()).isEqualTo("test@example.com");
            assertThat(savedApplicant.getName()).isEqualTo("홍길동");
            assertThat(savedApplicant.getPassword()).isEqualTo("encodedPassword");
            assertThat(savedApplicant.getUuid()).isNotNull();
            assertThat(savedApplicant.getStatus()).isNotNull();
            assertThat(savedApplicant.getLoginFailCnt()).isEqualTo(0);
        }

        @Test
        @DisplayName("실패: 이메일 중복 시 BusinessException 발생")
        void fail_duplicateEmail_throwsBusinessException() {
            // Given
            ApplicantCommand.Create command = new ApplicantCommand.Create(
                    "duplicate@example.com",
                    "password123",
                    "홍길동"
            );
            when(applicantRepository.existsByEmail(command.getEmail())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> applicantService.joinApplicant(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.APPLICANT_ALREADY_EXISTS.getMessage())
                    .satisfies(ex -> {
                        BusinessException businessException = (BusinessException) ex;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.APPLICANT_ALREADY_EXISTS);
                        assertThat(businessException.getErrorCode().getCode()).isEqualTo("A002");
                    });

            verify(applicantRepository, times(1)).existsByEmail(command.getEmail());
            verify(passwordEncoder, never()).encode(anyString());
            verify(applicantRepository, never()).save(any(Applicant.class));
        }

        @Test
        @DisplayName("검증: Repository 호출 횟수 검증")
        void verify_repositoryCalls() {
            // Given
            ApplicantCommand.Create command = new ApplicantCommand.Create(
                    "test@example.com",
                    "password123",
                    "홍길동"
            );
            when(applicantRepository.existsByEmail(command.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(command.getPassword())).thenReturn("encoded");

            // When
            applicantService.joinApplicant(command);

            // Then
            verify(applicantRepository, times(1)).existsByEmail(command.getEmail());
            verify(applicantRepository, times(1)).save(any(Applicant.class));
            verifyNoMoreInteractions(applicantRepository);
        }
    }
}
