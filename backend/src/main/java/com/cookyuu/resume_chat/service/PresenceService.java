package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.enums.SenderType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 접속 상태 관리 서비스
 *
 * WebSocket 연결/해제 시 사용자 접속 상태를 추적하고 브로드캐스트합니다.
 * - 채팅방별 접속자 목록 관리
 * - 접속/해제 이벤트 브로드캐스트
 * - 지원자/채용담당자 구분 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final SimpMessagingTemplate messagingTemplate;

    // sessionToken -> 접속자 목록 (Set<UserPresence>)
    private final ConcurrentHashMap<String, Set<UserPresence>> sessionPresenceMap = new ConcurrentHashMap<>();

    // WebSocket sessionId -> UserPresence 매핑 (연결 해제 시 사용)
    private final ConcurrentHashMap<String, UserPresence> webSocketSessionMap = new ConcurrentHashMap<>();

    /**
     * 사용자 접속 처리
     *
     * @param webSocketSessionId WebSocket 세션 ID
     * @param chatSessionToken 채팅 세션 토큰
     * @param userIdentifier 사용자 식별자 (지원자 UUID 또는 채용담당자 sessionToken)
     * @param senderType 사용자 타입 (APPLICANT/RECRUITER)
     * @param displayName 표시 이름
     */
    public void userConnected(String webSocketSessionId, String chatSessionToken,
                            String userIdentifier, SenderType senderType, String displayName) {
        UserPresence presence = new UserPresence(
                webSocketSessionId,
                chatSessionToken,
                userIdentifier,
                senderType,
                displayName,
                LocalDateTime.now()
        );

        // 세션별 접속자 목록에 추가
        sessionPresenceMap.computeIfAbsent(chatSessionToken, k -> ConcurrentHashMap.newKeySet())
                .add(presence);

        // WebSocket 세션 매핑 추가
        webSocketSessionMap.put(webSocketSessionId, presence);

        log.info("사용자 접속 - chatSession: {}, user: {}, type: {}, displayName: {}",
                chatSessionToken, userIdentifier, senderType, displayName);

        // 해당 채팅방에 접속 상태 브로드캐스트
        broadcastPresenceUpdate(chatSessionToken, PresenceEventType.CONNECTED, presence);
    }

    /**
     * 사용자 접속 해제 처리
     *
     * @param webSocketSessionId WebSocket 세션 ID
     */
    public void userDisconnected(String webSocketSessionId) {
        UserPresence presence = webSocketSessionMap.remove(webSocketSessionId);

        if (presence != null) {
            String chatSessionToken = presence.getChatSessionToken();

            // 세션별 접속자 목록에서 제거
            Set<UserPresence> presences = sessionPresenceMap.get(chatSessionToken);
            if (presences != null) {
                presences.remove(presence);

                // 채팅방에 접속자가 없으면 맵에서 제거
                if (presences.isEmpty()) {
                    sessionPresenceMap.remove(chatSessionToken);
                }
            }

            log.info("사용자 접속 해제 - chatSession: {}, user: {}, type: {}",
                    chatSessionToken, presence.getUserIdentifier(), presence.getSenderType());

            // 해당 채팅방에 접속 해제 브로드캐스트
            broadcastPresenceUpdate(chatSessionToken, PresenceEventType.DISCONNECTED, presence);
        } else {
            log.warn("접속 해제 시 사용자 정보 없음 - webSocketSessionId: {}", webSocketSessionId);
        }
    }

    /**
     * 특정 채팅방의 현재 접속자 목록 조회
     *
     * @param chatSessionToken 채팅 세션 토큰
     * @return 접속자 목록
     */
    public Set<UserPresence> getConnectedUsers(String chatSessionToken) {
        return new HashSet<>(sessionPresenceMap.getOrDefault(chatSessionToken, Collections.emptySet()));
    }

    /**
     * 특정 사용자의 접속 여부 확인
     *
     * @param chatSessionToken 채팅 세션 토큰
     * @param userIdentifier 사용자 식별자
     * @return 접속 여부
     */
    public boolean isUserConnected(String chatSessionToken, String userIdentifier) {
        Set<UserPresence> presences = sessionPresenceMap.get(chatSessionToken);
        if (presences == null) {
            return false;
        }

        return presences.stream()
                .anyMatch(p -> p.getUserIdentifier().equals(userIdentifier));
    }

    /**
     * 접속 상태 변경 브로드캐스트
     *
     * @param chatSessionToken 채팅 세션 토큰
     * @param eventType 이벤트 타입
     * @param presence 사용자 접속 정보
     */
    private void broadcastPresenceUpdate(String chatSessionToken, PresenceEventType eventType, UserPresence presence) {
        PresenceUpdate update = new PresenceUpdate(
                eventType,
                presence.getUserIdentifier(),
                presence.getSenderType(),
                presence.getDisplayName(),
                LocalDateTime.now(),
                getConnectedUsers(chatSessionToken).size()
        );

        String destination = "/topic/session/" + chatSessionToken + "/presence";
        messagingTemplate.convertAndSend(destination, update);

        log.debug("접속 상태 브로드캐스트 - destination: {}, event: {}, user: {}",
                destination, eventType, presence.getUserIdentifier());
    }

    /**
     * 사용자 접속 정보
     */
    @Getter
    public static class UserPresence {
        private final String webSocketSessionId;
        private final String chatSessionToken;
        private final String userIdentifier;  // 지원자 UUID 또는 채용담당자 sessionToken
        private final SenderType senderType;
        private final String displayName;
        private final LocalDateTime connectedAt;

        public UserPresence(String webSocketSessionId, String chatSessionToken, String userIdentifier,
                          SenderType senderType, String displayName, LocalDateTime connectedAt) {
            this.webSocketSessionId = webSocketSessionId;
            this.chatSessionToken = chatSessionToken;
            this.userIdentifier = userIdentifier;
            this.senderType = senderType;
            this.displayName = displayName;
            this.connectedAt = connectedAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserPresence that = (UserPresence) o;
            return Objects.equals(webSocketSessionId, that.webSocketSessionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(webSocketSessionId);
        }
    }

    /**
     * 접속 상태 업데이트 메시지
     */
    @Getter
    public static class PresenceUpdate {
        private final PresenceEventType eventType;
        private final String userIdentifier;
        private final SenderType senderType;
        private final String displayName;
        private final LocalDateTime timestamp;
        private final int totalConnected;

        public PresenceUpdate(PresenceEventType eventType, String userIdentifier, SenderType senderType,
                            String displayName, LocalDateTime timestamp, int totalConnected) {
            this.eventType = eventType;
            this.userIdentifier = userIdentifier;
            this.senderType = senderType;
            this.displayName = displayName;
            this.timestamp = timestamp;
            this.totalConnected = totalConnected;
        }
    }

    /**
     * 접속 이벤트 타입
     */
    public enum PresenceEventType {
        CONNECTED,      // 접속
        DISCONNECTED    // 접속 해제
    }
}
