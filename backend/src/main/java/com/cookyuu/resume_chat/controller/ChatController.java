package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.common.response.ApiResponse;
import com.cookyuu.resume_chat.dto.ChatDto;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.service.ChatService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Chat", description = "채팅 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "채용담당자 메시지 전송",
            description = "채용담당자가 지원자의 이력서에 첫 메시지를 전송하거나 기존 채팅 세션에 메시지를 전송합니다.\n\n" +
                    "- 동일한 이력서와 채용담당자 이메일로 첫 요청 시 새로운 채팅 세션이 생성됩니다.\n" +
                    "- 이후 동일한 조합으로 요청 시 기존 세션에 메시지가 추가됩니다.\n" +
                    "- 인증이 필요하지 않은 public 엔드포인트입니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "메시지 전송 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "sessionToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "messageId": "m1n2o3p4-q5r6-7890-stuv-wx1234567890",
                                        "recruiterEmail": "recruiter@company.com",
                                        "recruiterName": "김채용",
                                        "recruiterCompany": "ABC회사",
                                        "message": "안녕하세요, 귀하의 이력서를 흥미롭게 봤습니다.",
                                        "sentAt": "2024-02-14T10:30:00"
                                      },
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
                                        "message": "올바른 이메일 형식이 아닙니다"
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
    @PostMapping("/chat/{resumeSlug}/send")
    public ResponseEntity<ApiResponse<ChatDto.SendMessageResponse>> sendMessage(
            @Parameter(description = "이력서 고유 식별자 (UUID)", required = true)
            @PathVariable UUID resumeSlug,
            @Valid @RequestBody ChatDto.SendMessageRequest request) {

        ChatDto.SendMessageResponse response = chatService.sendMessage(resumeSlug, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(
            summary = "이력서별 채팅 세션 목록 조회",
            description = "지원자가 자신의 특정 이력서에 대한 모든 채팅 세션 목록을 조회합니다.\n\n" +
                    "- 채용담당자별로 생성된 채팅 세션 정보를 확인할 수 있습니다.\n" +
                    "- 각 세션의 총 메시지 수와 읽지 않은 메시지 수를 포함합니다.\n" +
                    "- JWT 인증이 필요하며, 본인의 이력서만 조회 가능합니다.",
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
                                        "resumeSlug": "123e4567-e89b-12d3-a456-426614174000",
                                        "resumeTitle": "백엔드 개발자 이력서",
                                        "sessions": [
                                          {
                                            "sessionToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                            "resumeSlug": "123e4567-e89b-12d3-a456-426614174000",
                                            "resumeTitle": "백엔드 개발자 이력서",
                                            "recruiterEmail": "recruiter@company.com",
                                            "recruiterName": "김채용",
                                            "recruiterCompany": "ABC회사",
                                            "totalMessages": 5,
                                            "unreadMessages": 2,
                                            "lastMessageAt": "2024-02-14T10:30:00",
                                            "createdAt": "2024-02-13T09:00:00"
                                          }
                                        ]
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
    @GetMapping("/applicant/resume/{resumeSlug}/chats")
    public ResponseEntity<ApiResponse<ChatDto.ResumeChatsResponse>> getResumeChats(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "이력서 고유 식별자 (UUID)", required = true)
            @PathVariable UUID resumeSlug) {

        ChatDto.ResumeChatsResponse response = chatService.getResumeChats(
                userDetails.getUuid(),
                resumeSlug
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "채팅 세션 메시지 조회",
            description = "특정 채팅 세션의 모든 메시지를 조회합니다.\n\n" +
                    "- 세션 정보와 메시지 목록을 함께 반환합니다.\n" +
                    "- 조회 시 읽지 않은 메시지를 자동으로 읽음 처리합니다.\n" +
                    "- JWT 인증이 필요하며, 본인의 채팅 세션만 조회 가능합니다.",
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
                                        "session": {
                                          "sessionToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                          "resumeSlug": "123e4567-e89b-12d3-a456-426614174000",
                                          "resumeTitle": "백엔드 개발자 이력서",
                                          "recruiterEmail": "recruiter@company.com",
                                          "recruiterName": "김채용",
                                          "recruiterCompany": "ABC회사",
                                          "totalMessages": 5,
                                          "unreadMessages": 2,
                                          "lastMessageAt": "2024-02-14T10:30:00",
                                          "createdAt": "2024-02-13T09:00:00"
                                        },
                                        "messages": [
                                          {
                                            "messageId": "m1n2o3p4-q5r6-7890-stuv-wx1234567890",
                                            "message": "안녕하세요, 귀하의 이력서를 흥미롭게 봤습니다.",
                                            "senderType": "RECRUITER",
                                            "readStatus": true,
                                            "sentAt": "2024-02-13T09:00:00"
                                          },
                                          {
                                            "messageId": "m2n3o4p5-q6r7-8901-stuv-wx2345678901",
                                            "message": "감사합니다. 면접 기회를 주신다면 성실히 임하겠습니다.",
                                            "senderType": "APPLICANT",
                                            "readStatus": true,
                                            "sentAt": "2024-02-13T10:15:00"
                                          }
                                        ]
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
                    responseCode = "403",
                    description = "권한 없음 (다른 사용자의 채팅 세션)",
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
                    description = "채팅 세션을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "S001",
                                        "message": "채팅 세션을 찾을 수 없습니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/applicant/chat/{sessionToken}/messages")
    public ResponseEntity<ApiResponse<ChatDto.ChatDetailResponse>> getSessionMessages(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "채팅 세션 토큰", required = true)
            @PathVariable String sessionToken) {

        ChatDto.ChatDetailResponse response = chatService.getSessionMessages(
                userDetails.getUuid(),
                sessionToken
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "지원자 메시지 전송",
            description = "지원자가 기존 채팅 세션에 메시지를 전송합니다.\n\n" +
                    "- 채용담당자가 먼저 생성한 세션에만 메시지를 전송할 수 있습니다.\n" +
                    "- JWT 인증이 필요하며, 본인의 채팅 세션에만 전송 가능합니다.\n" +
                    "- 메시지는 SenderType.APPLICANT로 저장됩니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "메시지 전송 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "sessionToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "messageId": "m3n4o5p6-q7r8-9012-stuv-wx3456789012",
                                        "message": "네, 면접 가능합니다. 일정 조율 부탁드립니다.",
                                        "sentAt": "2024-02-14T10:30:00"
                                      },
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
                                        "message": "메시지 내용은 필수입니다"
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
                    responseCode = "403",
                    description = "권한 없음 (다른 사용자의 채팅 세션)",
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
                    description = "채팅 세션을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "S001",
                                        "message": "채팅 세션을 찾을 수 없습니다"
                                      },
                                      "timestamp": "2024-02-14T10:30:00"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/applicant/chat/{sessionToken}/send")
    public ResponseEntity<ApiResponse<ChatDto.ApplicantSendMessageResponse>> sendMessageByApplicant(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "채팅 세션 토큰", required = true)
            @PathVariable String sessionToken,
            @Valid @RequestBody ChatDto.ApplicantSendMessageRequest request) {

        ChatDto.ApplicantSendMessageResponse response = chatService.sendMessageByApplicant(
                userDetails.getUuid(),
                sessionToken,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
