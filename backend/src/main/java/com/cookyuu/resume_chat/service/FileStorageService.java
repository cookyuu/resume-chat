package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.config.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileProperties fileProperties;

    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + "." + fileExtension;

        try {
            Path uploadPath = Paths.get(fileProperties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("파일 저장 완료: originalFileName={}, storedFileName={}", originalFileName, storedFileName);
            return storedFileName;
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_NAME);
        }

        String fileExtension = getFileExtension(originalFileName).toLowerCase();
        if (!"pdf".equals(fileExtension)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_EXTENSION);
        }

        if (file.getSize() > fileProperties.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            throw new BusinessException(ErrorCode.INVALID_FILE_NAME);
        }
        return fileName.substring(lastIndexOf + 1);
    }

    public Path getFilePath(String fileName) {
        return Paths.get(fileProperties.getUploadDir()).toAbsolutePath().normalize().resolve(fileName);
    }

    /**
     * 채팅 첨부파일 저장
     *
     * <p>채팅에서 업로드된 첨부파일을 저장합니다. 다양한 파일 타입을 지원합니다.</p>
     *
     * @param file 업로드된 파일
     * @return 저장된 파일명
     * @throws BusinessException 파일 저장 실패 시
     */
    public String storeAttachment(MultipartFile file) {
        validateAttachment(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + "." + fileExtension;

        try {
            // 첨부파일 전용 디렉토리
            Path uploadPath = Paths.get(fileProperties.getUploadDir(), "attachments")
                    .toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("첨부파일 저장 완료: originalFileName={}, storedFileName={}", originalFileName, storedFileName);
            return storedFileName;
        } catch (IOException e) {
            log.error("첨부파일 저장 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    /**
     * 첨부파일 유효성 검증
     *
     * <p>파일 타입, 크기 제한 검증:
     * - 문서: PDF, DOCX, PPTX - 10MB
     * - 이미지: JPG, PNG, GIF - 5MB
     * - 압축: ZIP - 20MB
     * </p>
     *
     * @param file 검증할 파일
     * @throws BusinessException 유효하지 않은 파일 시
     */
    private void validateAttachment(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_NAME);
        }

        String fileExtension = getFileExtension(originalFileName).toLowerCase();
        long fileSize = file.getSize();

        // 파일 타입별 크기 제한
        if (isDocumentFile(fileExtension)) {
            if (fileSize > 10 * 1024 * 1024) { // 10MB
                throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
            }
        } else if (isImageFile(fileExtension)) {
            if (fileSize > 5 * 1024 * 1024) { // 5MB
                throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
            }
        } else if (isArchiveFile(fileExtension)) {
            if (fileSize > 20 * 1024 * 1024) { // 20MB
                throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
            }
        } else {
            throw new BusinessException(ErrorCode.INVALID_FILE_EXTENSION);
        }
    }

    private boolean isDocumentFile(String extension) {
        return extension.equals("pdf") || extension.equals("docx") || extension.equals("pptx");
    }

    private boolean isImageFile(String extension) {
        return extension.equals("jpg") || extension.equals("jpeg") ||
                extension.equals("png") || extension.equals("gif");
    }

    private boolean isArchiveFile(String extension) {
        return extension.equals("zip");
    }

    /**
     * 첨부파일 경로 반환
     *
     * @param fileName 저장된 파일명
     * @return 파일 경로
     */
    public Path getAttachmentPath(String fileName) {
        return Paths.get(fileProperties.getUploadDir(), "attachments")
                .toAbsolutePath().normalize().resolve(fileName);
    }
}
