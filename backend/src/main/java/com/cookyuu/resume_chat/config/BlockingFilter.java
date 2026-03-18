package com.cookyuu.resume_chat.config;

import com.cookyuu.resume_chat.domain.BlockedIp;
import com.cookyuu.resume_chat.repository.BlockedEmailRepository;
import com.cookyuu.resume_chat.repository.BlockedIpRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Blocking Filter - 차단된 IP/이메일 요청 차단
 *
 * <p>악성 사용자의 IP 주소 또는 이메일을 차단하여 서비스 보호</p>
 *
 * <h3>차단 정책</h3>
 * <ul>
 *   <li>IP 차단: 만료 시간 설정 가능 (영구 차단 가능)</li>
 *   <li>이메일 차단: 영구 차단</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class BlockingFilter extends OncePerRequestFilter {

    private final BlockedIpRepository blockedIpRepository;
    private final BlockedEmailRepository blockedEmailRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIP(request);

        // IP 차단 확인
        Optional<BlockedIp> blockedIp = blockedIpRepository.findByIpAddress(clientIp);
        if (blockedIp.isPresent()) {
            BlockedIp block = blockedIp.get();

            // 만료된 차단은 삭제
            if (block.isExpired()) {
                blockedIpRepository.delete(block);
            } else {
                // 차단된 IP
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                        "{\"success\":false,\"error\":\"Access denied. Your IP has been blocked.\",\"reason\":\"%s\",\"timestamp\":\"%s\"}",
                        block.getReason() != null ? block.getReason() : "Violation of terms of service",
                        java.time.LocalDateTime.now()
                ));
                return;
            }
        }

        // 이메일 차단 확인 (로그인/회원가입 요청만)
        if (request.getRequestURI().contains("/login") || request.getRequestURI().contains("/join")) {
            // 요청 본문에서 이메일 추출은 복잡하므로, Service 레이어에서 확인하도록 위임
            // 여기서는 IP 차단만 처리
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // WebSocket, Swagger, 정적 리소스는 필터링 제외
        return path.startsWith("/ws") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/uploads");
    }
}
