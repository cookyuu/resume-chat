package com.cookyuu.resume_chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class LoginApplicantDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID uuid;
        private String email;
        private String name;
        private String accessToken;
        private String refreshToken;
    }
}
