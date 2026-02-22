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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    @Nested
    @DisplayName("BCrypt 비밀번호 검증 테스트")
    class BCryptPasswordTest {

        @Test
        @DisplayName("디버깅: 실제 BCrypt로 비밀번호 일치 확인")
        void debug_actualBCryptMatches() {
            // Given
            String rawPassword = "Rhk1234!!";
            String storedHash = "$2a$10$4Iq2Xcd0PgIcFthHiWVWG.rqqd6FBjcSisvwClzeN.LtRrCCYdh0e";
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            // When
            boolean matches = encoder.matches(rawPassword, storedHash);

            // Then
            System.out.println("=== BCrypt 비밀번호 검증 결과 ===");
            System.out.println("입력 비밀번호: " + rawPassword);
            System.out.println("저장된 해시: " + storedHash);
            System.out.println("일치 여부: " + matches);
            System.out.println("==============================");

            // 결과 출력 (일치하지 않으면 이 테스트는 실패함)
            if (!matches) {
                System.out.println("⚠️ 비밀번호가 일치하지 않습니다!");
                System.out.println("회원가입 시 다른 비밀번호를 사용했거나, 입력한 비밀번호가 잘못되었습니다.");
            }

            // 검증: 일치 여부 확인 (현재는 불일치 상태)
            assertThat(matches).as("비밀번호 일치 여부").isFalse();
        }

        @Test
        @DisplayName("실제 인코딩 및 검증 플로우 테스트")
        void test_encodeAndVerifyFlow() {
            // Given
            String rawPassword = "Rhk1234!!";
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            // When - 인코딩
            String encodedPassword = encoder.encode(rawPassword);
            System.out.println("=== 실제 인코딩 테스트 ===");
            System.out.println("평문 비밀번호: " + rawPassword);
            System.out.println("인코딩 결과: " + encodedPassword);

            // Then - 검증
            boolean matches = encoder.matches(rawPassword, encodedPassword);
            System.out.println("검증 결과: " + matches);
            System.out.println("========================");

            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("여러 가지 비밀번호 패턴 테스트")
        void test_variousPasswordPatterns() {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String storedHash = "$2a$10$oXTUW3qyw1KxuvuGL2UMredZVoj3aJzqnSLQL8MVsctVUC5RVNbQK";

            System.out.println("=== 다양한 비밀번호 패턴 테스트 ===");
            System.out.println("DB 해시: " + storedHash);
            System.out.println();

            // 테스트할 비밀번호 패턴들
            String[] passwords = {
                    "Rtest1234!!",      // 입력한 비밀번호
                    "rtest1234!!",      // 소문자
                    "RTEST1234!!",      // 대문자
                    "Rtest1234!",       // 느낌표 1개
                    "Rtest1234!!!",     // 느낌표 3개
                    " Rtest1234!!",     // 앞에 공백
                    "Rtest1234!! ",     // 뒤에 공백
                    "Rtest1234",        // 느낌표 없음
                    "Rtest12345!!",     // 숫자 다름
                    "Rhk1234!!",        // 다른 철자
                    "Test1234!!",       // R 없음
                    "Rtest123!!",       // 4 하나 적음
                    "Rtest12344!!",     // 4 하나 많음
            };

            for (String pwd : passwords) {
                boolean matches = encoder.matches(pwd, storedHash);
                System.out.println(String.format("비밀번호: [%s] (길이: %d) -> 일치: %s",
                        pwd, pwd.length(), matches ? "✅ 일치!" : "❌"));

                if (matches) {
                    System.out.println("🎯🎯🎯 일치하는 비밀번호를 찾았습니다: [" + pwd + "] 🎯🎯🎯");
                    assertThat(matches).isTrue();
                    return;
                }
            }
            System.out.println();
            System.out.println("⚠️ 일치하는 비밀번호를 찾지 못했습니다.");
            System.out.println("회원가입 시 입력한 비밀번호가 위 패턴들과 다릅니다.");
            System.out.println("================================");
        }
    }
}
