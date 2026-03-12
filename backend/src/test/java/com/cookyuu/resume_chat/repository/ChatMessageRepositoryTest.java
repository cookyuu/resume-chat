package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.common.enums.ApplicantStatus;
import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.config.TestJpaConfig;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaConfig.class)
@DisplayName("ChatMessageRepository 테스트")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ChatSession testSession;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        Applicant applicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .password("password")
                .name("홍길동")
                .status(ApplicantStatus.ACTIVE)
                .loginFailCnt(0)
                .build();
        applicantRepository.save(applicant);

        Resume resume = Resume.createNewResume(
                applicant,
                "백엔드 개발자 이력서",
                "3년차 Spring 개발자",
                "file.pdf",
                "resume.pdf"
        );
        resumeRepository.save(resume);

        testSession = ChatSession.createNewSession(
                resume,
                "김채용",
                "recruiter@company.com",
                "ABC회사"
        );
        chatSessionRepository.save(testSession);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("페이지네이션 조회 - DESC 정렬 (최신 순)")
    void findBySessionOrderByCreatedAtDesc_Paged_Success() {
        // Given: 메시지 5개 생성
        for (int i = 1; i <= 5; i++) {
            ChatMessage message = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "메시지 " + i);
            chatMessageRepository.save(message);
        }
        entityManager.flush();
        entityManager.clear();

        // 새로운 세션 로드
        ChatSession session = chatSessionRepository.findById(testSession.getId()).orElseThrow();

        // When: 페이지 0, 크기 3으로 조회 (최신 순)
        Pageable pageable = PageRequest.of(0, 3, Sort.by("createdAt").descending());
        Page<ChatMessage> page = chatMessageRepository.findBySessionOrderByCreatedAtDesc(session, pageable);

        // Then
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();

        // 최신 메시지가 먼저 나와야 함
        assertThat(page.getContent().get(0).getContent()).isEqualTo("메시지 5");
        assertThat(page.getContent().get(2).getContent()).isEqualTo("메시지 3");
    }

    @Test
    @DisplayName("페이지네이션 조회 - ASC 정렬 (오래된 순)")
    void findBySessionOrderByCreatedAtAsc_Paged_Success() {
        // Given: 메시지 5개 생성
        for (int i = 1; i <= 5; i++) {
            ChatMessage message = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "메시지 " + i);
            chatMessageRepository.save(message);
        }
        entityManager.flush();
        entityManager.clear();

        // 새로운 세션 로드
        ChatSession session = chatSessionRepository.findById(testSession.getId()).orElseThrow();

        // When: 페이지 0, 크기 3으로 조회 (오래된 순)
        Pageable pageable = PageRequest.of(0, 3, Sort.by("createdAt").ascending());
        Page<ChatMessage> page = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session, pageable);

        // Then
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();

        // 오래된 메시지가 먼저 나와야 함
        assertThat(page.getContent().get(0).getContent()).isEqualTo("메시지 1");
        assertThat(page.getContent().get(2).getContent()).isEqualTo("메시지 3");
    }

    @Test
    @DisplayName("페이지네이션 조회 - 두 번째 페이지")
    void findBySessionOrderByCreatedAtDesc_SecondPage_Success() {
        // Given: 메시지 5개 생성
        for (int i = 1; i <= 5; i++) {
            ChatMessage message = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "메시지 " + i);
            chatMessageRepository.save(message);
        }
        entityManager.flush();
        entityManager.clear();

        // 새로운 세션 로드
        ChatSession session = chatSessionRepository.findById(testSession.getId()).orElseThrow();

        // When: 페이지 1 (두 번째 페이지), 크기 3으로 조회
        Pageable pageable = PageRequest.of(1, 3, Sort.by("createdAt").descending());
        Page<ChatMessage> page = chatMessageRepository.findBySessionOrderByCreatedAtDesc(session, pageable);

        // Then
        assertThat(page.getContent()).hasSize(2); // 마지막 페이지는 2개만
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("페이지네이션 조회 - 빈 결과")
    void findBySessionOrderByCreatedAtDesc_EmptyResult_Success() {
        // Given: 메시지가 없는 상태
        entityManager.flush();
        entityManager.clear();

        // 새로운 세션 로드
        ChatSession session = chatSessionRepository.findById(testSession.getId()).orElseThrow();

        // When
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Page<ChatMessage> page = chatMessageRepository.findBySessionOrderByCreatedAtDesc(session, pageable);

        // Then
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("증분 조회 - timestamp 기반")
    void findBySessionAndCreatedAtAfter_Success() throws InterruptedException {
        // Given: 첫 번째 메시지 생성
        ChatMessage message1 = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "첫 번째 메시지");
        chatMessageRepository.save(message1);
        entityManager.flush();

        LocalDateTime timestamp = LocalDateTime.now();
        Thread.sleep(100); // 시간 차이를 보장하기 위해 대기

        // 두 번째, 세 번째 메시지 생성
        ChatMessage message2 = ChatMessage.createMessage(testSession, SenderType.APPLICANT, "두 번째 메시지");
        ChatMessage message3 = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "세 번째 메시지");
        chatMessageRepository.save(message2);
        chatMessageRepository.save(message3);
        entityManager.flush();
        entityManager.clear();

        // 새로운 세션 로드
        ChatSession session = chatSessionRepository.findById(testSession.getId()).orElseThrow();

        // When: timestamp 이후 메시지 조회
        List<ChatMessage> messages = chatMessageRepository
                .findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(session, timestamp);

        // Then: timestamp 이후 메시지만 조회되어야 함
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getContent()).isEqualTo("두 번째 메시지");
        assertThat(messages.get(1).getContent()).isEqualTo("세 번째 메시지");
    }

    @Test
    @DisplayName("증분 조회 - timestamp 기반, 결과 없음")
    void findBySessionAndCreatedAtAfter_NoResults_Success() {
        // Given: 메시지 생성
        ChatMessage message = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "메시지");
        chatMessageRepository.save(message);
        entityManager.flush();
        entityManager.clear();

        // 새로운 세션 로드
        ChatSession session = chatSessionRepository.findById(testSession.getId()).orElseThrow();

        // When: 미래 시간으로 조회
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1);
        List<ChatMessage> messages = chatMessageRepository
                .findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(session, futureTime);

        // Then
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("증분 조회 - messageId 기반")
    void findBySessionAndIdGreaterThan_Success() {
        // Given: 메시지 3개 생성
        ChatMessage message1 = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "첫 번째 메시지");
        ChatMessage message2 = ChatMessage.createMessage(testSession, SenderType.APPLICANT, "두 번째 메시지");
        ChatMessage message3 = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "세 번째 메시지");

        chatMessageRepository.save(message1);
        chatMessageRepository.save(message2);
        chatMessageRepository.save(message3);
        entityManager.flush();

        Long firstMessageId = message1.getId();
        entityManager.clear();

        // 새로운 세션 로드
        ChatSession session = chatSessionRepository.findById(testSession.getId()).orElseThrow();

        // When: 첫 번째 메시지 ID 이후 조회
        List<ChatMessage> messages = chatMessageRepository
                .findBySessionAndIdGreaterThanOrderByCreatedAtAsc(session, firstMessageId);

        // Then: 두 번째, 세 번째 메시지만 조회되어야 함
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getContent()).isEqualTo("두 번째 메시지");
        assertThat(messages.get(1).getContent()).isEqualTo("세 번째 메시지");
    }

    @Test
    @DisplayName("증분 조회 - messageId 기반, 결과 없음")
    void findBySessionAndIdGreaterThan_NoResults_Success() {
        // Given: 메시지 생성
        ChatMessage message = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "메시지");
        chatMessageRepository.save(message);
        entityManager.flush();

        Long lastMessageId = message.getId();
        entityManager.clear();

        // 새로운 세션 로드
        ChatSession session = chatSessionRepository.findById(testSession.getId()).orElseThrow();

        // When: 마지막 메시지 ID 이후 조회
        List<ChatMessage> messages = chatMessageRepository
                .findBySessionAndIdGreaterThanOrderByCreatedAtAsc(session, lastMessageId);

        // Then
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("다른 세션의 메시지는 조회되지 않음")
    void findBySession_OnlyReturnsMessagesFromGivenSession() {
        // Given: 첫 번째 세션에 메시지 추가
        ChatMessage message1 = ChatMessage.createMessage(testSession, SenderType.RECRUITER, "세션1 메시지");
        chatMessageRepository.save(message1);

        // 두 번째 세션 생성
        Resume resume = resumeRepository.findAll().get(0);
        ChatSession anotherSession = ChatSession.createNewSession(
                resume,
                "박채용",
                "another@company.com",
                "XYZ회사"
        );
        chatSessionRepository.save(anotherSession);

        // 두 번째 세션에 메시지 추가
        ChatMessage message2 = ChatMessage.createMessage(anotherSession, SenderType.RECRUITER, "세션2 메시지");
        chatMessageRepository.save(message2);
        entityManager.flush();
        entityManager.clear();

        // 새로운 세션 로드
        ChatSession session = chatSessionRepository.findById(testSession.getId()).orElseThrow();

        // When: 첫 번째 세션의 메시지만 조회
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").ascending());
        Page<ChatMessage> page = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session, pageable);

        // Then: 첫 번째 세션의 메시지만 조회되어야 함
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("세션1 메시지");
    }
}
