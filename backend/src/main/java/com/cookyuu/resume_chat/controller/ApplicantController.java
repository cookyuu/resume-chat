package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.command.ApplicantCommand;
import com.cookyuu.resume_chat.common.enums.ClientType;
import com.cookyuu.resume_chat.common.response.ApiResponse;
import com.cookyuu.resume_chat.dto.ApplicantProfileDto;
import com.cookyuu.resume_chat.dto.JoinApplicantDto;
import com.cookyuu.resume_chat.dto.LoginApplicantDto;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.service.ApplicantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applicant")
public class ApplicantController {

    private final ApplicantService applicantService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> joinApplicant(
            @Valid @RequestBody JoinApplicantDto.Request request) {
        ApplicantCommand.Create command = ApplicantCommand.Create.from(request);
        applicantService.joinApplicant(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginApplicantDto.Response>> loginApplicant(
            @Valid @RequestBody LoginApplicantDto.Request request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader) {
        ApplicantCommand.Login command = ApplicantCommand.Login.from(request);
        LoginApplicantDto.Response response = applicantService.login(command);

        ClientType clientType = ClientType.from(clientTypeHeader);

        if (clientType == ClientType.WEB) {
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                    .httpOnly(true)
//                    .secure(true)
                    .path("/")
                    .maxAge(Duration.ofDays(7))
//                    .sameSite("Strict")
                    .build();

            response = new LoginApplicantDto.Response(
                    response.getUuid(),
                    response.getEmail(),
                    response.getName(),
                    response.getAccessToken(),
                    null
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(ApiResponse.success(response));
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ApplicantProfileDto.Response>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicantProfileDto.Response response = applicantService.getProfile(userDetails.getUuid());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}