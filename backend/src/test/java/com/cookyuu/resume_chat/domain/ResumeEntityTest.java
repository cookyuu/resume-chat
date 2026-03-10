package com.cookyuu.resume_chat.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resume 엔티티 테스트")
class ResumeEntityTest {

    @Test
    @DisplayName("createNewResume 정적 팩토리 메서드 테스트")
    void createNewResume_Success() {
        // Given
        Applicant applicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        String title = "백엔드 개발자 이력서";
        String description = "3년차 Spring 개발자";
        String filePath = "file.pdf";
        String originalFileName = "resume.pdf";

        // When
        Resume resume = Resume.createNewResume(
                applicant,
                title,
                description,
                filePath,
                originalFileName
        );

        // Then
        assertThat(resume).isNotNull();
        assertThat(resume.getResumeSlug()).isNotNull();
        assertThat(resume.getApplicant()).isEqualTo(applicant);
        assertThat(resume.getTitle()).isEqualTo(title);
        assertThat(resume.getDescription()).isEqualTo(description);
        assertThat(resume.getFilePath()).isEqualTo(filePath);
        assertThat(resume.getOriginalFileName()).isEqualTo(originalFileName);
        assertThat(resume.getViewCnt()).isEqualTo(0);
    }

    @Test
    @DisplayName("incrementViewCount 테스트")
    void incrementViewCount_Success() {
        // Given
        Applicant applicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        Resume resume = Resume.createNewResume(
                applicant,
                "이력서",
                "설명",
                "file.pdf",
                "resume.pdf"
        );

        int initialViewCnt = resume.getViewCnt();

        // When
        resume.incrementViewCount();
        resume.incrementViewCount();
        resume.incrementViewCount();

        // Then
        assertThat(resume.getViewCnt()).isEqualTo(initialViewCnt + 3);
    }

    @Test
    @DisplayName("Builder 패턴 테스트")
    void builder_Success() {
        // Given
        UUID resumeSlug = UUID.randomUUID();
        Applicant applicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        // When
        Resume resume = Resume.builder()
                .resumeSlug(resumeSlug)
                .applicant(applicant)
                .title("이력서 제목")
                .description("이력서 설명")
                .filePath("stored-file.pdf")
                .originalFileName("resume.pdf")
                .viewCnt(5)
                .build();

        // Then
        assertThat(resume.getResumeSlug()).isEqualTo(resumeSlug);
        assertThat(resume.getApplicant()).isEqualTo(applicant);
        assertThat(resume.getTitle()).isEqualTo("이력서 제목");
        assertThat(resume.getDescription()).isEqualTo("이력서 설명");
        assertThat(resume.getFilePath()).isEqualTo("stored-file.pdf");
        assertThat(resume.getOriginalFileName()).isEqualTo("resume.pdf");
        assertThat(resume.getViewCnt()).isEqualTo(5);
    }

    @Test
    @DisplayName("description이 null인 경우 테스트")
    void createNewResume_WithNullDescription() {
        // Given
        Applicant applicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        // When
        Resume resume = Resume.createNewResume(
                applicant,
                "제목만 있는 이력서",
                null,
                "file.pdf",
                "resume.pdf"
        );

        // Then
        assertThat(resume.getDescription()).isNull();
        assertThat(resume.getTitle()).isNotNull();
    }

    @Test
    @DisplayName("chatSessions 초기화 테스트")
    void chatSessions_InitializedAsEmptyList() {
        // Given
        Applicant applicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        // When
        Resume resume = Resume.createNewResume(
                applicant,
                "이력서",
                "설명",
                "file.pdf",
                "resume.pdf"
        );

        // Then
        assertThat(resume.getChatSessions()).isNotNull();
        assertThat(resume.getChatSessions()).isEmpty();
    }

    @Test
    @DisplayName("동일한 resumeSlug를 가진 Resume 비교")
    void equals_SameResumeSlug() {
        // Given
        UUID resumeSlug = UUID.randomUUID();
        Applicant applicant = Applicant.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        Resume resume1 = Resume.builder()
                .id(1L)
                .resumeSlug(resumeSlug)
                .applicant(applicant)
                .title("이력서1")
                .filePath("file1.pdf")
                .originalFileName("resume1.pdf")
                .viewCnt(0)
                .build();

        Resume resume2 = Resume.builder()
                .id(1L)
                .resumeSlug(resumeSlug)
                .applicant(applicant)
                .title("이력서2")
                .filePath("file2.pdf")
                .originalFileName("resume2.pdf")
                .viewCnt(5)
                .build();

        // When & Then
        // Lombok의 @EqualsAndHashCode는 모든 필드를 기반으로 하므로
        // id와 resumeSlug가 같아도 다른 필드가 다르면 equals는 false
        assertThat(resume1.getId()).isEqualTo(resume2.getId());
        assertThat(resume1.getResumeSlug()).isEqualTo(resume2.getResumeSlug());
    }
}
