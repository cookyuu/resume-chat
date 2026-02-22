package com.cookyuu.resume_chat.dto;

import com.cookyuu.resume_chat.entity.Resume;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class ResumeDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRequest {
        @NotBlank(message = "제목은 필수입니다")
        @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요")
        private String title;

        @Size(max = 500, message = "설명은 500자 이하로 입력해주세요")
        private String description;
    }

    @Getter
    @AllArgsConstructor
    public static class UploadResponse {
        private UUID resumeSlug;
        private String title;
        private String description;
        private String originalFileName;
        private String chatLink;
        private LocalDateTime createdAt;

        public static UploadResponse from(Resume resume, String frontendUrl) {
            String chatLink = frontendUrl + "/chat/" + resume.getResumeSlug();
            return new UploadResponse(
                    resume.getResumeSlug(),
                    resume.getTitle(),
                    resume.getDescription(),
                    resume.getOriginalFileName(),
                    chatLink,
                    resume.getCreatedAt()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ResumeInfo {
        private UUID resumeSlug;
        private String title;
        private String description;
        private String originalFileName;
        private String chatLink;
        private int viewCnt;
        private LocalDateTime createdAt;

        public static ResumeInfo from(Resume resume, String frontendUrl) {
            String chatLink = frontendUrl + "/chat/" + resume.getResumeSlug();
            return new ResumeInfo(
                    resume.getResumeSlug(),
                    resume.getTitle(),
                    resume.getDescription(),
                    resume.getOriginalFileName(),
                    chatLink,
                    resume.getViewCnt(),
                    resume.getCreatedAt()
            );
        }
    }
}
