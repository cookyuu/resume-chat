package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket 채팅 컨트롤러
 *
 * STOMP 프로토콜을 사용한 실시간 채팅 메시지 처리
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;

    /**
     * 채팅 메시지 전송
     *
     * @param sessionToken 세션 토큰
     * @param message      메시지 객체
     * @param headerAccessor STOMP 헤더 접근자
     * @return 저장된 메시지 정보
     *
     * Client → Server: /app/chat/{sessionToken}
     * Server → Clients: /topic/session/{sessionToken}
     */
    @MessageMapping("/chat/{sessionToken}")
    @SendTo("/topic/session/{sessionToken}")
    public WebSocketChatMessage sendMessage(
            @DestinationVariable String sessionToken,
            WebSocketChatMessage message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        log.info("WebSocket 메시지 수신 - sessionToken: {}, senderType: {}", sessionToken, message.getSenderType());

        try {
            // 지원자 UUID 추출 (지원자인 경우에만)
            UUID applicantUuid = null;
            if (message.getSenderType() == SenderType.APPLICANT) {
                Principal user = headerAccessor.getUser();
                if (user == null) {
                    log.error("지원자 메시지 전송 시 JWT 인증 누락 - sessionToken: {}", sessionToken);
                    throw new BusinessException(ErrorCode.WEBSOCKET_AUTH_REQUIRED);
                }

                if (user instanceof UsernamePasswordAuthenticationToken) {
                    UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) user;
                    if (authentication.getPrincipal() instanceof CustomUserDetails) {
                        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                        applicantUuid = userDetails.getUuid();
                    }
                }
            }

            // 메시지 저장 및 세션 업데이트 (트랜잭션 내에서 권한 검증 포함)
            ChatMessage savedMessage = chatService.saveMessageAndUpdateSession(
                    sessionToken,
                    message.getSenderType(),
                    message.getContent(),
                    applicantUuid
            );

            // 저장된 메시지 정보 반환
            return WebSocketChatMessage.builder()
                    .messageId(savedMessage.getMessageId())
                    .sessionToken(sessionToken)
                    .senderType(savedMessage.getSenderType())
                    .content(savedMessage.getContent())
                    .sentAt(savedMessage.getCreatedAt())
                    .build();

        } catch (BusinessException e) {
            log.error("메시지 저장 실패 (비즈니스 예외) - sessionToken: {}, error: {}", sessionToken, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("메시지 저장 실패 - sessionToken: {}, error: {}", sessionToken, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * WebSocket 채팅 메시지 DTO
     */
    @lombok.Getter
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class WebSocketChatMessage {
        private UUID messageId;
        private String sessionToken;
        private SenderType senderType;
        private String content;
        private LocalDateTime sentAt;
    }
}
