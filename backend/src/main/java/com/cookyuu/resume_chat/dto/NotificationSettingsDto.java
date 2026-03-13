package com.cookyuu.resume_chat.dto;

import com.cookyuu.resume_chat.domain.NotificationSettings;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 설정 DTO
 */
public class NotificationSettingsDto {

    /**
     * 알림 설정 조회 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        @NotNull(message = "이메일 알림 - 새 메시지 설정은 필수입니다")
        private Boolean emailNewMessage;

        @NotNull(message = "이메일 알림 - 새 세션 설정은 필수입니다")
        private Boolean emailNewSession;

        @NotNull(message = "푸시 알림 - 새 메시지 설정은 필수입니다")
        private Boolean pushNewMessage;

        /**
         * NotificationSettings 엔티티를 Response DTO로 변환
         *
         * @param settings 알림 설정 엔티티
         * @return Response DTO
         */
        public static Response from(NotificationSettings settings) {
            return Response.builder()
                    .emailNewMessage(settings.getEmailNewMessage())
                    .emailNewSession(settings.getEmailNewSession())
                    .pushNewMessage(settings.getPushNewMessage())
                    .build();
        }
    }

    /**
     * 알림 설정 업데이트 요청 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateRequest {
        @NotNull(message = "이메일 알림 - 새 메시지 설정은 필수입니다")
        private Boolean emailNewMessage;

        @NotNull(message = "이메일 알림 - 새 세션 설정은 필수입니다")
        private Boolean emailNewSession;

        @NotNull(message = "푸시 알림 - 새 메시지 설정은 필수입니다")
        private Boolean pushNewMessage;
    }
}
