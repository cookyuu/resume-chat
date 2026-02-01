package com.cookyuu.resume_chat.command;

import com.cookyuu.resume_chat.dto.JoinApplicantDto;
import com.cookyuu.resume_chat.dto.LoginApplicantDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ApplicantCommand {

    @Getter
    @AllArgsConstructor
    public static class Create {
        private final String email;
        private final String password;
        private final String name;

        public static Create from(JoinApplicantDto.Request request) {
            return new Create(
                    request.getEmail(),
                    request.getPassword(),
                    request.getName()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Login {
        private final String email;
        private final String password;

        public static Login from(LoginApplicantDto.Request request) {
            return new Login(
                    request.getEmail(),
                    request.getPassword()
            );
        }
    }
}
