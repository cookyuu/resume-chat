package com.cookyuu.resume_chat.config;

import com.cookyuu.resume_chat.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정
 *
 * <p>STOMP 프로토콜을 사용한 실시간 양방향 통신 설정</p>
 *
 * <h2>브로드캐스트 Destination 패턴</h2>
 *
 * <h3>1. 클라이언트 → 서버 (메시지 전송)</h3>
 * <ul>
 *   <li>Destination: {@code /app/chat/{sessionToken}}</li>
 *   <li>Method: {@link com.cookyuu.resume_chat.controller.ChatWebSocketController#sendMessage}</li>
 *   <li>예시: {@code /app/chat/abc123-def456}</li>
 * </ul>
 *
 * <h3>2. 서버 → 클라이언트 (브로드캐스트)</h3>
 * <ul>
 *   <li>Destination: {@code /topic/session/{sessionToken}}</li>
 *   <li>구독 패턴: 클라이언트는 참여 중인 세션의 destination을 구독</li>
 *   <li>예시: {@code /topic/session/abc123-def456}</li>
 * </ul>
 *
 * <h3>3. 브로드캐스트 동작 방식</h3>
 * <ul>
 *   <li>1:N 통신: 하나의 메시지가 세션의 모든 구독자에게 전송됨</li>
 *   <li>자동 브로드캐스트: {@code @SendTo} 어노테이션으로 자동 처리</li>
 *   <li>수동 브로드캐스트: {@code SimpMessagingTemplate.convertAndSend()} 사용</li>
 * </ul>
 *
 * <h3>4. 메시지 형식</h3>
 * <ul>
 *   <li>DTO: {@link com.cookyuu.resume_chat.dto.ChatDto.WebSocketChatMessage}</li>
 *   <li>필드: messageId, sessionToken, senderType, messageType, content, sentAt</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * 메시지 브로커 설정
     *
     * <h3>Broker Prefix</h3>
     * <ul>
     *   <li><b>/topic</b>: 브로드캐스트용 (1:N 통신)</li>
     *   <li><b>/app</b>: 애플리케이션 destination prefix (클라이언트 → 서버)</li>
     * </ul>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지를 구독하는 요청의 prefix: /topic
        config.enableSimpleBroker("/topic");

        // 메시지를 발행하는 요청의 prefix: /app
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * STOMP 엔드포인트 등록
     *
     * - 엔드포인트: /api/ws
     * - SockJS fallback 지원
     * - CORS 허용: 모든 localhost 포트
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:31000",  // 프론트엔드 (Nginx)
                        "http://localhost:3000",   // 프론트엔드 (개발)
                        "http://localhost:5173",   // 프론트엔드 (Vite)
                        "http://localhost:8080"    // 백엔드
                )
                .withSockJS();
    }

    /**
     * 클라이언트 인바운드 채널 설정
     *
     * WebSocket 인증 인터셉터 등록
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
