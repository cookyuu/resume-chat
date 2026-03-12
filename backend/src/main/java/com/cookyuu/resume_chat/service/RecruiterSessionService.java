package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채용담당자 세션 관리 서비스
 *
 * 채용담당자는 JWT 토큰이 없으므로 sessionToken 기반의 임시 인증 시스템을 사용합니다.
 * - 세션 토큰 검증
 * - 세션 접근 권한 확인
 * - 세션 만료 관리 (24시간)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterSessionService {

    private final ChatSessionRepository chatSessionRepository;

    // 채용담당자 세션 활성화 시간 추적 (메모리 기반, 서버 재시작 시 초기화됨)
    // 실제 프로덕션에서는 Redis 등을 사용하는 것을 권장
    private final ConcurrentHashMap<String, LocalDateTime> sessionLastAccessMap = new ConcurrentHashMap<>();

    private static final Duration SESSION_TIMEOUT = Duration.ofHours(24);

    /**
     * 세션 토큰 검증 및 세션 조회
     *
     * @param sessionToken 세션 토큰
     * @return 채팅 세션
     * @throws BusinessException 세션이 없거나 만료된 경우
     */
    @Transactional(readOnly = true)
    public ChatSession validateAndGetSession(String sessionToken) {
        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 세션 만료 확인
        if (isSessionExpired(sessionToken)) {
            log.warn("채용담당자 세션 만료: sessionToken={}", sessionToken);
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        // 마지막 접근 시간 갱신
        updateLastAccessTime(sessionToken);

        log.debug("채용담당자 세션 검증 성공: sessionToken={}, recruiterEmail={}",
                sessionToken, session.getRecruiterEmail());

        return session;
    }

    /**
     * 채용담당자 이메일로 세션 접근 권한 검증
     *
     * @param sessionToken 세션 토큰
     * @param recruiterEmail 채용담당자 이메일
     * @throws BusinessException 권한이 없는 경우
     */
    @Transactional(readOnly = true)
    public void validateRecruiterAccess(String sessionToken, String recruiterEmail) {
        ChatSession session = validateAndGetSession(sessionToken);

        if (!session.getRecruiterEmail().equals(recruiterEmail)) {
            log.error("채용담당자 세션 접근 권한 없음 - sessionToken: {}, requestEmail: {}, ownerEmail: {}",
                    sessionToken, recruiterEmail, session.getRecruiterEmail());
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }
    }

    /**
     * 세션 활성화 (최초 진입 시)
     *
     * @param sessionToken 세션 토큰
     */
    public void activateSession(String sessionToken) {
        updateLastAccessTime(sessionToken);
        log.info("채용담당자 세션 활성화: sessionToken={}", sessionToken);
    }

    /**
     * 세션 비활성화 (명시적 로그아웃 또는 세션 종료 시)
     *
     * @param sessionToken 세션 토큰
     */
    public void deactivateSession(String sessionToken) {
        sessionLastAccessMap.remove(sessionToken);
        log.info("채용담당자 세션 비활성화: sessionToken={}", sessionToken);
    }

    /**
     * 세션 만료 여부 확인
     *
     * @param sessionToken 세션 토큰
     * @return 만료 여부
     */
    private boolean isSessionExpired(String sessionToken) {
        LocalDateTime lastAccess = sessionLastAccessMap.get(sessionToken);
        if (lastAccess == null) {
            // 첫 접근이거나 서버 재시작 후 첫 접근
            return false;
        }

        Duration elapsed = Duration.between(lastAccess, LocalDateTime.now());
        return elapsed.compareTo(SESSION_TIMEOUT) > 0;
    }

    /**
     * 마지막 접근 시간 갱신
     *
     * @param sessionToken 세션 토큰
     */
    private void updateLastAccessTime(String sessionToken) {
        sessionLastAccessMap.put(sessionToken, LocalDateTime.now());
    }

    /**
     * 세션 토큰 존재 여부 확인
     *
     * @param sessionToken 세션 토큰
     * @return 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean existsSessionToken(String sessionToken) {
        return chatSessionRepository.findBySessionToken(sessionToken).isPresent();
    }
}
