package com.cookyuu.resume_chat.integration;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.dto.ChatDto;
import com.cookyuu.resume_chat.entity.Applicant;
import com.cookyuu.resume_chat.entity.ChatMessage;
import com.cookyuu.resume_chat.entity.ChatSession;
import com.cookyuu.resume_chat.entity.Resume;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.repository.ChatMessageRepository;
import com.cookyuu.resume_chat.repository.ChatSessionRepository;
import com.cookyuu.resume_chat.repository.ResumeRepository;
import com.cookyuu.resume_chat.security.JwtTokenProvider;
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
@DisplayName("Chat 통합 테스트")
class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void tearDown() {
        chatMessageRepository.deleteAll();
        chatSessionRepository.deleteAll();
        resumeRepository.deleteAll();
        applicantRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/applicant/chat/{sessionToken}/send - 지원자 메시지 전송 E2E")
    class ApplicantSendMessageE2E {

        @Test
        @DisplayName("성공: 지원자가 채팅 세션에 메시지 전송")
        @Transactional
        void success_applicantSendsMessage() throws Exception {
            // Given - 지원자 생성
            Applicant applicant = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

            // 이력서 생성
            Resume resume = Resume.createNewResume(
                    applicant,
                    "백엔드 개발자 이력서",
                    "백엔드 개발 경력 5년",
                    "/uploads/resume.pdf",
                    "resume.pdf"
            );
            resumeRepository.save(resume);

            // 채팅 세션 생성 (채용담당자가 먼저 메시지를 보낸 상황)
            ChatSession session = ChatSession.createNewSession(
                    resume,
                    "김채용",
                    "recruiter@company.com",
                    "ABC회사"
            );
            chatSessionRepository.save(session);

            // 채용담당자의 첫 메시지
            ChatMessage recruiterMessage = ChatMessage.createMessage(
                    session,
                    SenderType.RECRUITER,
                    "안녕하세요, 이력서 잘 봤습니다."
            );
            chatMessageRepository.save(recruiterMessage);
            session.incrementMessageCount();

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(applicant.getUuid(), applicant.getEmail());

            // 지원자 메시지 요청
            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(
                    "안녕하세요, 연락 주셔서 감사합니다."
            );

            // When
            mockMvc.perform(post("/api/applicant/chat/{sessionToken}/send", session.getSessionToken())
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionToken").value(session.getSessionToken()))
                    .andExpect(jsonPath("$.data.messageId").exists())
                    .andExpect(jsonPath("$.data.message").value("안녕하세요, 연락 주셔서 감사합니다."))
                    .andExpect(jsonPath("$.data.sentAt").exists());

            // Then - DB 검증
            List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
            assertThat(messages).hasSize(2);

            ChatMessage applicantMessage = messages.get(1);
            assertThat(applicantMessage.getSenderType()).isEqualTo(SenderType.APPLICANT);
            assertThat(applicantMessage.getContent()).isEqualTo("안녕하세요, 연락 주셔서 감사합니다.");
            assertThat(applicantMessage.isReadStatus()).isFalse();
            assertThat(applicantMessage.getSession().getId()).isEqualTo(session.getId());

            // 세션의 메시지 카운트 증가 확인
            ChatSession updatedSession = chatSessionRepository.findById(session.getId()).get();
            assertThat(updatedSession.getTotalMessages()).isEqualTo(2);
            assertThat(updatedSession.getLastMessageAt()).isNotNull();
        }

        @Test
        @DisplayName("실패: 인증 없이 메시지 전송 시 401 Unauthorized")
        @Transactional
        void fail_noAuthentication_returns401() throws Exception {
            // Given
            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(
                    "테스트 메시지"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/chat/{sessionToken}/send", "test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 세션 토큰으로 메시지 전송 시 404 Not Found")
        @Transactional
        void fail_sessionNotFound_returns404() throws Exception {
            // Given - 지원자 생성
            Applicant applicant = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

            String accessToken = jwtTokenProvider.generateAccessToken(applicant.getUuid(), applicant.getEmail());

            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(
                    "테스트 메시지"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/chat/{sessionToken}/send", "non-existent-token")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패: 다른 지원자의 채팅 세션에 메시지 전송 시 403 Forbidden")
        @Transactional
        void fail_unauthorizedAccess_returns403() throws Exception {
            // Given - 지원자1 생성
            Applicant applicant1 = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant1@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

            // 지원자1의 이력서 및 세션 생성
            Resume resume1 = Resume.createNewResume(
                    applicant1,
                    "백엔드 개발자 이력서",
                    "백엔드 개발 경력 5년",
                    "/uploads/resume.pdf",
                    "resume.pdf"
            );
            resumeRepository.save(resume1);

            ChatSession session1 = ChatSession.createNewSession(
                    resume1,
                    "김채용",
                    "recruiter@company.com",
                    "ABC회사"
            );
            chatSessionRepository.save(session1);

            // 지원자2 생성
            Applicant applicant2 = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant2@example.com",
                    "김철수",
                    passwordEncoder.encode("password123")
            ));

            // 지원자2의 토큰으로 지원자1의 세션에 접근 시도
            String accessToken = jwtTokenProvider.generateAccessToken(applicant2.getUuid(), applicant2.getEmail());

            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(
                    "테스트 메시지"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/chat/{sessionToken}/send", session1.getSessionToken())
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패: 빈 메시지 전송 시 400 Bad Request")
        @Transactional
        void fail_emptyMessage_returns400() throws Exception {
            // Given - 지원자 생성
            Applicant applicant = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

            // 이력서 생성
            Resume resume = Resume.createNewResume(
                    applicant,
                    "백엔드 개발자 이력서",
                    "백엔드 개발 경력 5년",
                    "/uploads/resume.pdf",
                    "resume.pdf"
            );
            resumeRepository.save(resume);

            // 채팅 세션 생성
            ChatSession session = ChatSession.createNewSession(
                    resume,
                    "김채용",
                    "recruiter@company.com",
                    "ABC회사"
            );
            chatSessionRepository.save(session);

            String accessToken = jwtTokenProvider.generateAccessToken(applicant.getUuid(), applicant.getEmail());

            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(
                    ""
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/chat/{sessionToken}/send", session.getSessionToken())
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패: 메시지가 1000자 초과 시 400 Bad Request")
        @Transactional
        void fail_messageTooLong_returns400() throws Exception {
            // Given - 지원자 생성
            Applicant applicant = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

            // 이력서 생성
            Resume resume = Resume.createNewResume(
                    applicant,
                    "백엔드 개발자 이력서",
                    "백엔드 개발 경력 5년",
                    "/uploads/resume.pdf",
                    "resume.pdf"
            );
            resumeRepository.save(resume);

            // 채팅 세션 생성
            ChatSession session = ChatSession.createNewSession(
                    resume,
                    "김채용",
                    "recruiter@company.com",
                    "ABC회사"
            );
            chatSessionRepository.save(session);

            String accessToken = jwtTokenProvider.generateAccessToken(applicant.getUuid(), applicant.getEmail());

            // 1001자 메시지 생성
            String longMessage = "a".repeat(1001);
            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(
                    longMessage
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/chat/{sessionToken}/send", session.getSessionToken())
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
