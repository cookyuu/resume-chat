package com.cookyuu.resume_chat.integration;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.dto.ChatDto;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        @DisplayName("실패: 인증 없이 메시지 전송 시 403 Forbidden")
        @Transactional
        void fail_noAuthentication_returns403() throws Exception {
            // Given
            ChatDto.ApplicantSendMessageRequest request = new ChatDto.ApplicantSendMessageRequest(
                    "테스트 메시지"
            );

            // When & Then - Spring Security는 인증 없이 접근 시 403 Forbidden 반환
            mockMvc.perform(post("/api/applicant/chat/{sessionToken}/send", "test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
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

    @Nested
    @DisplayName("POST /api/chat/{resumeSlug}/enter - 채용담당자 세션 진입 E2E")
    class RecruiterEnterSessionE2E {

        @Test
        @DisplayName("성공: 새 세션 생성")
        @Transactional
        void success_createNewSession() throws Exception {
            // Given - 지원자 및 이력서 생성
            Applicant applicant = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

            Resume resume = Resume.createNewResume(
                    applicant,
                    "백엔드 개발자 이력서",
                    "백엔드 개발 경력 5년",
                    "/uploads/resume.pdf",
                    "resume.pdf"
            );
            resumeRepository.save(resume);

            ChatDto.EnterSessionRequest request = new ChatDto.EnterSessionRequest(
                    "recruiter@company.com",
                    "김채용",
                    "ABC회사"
            );

            // When
            mockMvc.perform(post("/api/chat/{resumeSlug}/enter", resume.getResumeSlug())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionToken").exists())
                    .andExpect(jsonPath("$.data.resumeSlug").value(resume.getResumeSlug().toString()))
                    .andExpect(jsonPath("$.data.resumeTitle").value("백엔드 개발자 이력서"))
                    .andExpect(jsonPath("$.data.recruiterEmail").value("recruiter@company.com"))
                    .andExpect(jsonPath("$.data.recruiterName").value("김채용"))
                    .andExpect(jsonPath("$.data.recruiterCompany").value("ABC회사"))
                    .andExpect(jsonPath("$.data.totalMessages").value(0))
                    .andExpect(jsonPath("$.data.createdAt").exists());

            // Then - DB 검증
            List<ChatSession> sessions = chatSessionRepository.findByResume(resume);
            assertThat(sessions).hasSize(1);
            assertThat(sessions.get(0).getRecruiterEmail()).isEqualTo("recruiter@company.com");
        }

        @Test
        @DisplayName("성공: 기존 세션 반환")
        @Transactional
        void success_returnExistingSession() throws Exception {
            // Given - 지원자 및 이력서 생성
            Applicant applicant = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

            Resume resume = Resume.createNewResume(
                    applicant,
                    "백엔드 개발자 이력서",
                    "백엔드 개발 경력 5년",
                    "/uploads/resume.pdf",
                    "resume.pdf"
            );
            resumeRepository.save(resume);

            // 기존 세션 생성
            ChatSession existingSession = ChatSession.createNewSession(
                    resume,
                    "김채용",
                    "recruiter@company.com",
                    "ABC회사"
            );
            chatSessionRepository.save(existingSession);

            ChatDto.EnterSessionRequest request = new ChatDto.EnterSessionRequest(
                    "recruiter@company.com",
                    "김채용",
                    "ABC회사"
            );

            // When
            mockMvc.perform(post("/api/chat/{resumeSlug}/enter", resume.getResumeSlug())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionToken").value(existingSession.getSessionToken()));

            // Then - DB 검증 (새 세션이 생성되지 않았는지 확인)
            List<ChatSession> sessions = chatSessionRepository.findByResume(resume);
            assertThat(sessions).hasSize(1);
        }

        @Test
        @DisplayName("실패: 이력서를 찾을 수 없음")
        @Transactional
        void fail_resumeNotFound_returns404() throws Exception {
            // Given
            ChatDto.EnterSessionRequest request = new ChatDto.EnterSessionRequest(
                    "recruiter@company.com",
                    "김채용",
                    "ABC회사"
            );

            // When & Then
            mockMvc.perform(post("/api/chat/{resumeSlug}/enter", "00000000-0000-0000-0000-000000000000")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/chat/session/{sessionToken}/messages - 채용담당자 메시지 조회 E2E")
    class RecruiterGetMessagesE2E {

        @Test
        @DisplayName("성공: 세션의 메시지 목록 조회")
        @Transactional
        void success_getMessages() throws Exception {
            // Given - 지원자 및 이력서 생성
            Applicant applicant = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

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

            // 메시지 생성
            ChatMessage message1 = ChatMessage.createMessage(session, SenderType.RECRUITER, "안녕하세요");
            ChatMessage message2 = ChatMessage.createMessage(session, SenderType.APPLICANT, "네 안녕하세요");
            chatMessageRepository.save(message1);
            chatMessageRepository.save(message2);
            session.incrementMessageCount();
            session.incrementMessageCount();

            // When
            mockMvc.perform(get("/api/chat/session/{sessionToken}/messages", session.getSessionToken()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.session.sessionToken").value(session.getSessionToken()))
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.messages.length()").value(2))
                    .andExpect(jsonPath("$.data.messages[0].message").value("안녕하세요"))
                    .andExpect(jsonPath("$.data.messages[0].senderType").value("RECRUITER"))
                    .andExpect(jsonPath("$.data.messages[1].message").value("네 안녕하세요"))
                    .andExpect(jsonPath("$.data.messages[1].senderType").value("APPLICANT"));
        }

        @Test
        @DisplayName("실패: 세션을 찾을 수 없음")
        @Transactional
        void fail_sessionNotFound_returns404() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/chat/session/{sessionToken}/messages", "non-existent-token"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/chat/session/{sessionToken}/send - 채용담당자 메시지 전송 E2E")
    class RecruiterSendMessageE2E {

        @Test
        @DisplayName("성공: 채용담당자가 메시지 전송")
        @Transactional
        void success_recruiterSendsMessage() throws Exception {
            // Given - 지원자 및 이력서 생성
            Applicant applicant = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

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

            ChatDto.RecruiterSendMessageRequest request = new ChatDto.RecruiterSendMessageRequest(
                    "면접 일정 조율 가능한가요?"
            );

            // When
            mockMvc.perform(post("/api/chat/session/{sessionToken}/send", session.getSessionToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionToken").value(session.getSessionToken()))
                    .andExpect(jsonPath("$.data.messageId").exists())
                    .andExpect(jsonPath("$.data.message").value("면접 일정 조율 가능한가요?"))
                    .andExpect(jsonPath("$.data.sentAt").exists());

            // Then - DB 검증
            List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
            assertThat(messages).hasSize(1);
            assertThat(messages.get(0).getSenderType()).isEqualTo(SenderType.RECRUITER);
            assertThat(messages.get(0).getContent()).isEqualTo("면접 일정 조율 가능한가요?");

            // 세션 메시지 카운트 증가 확인
            ChatSession updatedSession = chatSessionRepository.findById(session.getId()).get();
            assertThat(updatedSession.getTotalMessages()).isEqualTo(1);
        }

        @Test
        @DisplayName("실패: 세션을 찾을 수 없음")
        @Transactional
        void fail_sessionNotFound_returns404() throws Exception {
            // Given
            ChatDto.RecruiterSendMessageRequest request = new ChatDto.RecruiterSendMessageRequest(
                    "테스트 메시지"
            );

            // When & Then
            mockMvc.perform(post("/api/chat/session/{sessionToken}/send", "non-existent-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패: 빈 메시지 전송 시 400 Bad Request")
        @Transactional
        void fail_emptyMessage_returns400() throws Exception {
            // Given - 지원자 및 이력서 생성
            Applicant applicant = applicantRepository.save(Applicant.createNewApplicant(
                    "applicant@example.com",
                    "홍길동",
                    passwordEncoder.encode("password123")
            ));

            Resume resume = Resume.createNewResume(
                    applicant,
                    "백엔드 개발자 이력서",
                    "백엔드 개발 경력 5년",
                    "/uploads/resume.pdf",
                    "resume.pdf"
            );
            resumeRepository.save(resume);

            ChatSession session = ChatSession.createNewSession(
                    resume,
                    "김채용",
                    "recruiter@company.com",
                    "ABC회사"
            );
            chatSessionRepository.save(session);

            ChatDto.RecruiterSendMessageRequest request = new ChatDto.RecruiterSendMessageRequest("");

            // When & Then
            mockMvc.perform(post("/api/chat/session/{sessionToken}/send", session.getSessionToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
