package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.config.AppProperties;
import com.cookyuu.resume_chat.dto.ResumeDto;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.Resume;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.repository.ResumeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeService 테스트")
class ResumeServiceTest {

    @InjectMocks
    private ResumeService resumeService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AppProperties appProperties;

    @Test
    @DisplayName("이력서 업로드 성공")
    void uploadResume_Success() {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        String title = "백엔드 개발자 이력서";
        String description = "3년차 Spring 개발자";
        String frontendUrl = "http://localhost:3000";

        Applicant applicant = Applicant.builder()
                .uuid(applicantUuid)
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        MultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        String storedFileName = UUID.randomUUID() + ".pdf";

        Resume savedResume = Resume.builder()
                .id(1L)
                .resumeSlug(UUID.randomUUID())
                .applicant(applicant)
                .title(title)
                .description(description)
                .filePath(storedFileName)
                .originalFileName("resume.pdf")
                .viewCnt(0)
                .build();

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.of(applicant));
        given(fileStorageService.storeFile(file)).willReturn(storedFileName);
        given(resumeRepository.save(any(Resume.class))).willReturn(savedResume);
        given(appProperties.getFrontendUrl()).willReturn(frontendUrl);

        // When
        ResumeDto.UploadResponse response = resumeService.uploadResume(
                applicantUuid, title, description, file
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo(title);
        assertThat(response.getDescription()).isEqualTo(description);
        assertThat(response.getOriginalFileName()).isEqualTo("resume.pdf");
        assertThat(response.getChatLink()).startsWith(frontendUrl + "/chat/");

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(fileStorageService, times(1)).storeFile(file);
        verify(resumeRepository, times(1)).save(any(Resume.class));

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());

        Resume capturedResume = resumeCaptor.getValue();
        assertThat(capturedResume.getTitle()).isEqualTo(title);
        assertThat(capturedResume.getDescription()).isEqualTo(description);
        assertThat(capturedResume.getFilePath()).isEqualTo(storedFileName);
        assertThat(capturedResume.getApplicant()).isEqualTo(applicant);
    }

    @Test
    @DisplayName("이력서 업로드 실패 - 지원자 없음")
    void uploadResume_Fail_ApplicantNotFound() {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> resumeService.uploadResume(
                applicantUuid, "제목", "설명", file
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.APPLICANT_NOT_FOUND.getMessage());

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(fileStorageService, never()).storeFile(any());
        verify(resumeRepository, never()).save(any());
    }

    @Test
    @DisplayName("지원자의 이력서 목록 조회 성공")
    void getApplicantResumes_Success() {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        String frontendUrl = "http://localhost:3000";

        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(applicantUuid)
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        Resume resume1 = Resume.builder()
                .resumeSlug(UUID.randomUUID())
                .applicant(applicant)
                .title("이력서1")
                .description("설명1")
                .filePath("file1.pdf")
                .originalFileName("resume1.pdf")
                .viewCnt(0)
                .build();

        Resume resume2 = Resume.builder()
                .resumeSlug(UUID.randomUUID())
                .applicant(applicant)
                .title("이력서2")
                .description("설명2")
                .filePath("file2.pdf")
                .originalFileName("resume2.pdf")
                .viewCnt(5)
                .build();

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.of(applicant));
        given(resumeRepository.findByApplicant(applicant)).willReturn(List.of(resume1, resume2));
        given(appProperties.getFrontendUrl()).willReturn(frontendUrl);

        // When
        List<ResumeDto.ResumeInfo> result = resumeService.getApplicantResumes(applicantUuid);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("이력서1");
        assertThat(result.get(0).getChatLink()).startsWith(frontendUrl + "/chat/");
        assertThat(result.get(1).getViewCnt()).isEqualTo(5);

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(resumeRepository, times(1)).findByApplicant(applicant);
    }

    @Test
    @DisplayName("이력서 삭제 성공")
    void deleteResume_Success() {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        UUID resumeSlug = UUID.randomUUID();

        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(applicantUuid)
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        Resume resume = Resume.builder()
                .id(1L)
                .resumeSlug(resumeSlug)
                .applicant(applicant)
                .title("이력서")
                .filePath("file.pdf")
                .originalFileName("resume.pdf")
                .viewCnt(0)
                .build();

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.of(applicant));
        given(resumeRepository.findByResumeSlug(resumeSlug)).willReturn(Optional.of(resume));

        // When
        resumeService.deleteResume(applicantUuid, resumeSlug);

        // Then
        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(resumeRepository, times(1)).findByResumeSlug(resumeSlug);
        verify(resumeRepository, times(1)).delete(resume);
    }

    @Test
    @DisplayName("이력서 삭제 실패 - 소유권 검증 실패")
    void deleteResume_Fail_AccessDenied() {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        UUID resumeSlug = UUID.randomUUID();
        UUID otherApplicantId = UUID.randomUUID();

        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(applicantUuid)
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        Applicant otherApplicant = Applicant.builder()
                .id(2L)  // 다른 ID
                .uuid(otherApplicantId)
                .email("other@example.com")
                .name("김철수")
                .password("password")
                .build();

        Resume resume = Resume.builder()
                .id(1L)
                .resumeSlug(resumeSlug)
                .applicant(otherApplicant)  // 다른 사람의 이력서
                .title("이력서")
                .filePath("file.pdf")
                .originalFileName("resume.pdf")
                .viewCnt(0)
                .build();

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.of(applicant));
        given(resumeRepository.findByResumeSlug(resumeSlug)).willReturn(Optional.of(resume));

        // When & Then
        assertThatThrownBy(() -> resumeService.deleteResume(applicantUuid, resumeSlug))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.RESUME_ACCESS_DENIED.getMessage());

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(resumeRepository, times(1)).findByResumeSlug(resumeSlug);
        verify(resumeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("이력서 삭제 실패 - 이력서 없음")
    void deleteResume_Fail_ResumeNotFound() {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        UUID resumeSlug = UUID.randomUUID();

        Applicant applicant = Applicant.builder()
                .id(1L)
                .uuid(applicantUuid)
                .email("test@example.com")
                .name("홍길동")
                .password("password")
                .build();

        given(applicantRepository.findByUuid(applicantUuid)).willReturn(Optional.of(applicant));
        given(resumeRepository.findByResumeSlug(resumeSlug)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> resumeService.deleteResume(applicantUuid, resumeSlug))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.RESUME_NOT_FOUND.getMessage());

        verify(applicantRepository, times(1)).findByUuid(applicantUuid);
        verify(resumeRepository, times(1)).findByResumeSlug(resumeSlug);
        verify(resumeRepository, never()).delete(any());
    }
}
