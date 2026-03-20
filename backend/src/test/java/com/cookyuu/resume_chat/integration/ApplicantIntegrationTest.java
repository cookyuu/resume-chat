package com.cookyuu.resume_chat.integration;

import com.cookyuu.resume_chat.common.enums.ApplicantStatus;
import com.cookyuu.resume_chat.dto.ApplicantDto;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Applicant 통합 테스트")
class ApplicantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        applicantRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/applicant/join - 회원가입 E2E")
    class JoinApplicantE2E {

        @Test
        @DisplayName("성공: 전체 플로우 성공 케이스 (Controller → Service → Repository)")
        @Transactional
        void success_fullFlow() throws Exception {
            // Given
            ApplicantDto.JoinRequest request = new ApplicantDto.JoinRequest(
                    "test@example.com",
                    "password123",
                    "홍길동"
            );

            // When
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").exists());

            // Then - DB 검증
            List<Applicant> applicants = applicantRepository.findAll();
            assertThat(applicants).hasSize(1);

            Applicant savedApplicant = applicants.get(0);
            assertThat(savedApplicant.getEmail()).isEqualTo("test@example.com");
            assertThat(savedApplicant.getName()).isEqualTo("홍길동");
            assertThat(savedApplicant.getUuid()).isNotNull();
            assertThat(savedApplicant.getStatus()).isEqualTo(ApplicantStatus.ACTIVE);
            assertThat(savedApplicant.getLoginFailCnt()).isEqualTo(0);

            // 비밀번호 암호화 검증
            assertThat(passwordEncoder.matches("password123", savedApplicant.getPassword())).isTrue();
        }

        @Test
        @DisplayName("실패: 이메일 중복 시 409 응답 및 저장 안 됨")
        @Transactional
        void fail_duplicateEmail_returns409AndNotSaved() throws Exception {
            // Given - 기존 회원 저장
            Applicant existingApplicant = Applicant.createNewApplicant(
                    "duplicate@example.com",
                    "기존회원",
                    passwordEncoder.encode("password123")
            );
            applicantRepository.save(existingApplicant);

            ApplicantDto.JoinRequest request = new ApplicantDto.JoinRequest(
                    "duplicate@example.com",
                    "newpassword123",
                    "신규회원"
            );

            // When
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("A002"))
                    .andExpect(jsonPath("$.error.message").value("이미 존재하는 이메일입니다"));

            // Then - DB에는 기존 회원만 존재
            List<Applicant> applicants = applicantRepository.findAll();
            assertThat(applicants).hasSize(1);
            assertThat(applicants.get(0).getName()).isEqualTo("기존회원");
        }

        @Test
        @DisplayName("실패: Validation 실패 시 400 응답 및 저장 안 됨")
        @Transactional
        void fail_validationError_returns400AndNotSaved() throws Exception {
            // Given
            ApplicantDto.JoinRequest request = new ApplicantDto.JoinRequest(
                    "invalid-email",  // 잘못된 이메일 형식
                    "password123",
                    "홍길동"
            );

            // When
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("C001"));

            // Then - DB에 저장 안 됨
            List<Applicant> applicants = applicantRepository.findAll();
            assertThat(applicants).isEmpty();
        }

        @Test
        @DisplayName("성공: 여러 회원 순차 가입 시 모두 저장됨")
        @Transactional
        void success_multipleApplicants_allSaved() throws Exception {
            // Given
            ApplicantDto.JoinRequest request1 = new ApplicantDto.JoinRequest(
                    "user1@example.com",
                    "password123",
                    "홍길동"
            );
            ApplicantDto.JoinRequest request2 = new ApplicantDto.JoinRequest(
                    "user2@example.com",
                    "password456",
                    "김철수"
            );

            // When
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());

            // Then
            List<Applicant> applicants = applicantRepository.findAll();
            assertThat(applicants).hasSize(2);
            assertThat(applicants)
                    .extracting(Applicant::getEmail)
                    .containsExactlyInAnyOrder("user1@example.com", "user2@example.com");
        }
    }
}
