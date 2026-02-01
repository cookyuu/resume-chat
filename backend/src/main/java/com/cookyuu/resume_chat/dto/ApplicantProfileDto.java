package com.cookyuu.resume_chat.dto;

import com.cookyuu.resume_chat.entity.Applicant;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class ApplicantProfileDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private UUID uuid;
        private String email;
        private String name;
        private LocalDateTime createdAt;

        public static Response from(Applicant applicant) {
            return new Response(
                    applicant.getUuid(),
                    applicant.getEmail(),
                    applicant.getName(),
                    applicant.getCreatedAt()
            );
        }
    }
}
