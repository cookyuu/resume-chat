package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.command.ApplicantCommand;
import com.cookyuu.resume_chat.common.enums.ClientType;
import com.cookyuu.resume_chat.common.response.ApiResponse;
import com.cookyuu.resume_chat.dto.ApplicantDto;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.service.ApplicantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "Applicant", description = "지원자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applicant")
public class ApplicantController {

    private final ApplicantService applicantService;

    @Operation(
            summary = "지원자 회원가입",
            description = "새로운 지원자 계정을 생성합니다.\n\n" +
                    "- 이메일은 고유해야 하며, 중복 시 409 에러가 발생합니다.\n" +
                    "- 비밀번호는 BCrypt로 암호화되어 저장됩니다.\n" +
                    "- 회원가입 시 계정 상태는 ACTIVE로 설정됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
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
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "C001",
                                        "message": "유효한 이메일 형식이 아닙니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이메일 중복",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "A002",
                                        "message": "이미 존재하는 이메일입니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> joinApplicant(
            @Valid @RequestBody ApplicantDto.JoinRequest request) {
        ApplicantCommand.Create command = ApplicantCommand.Create.from(request);
        applicantService.joinApplicant(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success());
    }

    @Operation(
            summary = "지원자 로그인",
            description = "지원자 계정으로 로그인하여 JWT 토큰을 발급받습니다.\n\n" +
                    "- 클라이언트 타입에 따라 Refresh Token 전달 방식이 다릅니다.\n" +
                    "- WEB: Refresh Token은 HttpOnly 쿠키로 전달, 응답 body에는 포함되지 않음\n" +
                    "- APP: Access Token과 Refresh Token 모두 응답 body에 포함\n" +
                    "- 로그인 실패 시 실패 횟수가 누적되며, 5회 실패 시 계정이 잠깁니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "APP 클라이언트 응답",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "data": {
                                                        "uuid": "123e4567-e89b-12d3-a456-426614174000",
                                                        "email": "applicant@example.com",
                                                        "name": "홍길동",
                                                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                                      },
                                                      "timestamp": "2024-02-14T10:30:00"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "WEB 클라이언트 응답",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "data": {
                                                        "uuid": "123e4567-e89b-12d3-a456-426614174000",
                                                        "email": "applicant@example.com",
                                                        "name": "홍길동",
                                                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                                      },
                                                      "timestamp": "2024-02-14T10:30:00"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "C001",
                                        "message": "이메일은 필수입니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (이메일 또는 비밀번호 불일치)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "A005",
                                        "message": "이메일 또는 비밀번호가 일치하지 않습니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "계정 잠김 (로그인 5회 실패)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "A004",
                                        "message": "계정이 잠겨있습니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<ApplicantDto.LoginResponse>> loginApplicant(
            @Valid @RequestBody ApplicantDto.LoginRequest request,
            @Parameter(
                    description = "클라이언트 타입 (web 또는 app, 기본값: web)",
                    example = "web",
                    schema = @Schema(allowableValues = {"web", "app"})
            )
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader) {
        ApplicantCommand.Login command = ApplicantCommand.Login.from(request);
        ApplicantDto.LoginResponse response = applicantService.login(command);

        ClientType clientType = ClientType.from(clientTypeHeader);

        if (clientType == ClientType.WEB) {
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                    .httpOnly(true)
//                    .secure(true)
                    .path("/")
                    .maxAge(Duration.ofDays(7))
//                    .sameSite("Strict")
                    .build();

            response = new ApplicantDto.LoginResponse(
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

    @Operation(
            summary = "지원자 프로필 조회",
            description = "현재 로그인한 지원자의 프로필 정보를 조회합니다.\n\n" +
                    "- JWT 인증이 필요합니다.\n" +
                    "- 본인의 정보만 조회 가능합니다.",
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
                                      "data": {
                                        "uuid": "123e4567-e89b-12d3-a456-426614174000",
                                        "email": "applicant@example.com",
                                        "name": "홍길동",
                                        "createdAt": "2024-01-01T10:00:00"
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "지원자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "A001",
                                        "message": "지원자를 찾을 수 없습니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ApplicantDto.ProfileResponse>> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicantDto.ProfileResponse response = applicantService.getProfile(userDetails.getUuid());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}