package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.common.response.ApiResponse;
import com.cookyuu.resume_chat.dto.ResumeDto;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Resume", description = "이력서 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applicant/resume")
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(
            summary = "이력서 업로드",
            description = "이력서 파일을 업로드하고 채팅 링크를 생성합니다.\n\n" +
                    "- JWT 인증이 필요합니다.\n" +
                    "- 지원 파일 형식: PDF\n" +
                    "- 최대 파일 크기: 10MB\n" +
                    "- 업로드 성공 시 채용담당자와 채팅 가능한 링크가 생성됩니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "이력서 업로드 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "resumeSlug": "123e4567-e89b-12d3-a456-426614174000",
                                        "title": "백엔드 개발자 이력서",
                                        "description": "Spring Boot 경력 5년",
                                        "originalFileName": "resume.pdf",
                                        "chatLink": "http://localhost:3000/chat/123e4567-e89b-12d3-a456-426614174000",
                                        "createdAt": "2024-02-14T10:30:00"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (파일 형식 또는 크기 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "F003",
                                        "message": "허용되지 않은 파일 확장자입니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "C002",
                                        "message": "인증이 필요합니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeDto.UploadResponse>> uploadResume(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "이력서 파일 (PDF)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "이력서 제목 (2~100자)", required = true, example = "백엔드 개발자 이력서")
            @RequestParam("title") String title,
            @Parameter(description = "이력서 설명 (최대 500자)", example = "Spring Boot 경력 5년")
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

    @Operation(
            summary = "내 이력서 목록 조회",
            description = "현재 로그인한 지원자의 모든 이력서 목록을 조회합니다.\n\n" +
                    "- JWT 인증이 필요합니다.\n" +
                    "- 본인이 업로드한 이력서만 조회됩니다.\n" +
                    "- 각 이력서의 조회수와 채팅 링크가 포함됩니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "resumeSlug": "123e4567-e89b-12d3-a456-426614174000",
                                          "title": "백엔드 개발자 이력서",
                                          "description": "Spring Boot 경력 5년",
                                          "originalFileName": "resume.pdf",
                                          "chatLink": "http://localhost:3000/chat/123e4567-e89b-12d3-a456-426614174000",
                                          "viewCnt": 10,
                                          "createdAt": "2024-02-14T10:30:00"
                                        }
                                      ],
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "C002",
                                        "message": "인증이 필요합니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeDto.ResumeInfo>>> getMyResumes(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ResumeDto.ResumeInfo> resumes = resumeService.getApplicantResumes(userDetails.getUuid());

        return ResponseEntity.ok(ApiResponse.success(resumes));
    }

    @Operation(
            summary = "이력서 삭제",
            description = "특정 이력서를 삭제합니다.\n\n" +
                    "- JWT 인증이 필요합니다.\n" +
                    "- 본인의 이력서만 삭제 가능합니다.\n" +
                    "- 이력서 삭제 시 관련된 채팅 세션과 메시지도 함께 삭제됩니다 (Cascade).",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": null,
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "C002",
                                        "message": "인증이 필요합니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (다른 사용자의 이력서)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "R002",
                                        "message": "이력서 접근 권한이 없습니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "이력서를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "R001",
                                        "message": "이력서를 찾을 수 없습니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            )
    })
    @DeleteMapping("/{resumeSlug}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "이력서 고유 식별자 (UUID)", required = true)
            @PathVariable UUID resumeSlug) {

        resumeService.deleteResume(userDetails.getUuid(), resumeSlug);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
