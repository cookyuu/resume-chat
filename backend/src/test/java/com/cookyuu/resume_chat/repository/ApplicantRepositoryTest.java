package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.common.enums.ApplicantStatus;
import com.cookyuu.resume_chat.config.TestJpaConfig;
import com.cookyuu.resume_chat.entity.Applicant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaConfig.class)
@DisplayName("ApplicantRepository 테스트")
class ApplicantRepositoryTest {

    @Autowired
    private ApplicantRepository applicantRepository;

    @Nested
    @DisplayName("existsByEmail - 이메일 중복 체크")
    class ExistsByEmail {

        @Test
        @DisplayName("성공: 존재하는 이메일 조회 시 true 반환")
        void success_existingEmail_returnsTrue() {
            // Given
            String email = "test@example.com";
            Applicant applicant = Applicant.builder()
                    .uuid(UUID.randomUUID())
                    .email(email)
                    .name("홍길동")
                    .password("encodedPassword")
                    .status(ApplicantStatus.ACTIVE)
                    .loginFailCnt(0)
                    .build();
            applicantRepository.save(applicant);

            // When
            boolean exists = applicantRepository.existsByEmail(email);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("성공: 존재하지 않는 이메일 조회 시 false 반환")
        void success_nonExistingEmail_returnsFalse() {
            // Given
            String email = "nonexistent@example.com";

            // When
            boolean exists = applicantRepository.existsByEmail(email);

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("save - 엔티티 저장")
    class Save {

        @Test
        @DisplayName("성공: Applicant 저장 및 조회")
        void success_saveAndFind() {
            // Given
            Applicant applicant = Applicant.builder()
                    .uuid(UUID.randomUUID())
                    .email("test@example.com")
                    .name("홍길동")
                    .password("encodedPassword")
                    .status(ApplicantStatus.ACTIVE)
                    .loginFailCnt(0)
                    .build();

            // When
            Applicant savedApplicant = applicantRepository.save(applicant);

            // Then
            assertThat(savedApplicant).isNotNull();
            assertThat(savedApplicant.getId()).isNotNull();
            assertThat(savedApplicant.getEmail()).isEqualTo("test@example.com");
            assertThat(savedApplicant.getName()).isEqualTo("홍길동");
            assertThat(savedApplicant.getPassword()).isEqualTo("encodedPassword");
            assertThat(savedApplicant.getStatus()).isEqualTo(ApplicantStatus.ACTIVE);
            assertThat(savedApplicant.getLoginFailCnt()).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 저장 후 ID로 조회 가능")
        void success_saveAndFindById() {
            // Given
            Applicant applicant = Applicant.builder()
                    .uuid(UUID.randomUUID())
                    .email("test@example.com")
                    .name("홍길동")
                    .password("encodedPassword")
                    .status(ApplicantStatus.ACTIVE)
                    .loginFailCnt(0)
                    .build();

            // When
            Applicant savedApplicant = applicantRepository.save(applicant);
            Applicant foundApplicant = applicantRepository.findById(savedApplicant.getId()).orElse(null);

            // Then
            assertThat(foundApplicant).isNotNull();
            assertThat(foundApplicant.getId()).isEqualTo(savedApplicant.getId());
            assertThat(foundApplicant.getEmail()).isEqualTo(savedApplicant.getEmail());
        }

        @Test
        @DisplayName("성공: createNewApplicant로 생성한 엔티티 저장")
        void success_saveEntityCreatedByFactory() {
            // Given
            Applicant applicant = Applicant.createNewApplicant(
                    "test@example.com",
                    "홍길동",
                    "encodedPassword"
            );

            // When
            Applicant savedApplicant = applicantRepository.save(applicant);

            // Then
            assertThat(savedApplicant).isNotNull();
            assertThat(savedApplicant.getId()).isNotNull();
            assertThat(savedApplicant.getUuid()).isNotNull();
            assertThat(savedApplicant.getEmail()).isEqualTo("test@example.com");
            assertThat(savedApplicant.getName()).isEqualTo("홍길동");
            assertThat(savedApplicant.getStatus()).isEqualTo(ApplicantStatus.ACTIVE);
            assertThat(savedApplicant.getLoginFailCnt()).isEqualTo(0);
        }
    }
}
