package com.cookyuu.resume_chat.command;

import com.cookyuu.resume_chat.dto.JoinApplicantDto;
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
}
