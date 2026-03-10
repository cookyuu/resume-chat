package com.cookyuu.resume_chat.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * WebSocket 메시지 보안 인터셉터
 *
 * STOMP 메시지 전송 시 JWT 토큰 검증 및 인증 정보 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * 메시지 전송 전 인증 검증
     *
     * STOMP CONNECT 프레임에서 JWT 토큰 추출 및 검증
     * 인증 성공 시 StompHeaderAccessor에 사용자 정보 설정
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwt = extractJwtFromHeader(accessor);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                try {
                    String email = jwtTokenProvider.getEmailFromToken(jwt);
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    accessor.setUser(authentication);
                    log.info("WebSocket 인증 성공: email={}", email);

                } catch (Exception e) {
                    log.error("WebSocket 인증 실패: {}", e.getMessage());
                    throw new IllegalArgumentException("Invalid JWT token");
                }
            } else {
                log.warn("WebSocket CONNECT 요청에 유효한 JWT 토큰이 없음");
                // 채용담당자의 경우 JWT 없이 접근 가능 (sessionToken으로 접근)
                // 지원자는 JWT 필수
            }
        }

        return message;
    }

    /**
     * STOMP 헤더에서 JWT 토큰 추출
     *
     * Authorization 헤더에서 "Bearer {token}" 형식으로 추출
     */
    private String extractJwtFromHeader(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
