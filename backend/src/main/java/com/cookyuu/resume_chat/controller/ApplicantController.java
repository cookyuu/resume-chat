package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.command.ApplicantCommand;
import com.cookyuu.resume_chat.common.response.ApiResponse;
import com.cookyuu.resume_chat.dto.JoinApplicantDto;
import com.cookyuu.resume_chat.service.ApplicantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applicant")
public class ApplicantController {

    private final ApplicantService applicantService;

    /**
     * 지원자 회원가입
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> joinApplicant(
            @Valid @RequestBody JoinApplicantDto.Request request) {
        ApplicantCommand.Create command = ApplicantCommand.Create.from(request);
        applicantService.joinApplicant(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success());
    }
}