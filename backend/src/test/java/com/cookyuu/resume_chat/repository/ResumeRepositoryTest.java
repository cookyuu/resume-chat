package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ResumeRepository 테스트")
class ResumeRepositoryTest {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Resume 저장 및 조회")
    void save_And_FindById() {
        // Given
        Applicant applicant = createApplicant("test@example.com", "홍길동");
        applicantRepository.save(applicant);

        Resume resume = Resume.createNewResume(
                applicant,
                "백엔드 개발자 이력서",
                "3년차 Spring 개발자",
                "file.pdf",
                "resume.pdf"
        );

        // When
        Resume savedResume = resumeRepository.save(resume);
        entityManager.flush();
        entityManager.clear();

        Optional<Resume> foundResume = resumeRepository.findById(savedResume.getId());

        // Then
        assertThat(foundResume).isPresent();
        assertThat(foundResume.get().getTitle()).isEqualTo("백엔드 개발자 이력서");
        assertThat(foundResume.get().getResumeSlug()).isNotNull();
    }

    @Test
    @DisplayName("resumeSlug로 Resume 조회")
    void findByResumeSlug_Success() {
        // Given
        Applicant applicant = createApplicant("test@example.com", "홍길동");
        applicantRepository.save(applicant);

        Resume resume = Resume.createNewResume(
                applicant,
                "이력서",
                "설명",
                "file.pdf",
                "resume.pdf"
        );
        resumeRepository.save(resume);
        entityManager.flush();
        entityManager.clear();

        UUID resumeSlug = resume.getResumeSlug();

        // When
        Optional<Resume> foundResume = resumeRepository.findByResumeSlug(resumeSlug);

        // Then
        assertThat(foundResume).isPresent();
        assertThat(foundResume.get().getResumeSlug()).isEqualTo(resumeSlug);
        assertThat(foundResume.get().getTitle()).isEqualTo("이력서");
    }

    @Test
    @DisplayName("resumeSlug로 Resume 조회 - 존재하지 않음")
    void findByResumeSlug_NotFound() {
        // Given
        UUID nonExistentSlug = UUID.randomUUID();

        // When
        Optional<Resume> foundResume = resumeRepository.findByResumeSlug(nonExistentSlug);

        // Then
        assertThat(foundResume).isEmpty();
    }

    @Test
    @DisplayName("Applicant로 Resume 목록 조회")
    void findByApplicant_Success() {
        // Given
        Applicant applicant = createApplicant("test@example.com", "홍길동");
        applicantRepository.save(applicant);

        Resume resume1 = Resume.createNewResume(applicant, "이력서1", "설명1", "file1.pdf", "resume1.pdf");
        Resume resume2 = Resume.createNewResume(applicant, "이력서2", "설명2", "file2.pdf", "resume2.pdf");
        Resume resume3 = Resume.createNewResume(applicant, "이력서3", "설명3", "file3.pdf", "resume3.pdf");

        resumeRepository.saveAll(List.of(resume1, resume2, resume3));
        entityManager.flush();
        entityManager.clear();

        // When
        List<Resume> resumes = resumeRepository.findByApplicant(applicant);

        // Then
        assertThat(resumes).hasSize(3);
        assertThat(resumes).extracting("title")
                .containsExactlyInAnyOrder("이력서1", "이력서2", "이력서3");
    }

    @Test
    @DisplayName("Applicant로 Resume 목록 조회 - 빈 목록")
    void findByApplicant_EmptyList() {
        // Given
        Applicant applicant = createApplicant("test@example.com", "홍길동");
        applicantRepository.save(applicant);

        // When
        List<Resume> resumes = resumeRepository.findByApplicant(applicant);

        // Then
        assertThat(resumes).isEmpty();
    }

    @Test
    @DisplayName("Resume 삭제 시 ChatSession과 ChatMessage도 함께 삭제 (Cascade)")
    void delete_Resume_Cascades_To_ChatSessions_And_Messages() {
        // Given
        Applicant applicant = createApplicant("test@example.com", "홍길동");
        applicantRepository.save(applicant);

        Resume resume = Resume.createNewResume(
                applicant,
                "이력서",
                "설명",
                "file.pdf",
                "resume.pdf"
        );
        resumeRepository.save(resume);

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

        ChatMessage message1 = ChatMessage.createMessage(
                session1,
                com.cookyuu.resume_chat.common.enums.SenderType.RECRUITER,
                "안녕하세요"
        );
        ChatMessage message2 = ChatMessage.createMessage(
                session1,
                com.cookyuu.resume_chat.common.enums.SenderType.RECRUITER,
                "면접 제안드립니다"
        );
        ChatMessage message3 = ChatMessage.createMessage(
                session2,
                com.cookyuu.resume_chat.common.enums.SenderType.RECRUITER,
                "포트폴리오 확인했습니다"
        );
        chatMessageRepository.saveAll(List.of(message1, message2, message3));

        entityManager.flush();
        entityManager.clear();

        Long resumeId = resume.getId();
        String sessionToken1 = session1.getSessionToken();
        String sessionToken2 = session2.getSessionToken();

        // When
        resumeRepository.deleteById(resumeId);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(resumeRepository.findById(resumeId)).isEmpty();
        assertThat(chatSessionRepository.findBySessionToken(sessionToken1)).isEmpty();
        assertThat(chatSessionRepository.findBySessionToken(sessionToken2)).isEmpty();

        // ChatMessage도 함께 삭제되었는지 확인
        List<ChatMessage> remainingMessages = chatMessageRepository.findAll();
        assertThat(remainingMessages).isEmpty();
    }

    @Test
    @DisplayName("다른 Applicant의 Resume는 조회되지 않음")
    void findByApplicant_OnlyOwnResumes() {
        // Given
        Applicant applicant1 = createApplicant("test1@example.com", "홍길동");
        Applicant applicant2 = createApplicant("test2@example.com", "김철수");
        applicantRepository.saveAll(List.of(applicant1, applicant2));

        Resume resume1 = Resume.createNewResume(applicant1, "홍길동 이력서", "설명", "file1.pdf", "resume1.pdf");
        Resume resume2 = Resume.createNewResume(applicant2, "김철수 이력서", "설명", "file2.pdf", "resume2.pdf");
        resumeRepository.saveAll(List.of(resume1, resume2));

        entityManager.flush();
        entityManager.clear();

        // When
        List<Resume> applicant1Resumes = resumeRepository.findByApplicant(applicant1);
        List<Resume> applicant2Resumes = resumeRepository.findByApplicant(applicant2);

        // Then
        assertThat(applicant1Resumes).hasSize(1);
        assertThat(applicant1Resumes.get(0).getTitle()).isEqualTo("홍길동 이력서");

        assertThat(applicant2Resumes).hasSize(1);
        assertThat(applicant2Resumes.get(0).getTitle()).isEqualTo("김철수 이력서");
    }

    @Test
    @DisplayName("Resume의 viewCnt 증가 및 저장")
    void incrementViewCount_And_Save() {
        // Given
        Applicant applicant = createApplicant("test@example.com", "홍길동");
        applicantRepository.save(applicant);

        Resume resume = Resume.createNewResume(
                applicant,
                "이력서",
                "설명",
                "file.pdf",
                "resume.pdf"
        );
        resumeRepository.save(resume);
        entityManager.flush();
        entityManager.clear();

        // When
        Resume foundResume = resumeRepository.findById(resume.getId()).orElseThrow();
        foundResume.incrementViewCount();
        foundResume.incrementViewCount();
        resumeRepository.save(foundResume);
        entityManager.flush();
        entityManager.clear();

        // Then
        Resume updatedResume = resumeRepository.findById(resume.getId()).orElseThrow();
        assertThat(updatedResume.getViewCnt()).isEqualTo(2);
    }

    private Applicant createApplicant(String email, String name) {
        return Applicant.builder()
                .uuid(UUID.randomUUID())
                .email(email)
                .name(name)
                .password("password")
                .build();
    }
}
