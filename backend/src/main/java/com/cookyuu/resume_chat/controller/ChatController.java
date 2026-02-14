package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.common.response.ApiResponse;
import com.cookyuu.resume_chat.dto.ChatDto;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat/{resumeSlug}/send")
    public ResponseEntity<ApiResponse<ChatDto.SendMessageResponse>> sendMessage(
            @PathVariable UUID resumeSlug,
            @Valid @RequestBody ChatDto.SendMessageRequest request) {

        ChatDto.SendMessageResponse response = chatService.sendMessage(resumeSlug, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/applicant/resume/{resumeSlug}/chats")
    public ResponseEntity<ApiResponse<ChatDto.ResumeChatsResponse>> getResumeChats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID resumeSlug) {

        ChatDto.ResumeChatsResponse response = chatService.getResumeChats(
                userDetails.getUuid(),
                resumeSlug
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/applicant/chat/{sessionToken}/messages")
    public ResponseEntity<ApiResponse<ChatDto.ChatDetailResponse>> getSessionMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionToken) {

        ChatDto.ChatDetailResponse response = chatService.getSessionMessages(
                userDetails.getUuid(),
                sessionToken
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
