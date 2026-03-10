package com.cookyuu.resume_chat.integration;

import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.repository.ChatSessionRepository;
import com.cookyuu.resume_chat.repository.ResumeRepository;
import com.cookyuu.resume_chat.security.JwtTokenProvider;
import com.cookyuu.resume_chat.service.ApplicantService;
import com.cookyuu.resume_chat.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Resume 통합 테스트")
class ResumeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicantService applicantService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;
    private UUID applicantUuid;

    @BeforeEach
    void setUp() {
        // 지원자 생성 및 로그인
        Applicant applicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .name("홍길동")
                .password(passwordEncoder.encode("password123!"))
                .build();

        applicantRepository.save(applicant);
        applicantUuid = applicant.getUuid();

        // JWT 토큰 생성
        accessToken = jwtTokenProvider.generateAccessToken(applicant.getUuid(), applicant.getEmail());
    }

    @Test
    @DisplayName("E2E: 이력서 업로드 → 조회 → 삭제")
    void fullFlow_Upload_Get_Delete() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        String title = "백엔드 개발자 이력서";
        String description = "3년차 Spring 개발자";

        // When & Then: 이력서 업로드
        String response = mockMvc.perform(multipart("/api/applicant/resume")
                        .file(file)
                        .param("title", title)
                        .param("description", description)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.description").value(description))
                .andExpect(jsonPath("$.data.resumeSlug").exists())
                .andExpect(jsonPath("$.data.chatLink").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String resumeSlug = objectMapper.readTree(response)
                .get("data")
                .get("resumeSlug")
                .asText();

        // When & Then: 이력서 목록 조회
        mockMvc.perform(get("/api/applicant/resume")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value(title))
                .andExpect(jsonPath("$.data[0].chatLink").exists());

        // When & Then: 이력서 삭제
        mockMvc.perform(delete("/api/applicant/resume/{resumeSlug}", resumeSlug)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // When & Then: 삭제 후 목록 조회 (빈 목록)
        mockMvc.perform(get("/api/applicant/resume")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("E2E: 이력서 업로드 후 DB에서 검증")
    void uploadResume_VerifyInDatabase() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        // When
        String response = mockMvc.perform(multipart("/api/applicant/resume")
                        .file(file)
                        .param("title", "이력서 제목")
                        .param("description", "이력서 설명")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String resumeSlugStr = objectMapper.readTree(response)
                .get("data")
                .get("resumeSlug")
                .asText();

        UUID resumeSlug = UUID.fromString(resumeSlugStr);

        // Then
        Resume resume = resumeRepository.findByResumeSlug(resumeSlug).orElseThrow();
        assertThat(resume.getTitle()).isEqualTo("이력서 제목");
        assertThat(resume.getDescription()).isEqualTo("이력서 설명");
        assertThat(resume.getOriginalFileName()).isEqualTo("resume.pdf");
        assertThat(resume.getFilePath()).endsWith(".pdf");
        assertThat(resume.getApplicant().getUuid()).isEqualTo(applicantUuid);
    }

    @Test
    @DisplayName("E2E: 이력서 삭제 시 ChatSession도 함께 삭제 (Cascade)")
    void deleteResume_Cascades_To_ChatSessions() throws Exception {
        // Given: 이력서 업로드
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/applicant/resume")
                        .file(file)
                        .param("title", "이력서")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID resumeSlug = UUID.fromString(
                objectMapper.readTree(response).get("data").get("resumeSlug").asText()
        );

        Resume resume = resumeRepository.findByResumeSlug(resumeSlug).orElseThrow();

        // Given: ChatSession 생성
        ChatSession session1 = ChatSession.createNewSession(
                resume,
                "채용담당자1",
                "recruiter1@company.com",
                "ABC회사"
        );
        ChatSession session2 = ChatSession.createNewSession(
                resume,
                "채용담당자2",
                "recruiter2@company.com",
                "XYZ회사"
        );
        chatSessionRepository.saveAll(List.of(session1, session2));

        String sessionToken1 = session1.getSessionToken();
        String sessionToken2 = session2.getSessionToken();

        // When: 이력서 삭제
        mockMvc.perform(delete("/api/applicant/resume/{resumeSlug}", resumeSlug)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Then: ChatSession도 함께 삭제됨
        assertThat(resumeRepository.findByResumeSlug(resumeSlug)).isEmpty();
        assertThat(chatSessionRepository.findBySessionToken(sessionToken1)).isEmpty();
        assertThat(chatSessionRepository.findBySessionToken(sessionToken2)).isEmpty();
    }

    @Test
    @DisplayName("E2E: 여러 이력서 업로드 및 목록 조회")
    void uploadMultipleResumes_And_GetList() throws Exception {
        // Given & When: 3개의 이력서 업로드
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "resume" + i + ".pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    ("PDF content " + i).getBytes()
            );

            mockMvc.perform(multipart("/api/applicant/resume")
                            .file(file)
                            .param("title", "이력서" + i)
                            .param("description", "설명" + i)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isCreated());
        }

        // Then: 목록 조회
        mockMvc.perform(get("/api/applicant/resume")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].chatLink").exists());
    }

    @Test
    @DisplayName("E2E: 다른 사용자의 이력서 삭제 시도 실패")
    void deleteOtherUsersResume_Fail() throws Exception {
        // Given: 첫 번째 사용자의 이력서 업로드
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/applicant/resume")
                        .file(file)
                        .param("title", "첫 번째 사용자 이력서")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID resumeSlug = UUID.fromString(
                objectMapper.readTree(response).get("data").get("resumeSlug").asText()
        );

        // Given: 두 번째 사용자 생성 및 로그인
        Applicant otherApplicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("other@example.com")
                .name("김철수")
                .password(passwordEncoder.encode("password123!"))
                .build();
        applicantRepository.save(otherApplicant);

        String otherAccessToken = jwtTokenProvider.generateAccessToken(otherApplicant.getUuid(), otherApplicant.getEmail());

        // When & Then: 다른 사용자가 삭제 시도
        mockMvc.perform(delete("/api/applicant/resume/{resumeSlug}", resumeSlug)
                        .header("Authorization", "Bearer " + otherAccessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // 이력서가 여전히 존재하는지 확인
        assertThat(resumeRepository.findByResumeSlug(resumeSlug)).isPresent();
    }

    @Test
    @DisplayName("E2E: 인증 없이 이력서 업로드 시도 실패")
    void uploadResume_WithoutAuth_Fail() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/applicant/resume")
                        .file(file)
                        .param("title", "이력서"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
