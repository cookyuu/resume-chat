package com.cookyuu.resume_chat.dto;

import com.cookyuu.resume_chat.domain.Applicant;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class ApplicantDto {

    /**
     * 회원가입 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요")
        private String password;

        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
        private String name;
    }

    /**
     * 로그인 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    /**
     * 로그인 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LoginResponse {
        private UUID uuid;
        private String email;
        private String name;
        private String accessToken;
        private String refreshToken;
    }

    /**
     * 프로필 조회 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class ProfileResponse {
        private UUID uuid;
        private String email;
        private String name;
        private LocalDateTime createdAt;

        public static ProfileResponse from(Applicant applicant) {
            return new ProfileResponse(
                    applicant.getUuid(),
                    applicant.getEmail(),
                    applicant.getName(),
                    applicant.getCreatedAt()
            );
        }
    }
}
