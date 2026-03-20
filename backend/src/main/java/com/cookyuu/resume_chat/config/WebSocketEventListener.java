package com.cookyuu.resume_chat.config;

import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.service.PresenceService;
import com.cookyuu.resume_chat.service.RecruiterSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 이벤트 리스너
 *
 * WebSocket 연결/해제 이벤트를 감지하여 접속 상태를 관리합니다.
 * - SessionConnectEvent: WebSocket 연결 시
 * - SessionDisconnectEvent: WebSocket 연결 해제 시
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PresenceService presenceService;
    private final RecruiterSessionService recruiterSessionService;

    /**
     * WebSocket 연결 이벤트 처리
     *
     * 사용자가 WebSocket에 연결할 때 호출됩니다.
     * - 지원자: JWT 토큰에서 UUID 추출
     * - 채용담당자: sessionToken 헤더에서 추출
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String webSocketSessionId = headerAccessor.getSessionId();

        log.info("WebSocket 연결 이벤트 - sessionId: {}", webSocketSessionId);

        try {
            // 1. 지원자 인증 정보 확인 (JWT)
            if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken authentication =
                        (UsernamePasswordAuthenticationToken) headerAccessor.getUser();

                if (authentication.getPrincipal() instanceof CustomUserDetails) {
                    // 지원자 접속
                    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                    String chatSessionToken = extractChatSessionToken(headerAccessor);

                    if (chatSessionToken != null) {
                        presenceService.userConnected(
                                webSocketSessionId,
                                chatSessionToken,
                                userDetails.getUuid().toString(),
                                SenderType.APPLICANT,
                                userDetails.getUsername() // 이메일
                        );
                    } else {
                        log.warn("지원자 WebSocket 연결 시 chatSessionToken 누락 - sessionId: {}", webSocketSessionId);
                    }
                    return;
                }

                // 채용담당자 인증 정보 확인 (sessionToken 기반)
                if (authentication.getPrincipal() instanceof UserDetails) {
                    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                    String username = userDetails.getUsername();

                    // "recruiter:{sessionToken}" 형식
                    if (username.startsWith("recruiter:")) {
                        String sessionToken = username.substring("recruiter:".length());
                        String chatSessionToken = extractChatSessionToken(headerAccessor);

                        if (chatSessionToken != null && chatSessionToken.equals(sessionToken)) {
                            presenceService.userConnected(
                                    webSocketSessionId,
                                    chatSessionToken,
                                    sessionToken,
                                    SenderType.RECRUITER,
                                    "채용담당자" // 실제로는 ChatSession에서 recruiterName을 가져와야 함
                            );
                        } else {
                            log.warn("채용담당자 WebSocket 연결 시 chatSessionToken 불일치 - sessionId: {}, sessionToken: {}, chatSessionToken: {}",
                                    webSocketSessionId, sessionToken, chatSessionToken);
                        }
                        return;
                    }
                }
            }

            log.warn("WebSocket 연결 시 인증 정보 없음 - sessionId: {}", webSocketSessionId);

        } catch (Exception e) {
            log.error("WebSocket 연결 이벤트 처리 실패 - sessionId: {}, error: {}",
                    webSocketSessionId, e.getMessage(), e);
        }
    }

    /**
     * WebSocket 연결 해제 이벤트 처리
     *
     * 사용자가 WebSocket 연결을 해제할 때 호출됩니다.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String webSocketSessionId = headerAccessor.getSessionId();

        log.info("WebSocket 연결 해제 이벤트 - sessionId: {}", webSocketSessionId);

        try {
            presenceService.userDisconnected(webSocketSessionId);
        } catch (Exception e) {
            log.error("WebSocket 연결 해제 이벤트 처리 실패 - sessionId: {}, error: {}",
                    webSocketSessionId, e.getMessage(), e);
        }
    }

    /**
     * STOMP 헤더에서 chatSessionToken 추출
     *
     * 클라이언트가 WebSocket 연결 시 X-Chat-Session-Token 헤더를 통해 전달
     */
    private String extractChatSessionToken(StompHeaderAccessor headerAccessor) {
        return headerAccessor.getFirstNativeHeader("X-Chat-Session-Token");
    }
}
