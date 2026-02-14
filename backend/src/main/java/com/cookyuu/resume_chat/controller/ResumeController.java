package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.common.response.ApiResponse;
import com.cookyuu.resume_chat.dto.ResumeDto;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applicant/resume")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<ApiResponse<ResumeDto.UploadResponse>> uploadResume(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) {

        ResumeDto.UploadResponse response = resumeService.uploadResume(
                userDetails.getUuid(),
                title,
                description,
                file
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeDto.ResumeInfo>>> getMyResumes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ResumeDto.ResumeInfo> resumes = resumeService.getApplicantResumes(userDetails.getUuid());

        return ResponseEntity.ok(ApiResponse.success(resumes));
    }

    @DeleteMapping("/{resumeSlug}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID resumeSlug) {

        resumeService.deleteResume(userDetails.getUuid(), resumeSlug);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
