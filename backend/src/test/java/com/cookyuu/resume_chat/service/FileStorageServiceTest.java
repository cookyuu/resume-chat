package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.config.FileProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileStorageService 테스트")
class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private FileProperties fileProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileProperties = new FileProperties();
        fileProperties.setUploadDir(tempDir.toString());
        fileProperties.setMaxFileSize(10485760L); // 10MB
        fileProperties.setAllowedExtensions("pdf");

        fileStorageService = new FileStorageService(fileProperties);
    }

    @Test
    @DisplayName("PDF 파일 저장 성공")
    void storeFile_Success() throws Exception {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        // When
        String storedFileName = fileStorageService.storeFile(file);

        // Then
        assertThat(storedFileName).isNotNull();
        assertThat(storedFileName).endsWith(".pdf");
        assertThat(Files.exists(tempDir.resolve(storedFileName))).isTrue();

        // 저장된 파일 내용 확인
        byte[] savedContent = Files.readAllBytes(tempDir.resolve(storedFileName));
        assertThat(savedContent).isEqualTo("PDF content".getBytes());
    }

    @Test
    @DisplayName("파일 저장 실패 - 빈 파일")
    void storeFile_Fail_EmptyFile() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                new byte[0]
        );

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_FILE.getMessage());
    }

    @Test
    @DisplayName("파일 저장 실패 - PDF가 아닌 파일")
    void storeFile_Fail_NotPdfFile() {
        // Given
        MultipartFile docFile = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(docFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_FILE_EXTENSION.getMessage());
    }

    @Test
    @DisplayName("파일 저장 실패 - 파일 크기 초과")
    void storeFile_Fail_FileSizeExceeded() {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                largeContent
        );

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(largeFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.FILE_SIZE_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("파일 저장 실패 - 경로 탐색 공격 시도")
    void storeFile_Fail_PathTraversalAttack() {
        // Given
        MultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../../../etc/passwd.pdf",
                "application/pdf",
                "Malicious content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(maliciousFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_FILE_NAME.getMessage());
    }

    @Test
    @DisplayName("파일 저장 실패 - 확장자 없는 파일")
    void storeFile_Fail_NoExtension() {
        // Given
        MultipartFile noExtFile = new MockMultipartFile(
                "file",
                "resume",
                "application/pdf",
                "PDF content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(noExtFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_FILE_NAME.getMessage());
    }

    @Test
    @DisplayName("파일 저장 성공 - 대문자 확장자 (대소문자 구분 없이 처리)")
    void storeFile_Success_UpperCaseExtension() throws Exception {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "resume.PDF",
                "application/pdf",
                "PDF content".getBytes()
        );

        // When
        String storedFileName = fileStorageService.storeFile(file);

        // Then
        assertThat(storedFileName).isNotNull();
        assertThat(storedFileName.toLowerCase()).endsWith(".pdf");
        assertThat(Files.exists(tempDir.resolve(storedFileName))).isTrue();
    }

    @Test
    @DisplayName("파일 경로 반환")
    void getFilePath_Success() {
        // Given
        String fileName = "test-file.pdf";

        // When
        Path filePath = fileStorageService.getFilePath(fileName);

        // Then
        assertThat(filePath).isNotNull();
        assertThat(filePath.toString()).contains(fileName);
        assertThat(filePath.isAbsolute()).isTrue();
    }

    @Test
    @DisplayName("파일 저장 실패 - JPG 파일")
    void storeFile_Fail_JpgFile() {
        // Given
        MultipartFile jpgFile = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                "JPG content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(jpgFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_FILE_EXTENSION.getMessage());
    }

    @Test
    @DisplayName("파일 저장 실패 - EXE 파일")
    void storeFile_Fail_ExeFile() {
        // Given
        MultipartFile exeFile = new MockMultipartFile(
                "file",
                "malware.pdf.exe",
                "application/octet-stream",
                "EXE content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(exeFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_FILE_EXTENSION.getMessage());
    }
}
