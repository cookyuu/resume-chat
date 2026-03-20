package com.cookyuu.resume_chat.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter - Bucket4j를 사용한 API 호출 제한
 *
 * <p>클라이언트 IP 기반으로 API 호출 횟수를 제한하여 서버 리소스를 보호합니다.</p>
 *
 * <h3>Rate Limit 정책</h3>
 * <ul>
 *   <li>일반 API: 분당 60회</li>
 *   <li>로그인 API: 분당 5회</li>
 *   <li>메시지 전송 API: 분당 20회</li>
 * </ul>
 *
 * <h3>응답 헤더</h3>
 * <ul>
 *   <li>X-RateLimit-Remaining: 남은 요청 횟수</li>
 *   <li>X-RateLimit-Retry-After-Seconds: 재시도 가능 시간 (429 에러 시)</li>
 * </ul>
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String key = getClientIP(request) + ":" + getRateLimitKey(request);
        Bucket bucket = resolveBucket(key, request);

        if (bucket.tryConsume(1)) {
            // 요청 허용
            long remainingTokens = bucket.getAvailableTokens();
            response.addHeader("X-RateLimit-Remaining", String.valueOf(remainingTokens));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit 초과
            long waitForRefill = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-RateLimit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write(String.format(
                    "{\"success\":false,\"error\":\"Too many requests. Please try again in %d seconds.\",\"timestamp\":\"%s\"}",
                    waitForRefill,
                    java.time.LocalDateTime.now()
            ));
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     *
     * <p>프록시를 통한 요청인 경우 X-Forwarded-For 헤더에서 실제 IP를 추출합니다.</p>
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 요청 경로에 따른 Rate Limit 키 반환
     */
    private String getRateLimitKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.contains("/api/applicant/login") || path.contains("/api/applicant/join")) {
            return "auth";
        } else if (path.contains("/send")) {
            return "message";
        }
        return "general";
    }

    /**
     * Rate Limit 버킷 생성 또는 조회
     *
     * <p>요청 경로에 따라 다른 Rate Limit 정책을 적용합니다.</p>
     */
    private Bucket resolveBucket(String key, HttpServletRequest request) {
        return cache.computeIfAbsent(key, k -> {
            String path = request.getRequestURI();
            Bandwidth limit;

            if (path.contains("/api/applicant/login") || path.contains("/api/applicant/join")) {
                // 로그인/회원가입: 분당 5회
                limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
            } else if (path.contains("/send")) {
                // 메시지 전송: 분당 20회
                limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
            } else {
                // 일반 API: 분당 60회
                limit = Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
            }

            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
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
