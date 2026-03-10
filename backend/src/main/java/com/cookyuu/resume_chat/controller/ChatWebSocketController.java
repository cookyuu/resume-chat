package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

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
     * @return 저장된 메시지 정보
     *
     * Client → Server: /app/chat/{sessionToken}
     * Server → Clients: /topic/session/{sessionToken}
     */
    @MessageMapping("/chat/{sessionToken}")
    @SendTo("/topic/session/{sessionToken}")
    public WebSocketChatMessage sendMessage(
            @DestinationVariable String sessionToken,
            WebSocketChatMessage message
    ) {
        log.info("WebSocket 메시지 수신 - sessionToken: {}, senderType: {}", sessionToken, message.getSenderType());

        try {
            // 세션 조회
            ChatSession session = chatService.getSessionByToken(sessionToken);

            // 메시지 저장
            ChatMessage savedMessage = ChatMessage.createMessage(
                    session,
                    message.getSenderType(),
                    message.getContent()
            );
            chatService.saveMessage(savedMessage);

            // 세션 메시지 카운트 증가
            session.incrementMessageCount();

            // 저장된 메시지 정보 반환
            return WebSocketChatMessage.builder()
                    .messageId(savedMessage.getMessageId())
                    .sessionToken(sessionToken)
                    .senderType(savedMessage.getSenderType())
                    .content(savedMessage.getContent())
                    .sentAt(savedMessage.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("메시지 저장 실패 - sessionToken: {}, error: {}", sessionToken, e.getMessage());
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
