package com.cookyuu.resume_chat.security;

import com.cookyuu.resume_chat.service.RecruiterSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

/**
 * WebSocket 메시지 보안 인터셉터
 *
 * STOMP 메시지 전송 시 JWT 토큰 또는 sessionToken 검증 및 인증 정보 설정
 * - 지원자: JWT 토큰 필수
 * - 채용담당자: sessionToken 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RecruiterSessionService recruiterSessionService;

    /**
     * 메시지 전송 전 인증 검증
     *
     * STOMP CONNECT 프레임에서 JWT 토큰 또는 sessionToken 추출 및 검증
     * 인증 성공 시 StompHeaderAccessor에 사용자 정보 설정
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 1. JWT 토큰으로 인증 시도 (지원자)
            String jwt = extractJwtFromHeader(accessor);
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                try {
                    String email = jwtTokenProvider.getEmailFromToken(jwt);
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    accessor.setUser(authentication);
                    log.info("WebSocket JWT 인증 성공: email={}", email);
                    return message;

                } catch (Exception e) {
                    log.error("WebSocket JWT 인증 실패: {}", e.getMessage());
                    throw new IllegalArgumentException("Invalid JWT token");
                }
            }

            // 2. sessionToken으로 인증 시도 (채용담당자)
            String sessionToken = extractSessionToken(accessor);
            if (StringUtils.hasText(sessionToken)) {
                try {
                    // sessionToken 유효성 검증
                    if (recruiterSessionService.existsSessionToken(sessionToken)) {
                        recruiterSessionService.activateSession(sessionToken);

                        // 채용담당자용 임시 Authentication 생성
                        Authentication authentication = createRecruiterAuthentication(sessionToken);
                        accessor.setUser(authentication);

                        log.info("WebSocket sessionToken 인증 성공: sessionToken={}", sessionToken);
                        return message;
                    } else {
                        log.warn("유효하지 않은 sessionToken: {}", sessionToken);
                        throw new IllegalArgumentException("Invalid session token");
                    }

                } catch (Exception e) {
                    log.error("WebSocket sessionToken 인증 실패: {}", e.getMessage());
                    throw new IllegalArgumentException("Invalid session token");
                }
            }

            // 3. 인증 정보가 없는 경우 경고
            log.warn("WebSocket CONNECT 요청에 유효한 인증 정보가 없음 (JWT 또는 sessionToken 필요)");
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

    /**
     * STOMP 헤더에서 sessionToken 추출
     *
     * X-Session-Token 헤더에서 추출
     */
    private String extractSessionToken(StompHeaderAccessor accessor) {
        return accessor.getFirstNativeHeader("X-Session-Token");
    }

    /**
     * 채용담당자용 임시 Authentication 생성
     *
     * @param sessionToken 세션 토큰
     * @return Authentication 객체
     */
    private Authentication createRecruiterAuthentication(String sessionToken) {
        UserDetails userDetails = User.builder()
                .username("recruiter:" + sessionToken)
                .password("")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_RECRUITER")))
                .build();

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
