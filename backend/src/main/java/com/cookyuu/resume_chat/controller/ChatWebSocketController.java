package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.common.enums.MessageType;
import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.dto.ChatDto;
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
     * <p>WebSocket을 통해 클라이언트로부터 채팅 메시지를 수신하고,
     * 저장된 메시지를 해당 세션의 모든 클라이언트에게 브로드캐스트합니다.</p>
     *
     * <h3>엔드포인트</h3>
     * <ul>
     *   <li>Client → Server: {@code /app/chat/{sessionToken}}</li>
     *   <li>Server → Clients: {@code /topic/session/{sessionToken}}</li>
     * </ul>
     *
     * <h3>메시지 타입 검증</h3>
     * <ul>
     *   <li>현재 지원: TEXT</li>
     *   <li>향후 확장: IMAGE, FILE, SYSTEM</li>
     * </ul>
     *
     * @param sessionToken 세션 토큰
     * @param message      메시지 객체
     * @param headerAccessor STOMP 헤더 접근자
     * @return 저장된 메시지 정보
     */
    @MessageMapping("/chat/{sessionToken}")
    @SendTo("/topic/session/{sessionToken}")
    public ChatDto.WebSocketChatMessage sendMessage(
            @DestinationVariable String sessionToken,
            ChatDto.WebSocketChatMessage message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        log.info("WebSocket 메시지 수신 - sessionToken: {}, senderType: {}, messageType: {}",
                sessionToken, message.getSenderType(), message.getMessageType());

        try {
            // 메시지 타입 검증 (현재는 TEXT만 허용)
            if (message.getMessageType() != MessageType.TEXT) {
                log.error("지원하지 않는 메시지 타입 - sessionToken: {}, messageType: {}",
                        sessionToken, message.getMessageType());
                throw new BusinessException(ErrorCode.INVALID_MESSAGE_TYPE);
            }

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
            return ChatDto.WebSocketChatMessage.from(sessionToken, savedMessage);

        } catch (BusinessException e) {
            log.error("메시지 저장 실패 (비즈니스 예외) - sessionToken: {}, error: {}", sessionToken, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("메시지 저장 실패 - sessionToken: {}, error: {}", sessionToken, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 입력 중 표시(Typing Indicator) 이벤트 전송
     *
     * <p>사용자가 메시지를 입력 중일 때 다른 사용자들에게 실시간으로 알립니다.</p>
     *
     * <h3>엔드포인트</h3>
     * <ul>
     *   <li>Client → Server: {@code /app/chat/{sessionToken}/typing}</li>
     *   <li>Server → Clients: {@code /topic/session/{sessionToken}/typing}</li>
     * </ul>
     *
     * <h3>사용 방법</h3>
     * <ul>
     *   <li>클라이언트가 입력 시작 시: typing = true로 전송</li>
     *   <li>클라이언트가 입력 중단 시: typing = false로 전송</li>
     *   <li>자동 타임아웃: 3초간 새 입력 없으면 클라이언트가 typing = false 전송 권장</li>
     * </ul>
     *
     * <h3>주의사항</h3>
     * <ul>
     *   <li>입력 중 표시는 저장되지 않으며, 실시간으로만 브로드캐스트됩니다.</li>
     *   <li>과도한 이벤트 발송 방지를 위해 클라이언트에서 debounce 처리 권장</li>
     * </ul>
     *
     * @param sessionToken 세션 토큰
     * @param typingEvent 입력 중 이벤트 정보
     * @return 타임스탬프가 추가된 입력 중 이벤트
     */
    @MessageMapping("/chat/{sessionToken}/typing")
    @SendTo("/topic/session/{sessionToken}/typing")
    public ChatDto.TypingEvent sendTypingIndicator(
            @DestinationVariable String sessionToken,
            ChatDto.TypingEvent typingEvent
    ) {
        log.debug("입력 중 이벤트 수신 - sessionToken: {}, senderName: {}, senderType: {}, typing: {}",
                sessionToken, typingEvent.getSenderName(), typingEvent.getSenderType(), typingEvent.getTyping());

        // 서버 타임스탬프 설정
        return ChatDto.TypingEvent.builder()
                .sessionToken(sessionToken)
                .senderName(typingEvent.getSenderName())
                .senderType(typingEvent.getSenderType())
                .typing(typingEvent.getTyping())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
