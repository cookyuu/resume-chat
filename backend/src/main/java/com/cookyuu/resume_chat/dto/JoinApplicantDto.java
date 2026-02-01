package com.cookyuu.resume_chat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class JoinApplicantDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
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
}
