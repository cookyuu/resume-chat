package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.common.response.ApiResponse;
import com.cookyuu.resume_chat.dto.NotificationSettingsDto;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.service.NotificationSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 알림 설정 API 컨트롤러
 *
 * <p>두 가지 경로를 지원합니다:</p>
 * <ul>
 *   <li>/api/applicant/notification-settings (백엔드 표준)</li>
 *   <li>/api/applicant/settings/notifications (프론트엔드 호환)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping({
    "/api/applicant/notification-settings",
    "/api/applicant/settings/notifications"
})
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;

    /**
     * 알림 설정 조회
     *
     * <p>지원자의 알림 설정을 조회합니다. 설정이 없으면 기본 설정을 생성하여 반환합니다.</p>
     *
     * <h3>기본 설정</h3>
     * <ul>
     *   <li>emailNewMessage: true</li>
     *   <li>emailNewSession: true</li>
     *   <li>pushNewMessage: false</li>
     * </ul>
     *
     * @param userDetails 인증된 지원자 정보
     * @return 알림 설정 응답
     */
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingsDto.Response>> getSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        NotificationSettingsDto.Response response = notificationSettingsService.getSettings(userDetails.getUuid());

        log.info("알림 설정 조회 API 호출 - applicantUuid: {}", userDetails.getUuid());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 알림 설정 업데이트
     *
     * <p>지원자의 알림 설정을 업데이트합니다.</p>
     *
     * <h3>요청 본문 예시</h3>
     * <pre>
     * {
     *   "emailNewMessage": true,
     *   "emailNewSession": false,
     *   "pushNewMessage": true
     * }
     * </pre>
     *
     * @param userDetails 인증된 지원자 정보
     * @param request 업데이트 요청 DTO
     * @return 업데이트된 알림 설정 응답
     */
    @PutMapping
    public ResponseEntity<ApiResponse<NotificationSettingsDto.Response>> updateSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationSettingsDto.UpdateRequest request) {

        NotificationSettingsDto.Response response = notificationSettingsService.updateSettings(userDetails.getUuid(), request);

        log.info("알림 설정 업데이트 API 호출 - applicantUuid: {}, emailNewMessage: {}, emailNewSession: {}, pushNewMessage: {}",
                userDetails.getUuid(), request.getEmailNewMessage(), request.getEmailNewSession(), request.getPushNewMessage());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
