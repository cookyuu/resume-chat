package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.common.enums.MessageType;
import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.dto.RedisChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 기반 채팅 메시지 Repository
 *
 * <p>Redis를 사용하여 실시간 채팅 메시지 캐싱 및 관리</p>
 *
 * <h3>Redis 데이터 구조</h3>
 * <ul>
 *   <li>chat:message:{messageId} - Hash (메시지 상세)</li>
 *   <li>chat:messages:{sessionToken} - List (메시지 목록, 최신 100개)</li>
 *   <li>chat:unread:{sessionToken}:{receiverType} - Set (읽지 않은 메시지 ID)</li>
 *   <li>chat:online:{sessionToken} - Set (온라인 사용자)</li>
 *   <li>chat:typing:{sessionToken} - Hash (입력 중 상태)</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_CACHED_MESSAGES = 100;
    private static final long MESSAGE_TTL = 3600; // 1시간

    /**
     * 메시지 저장 (Redis)
     *
     * @param message Redis 메시지 DTO
     */
    public void saveMessage(RedisChatMessage message) {
        String messageKey = "chat:message:" + message.getMessageId();
        String messagesKey = "chat:messages:" + message.getSessionToken();

        // 메시지 상세 저장
        Map<String, Object> messageData = convertToMap(message);
        redisTemplate.opsForHash().putAll(messageKey, messageData);
        redisTemplate.expire(messageKey, MESSAGE_TTL, TimeUnit.SECONDS);

        // 메시지 목록에 추가 (최신 메시지가 앞으로)
        redisTemplate.opsForList().leftPush(messagesKey, message.getMessageId().toString());
        redisTemplate.opsForList().trim(messagesKey, 0, MAX_CACHED_MESSAGES - 1);

        log.debug("Redis에 메시지 저장: messageId={}, sessionToken={}",
            message.getMessageId(), message.getSessionToken());
    }

    /**
     * 세션의 최근 메시지 조회
     *
     * @param sessionToken 세션 토큰
     * @param limit 조회할 메시지 수
     * @return 메시지 목록 (최신순)
     */
    public List<RedisChatMessage> getRecentMessages(String sessionToken, int limit) {
        String messagesKey = "chat:messages:" + sessionToken;
        List<Object> messageIds = redisTemplate.opsForList().range(messagesKey, 0, limit - 1);

        if (messageIds == null || messageIds.isEmpty()) {
            log.debug("Redis 캐시 미스: sessionToken={}", sessionToken);
            return Collections.emptyList();
        }

        log.debug("Redis 캐시 히트: sessionToken={}, count={}", sessionToken, messageIds.size());

        return messageIds.stream()
                .map(id -> getMessage(UUID.fromString(id.toString())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 특정 메시지 조회
     *
     * @param messageId 메시지 ID
     * @return Redis 메시지 DTO (없으면 null)
     */
    public RedisChatMessage getMessage(UUID messageId) {
        String messageKey = "chat:message:" + messageId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(messageKey);

        if (data.isEmpty()) {
            return null;
        }

        return convertToMessage(data);
    }

    /**
     * 읽지 않은 메시지 수 조회
     *
     * @param sessionToken 세션 토큰
     * @param receiverType 수신자 타입
     * @return 읽지 않은 메시지 수
     */
    public long getUnreadCount(String sessionToken, SenderType receiverType) {
        String unreadKey = "chat:unread:" + sessionToken + ":" + receiverType;
        Long count = redisTemplate.opsForSet().size(unreadKey);
        return count != null ? count : 0;
    }

    /**
     * 메시지 읽음 처리
     *
     * @param sessionToken 세션 토큰
     * @param receiverType 수신자 타입
     * @param messageId 메시지 ID
     */
    public void markAsRead(String sessionToken, SenderType receiverType, UUID messageId) {
        String unreadKey = "chat:unread:" + sessionToken + ":" + receiverType;
        redisTemplate.opsForSet().remove(unreadKey, messageId.toString());

        log.debug("Redis 읽음 처리: sessionToken={}, receiverType={}, messageId={}",
            sessionToken, receiverType, messageId);
    }

    /**
     * 읽지 않은 메시지 추가
     *
     * @param sessionToken 세션 토큰
     * @param senderType 발신자 타입
     * @param messageId 메시지 ID
     */
    public void addUnreadMessage(String sessionToken, SenderType senderType, UUID messageId) {
        // 수신자 타입 결정 (발신자 반대)
        SenderType receiverType = senderType == SenderType.APPLICANT
                ? SenderType.RECRUITER
                : SenderType.APPLICANT;

        String unreadKey = "chat:unread:" + sessionToken + ":" + receiverType;
        redisTemplate.opsForSet().add(unreadKey, messageId.toString());

        log.debug("Redis 읽지 않은 메시지 추가: sessionToken={}, receiverType={}, messageId={}",
            sessionToken, receiverType, messageId);
    }

    /**
     * 온라인 상태 설정
     *
     * @param sessionToken 세션 토큰
     * @param userId 사용자 ID
     */
    public void setOnline(String sessionToken, String userId) {
        String onlineKey = "chat:online:" + sessionToken;
        redisTemplate.opsForSet().add(onlineKey, userId);
        redisTemplate.expire(onlineKey, 300, TimeUnit.SECONDS); // 5분 TTL

        log.debug("Redis 온라인 상태 설정: sessionToken={}, userId={}", sessionToken, userId);
    }

    /**
     * 오프라인 상태 설정
     *
     * @param sessionToken 세션 토큰
     * @param userId 사용자 ID
     */
    public void setOffline(String sessionToken, String userId) {
        String onlineKey = "chat:online:" + sessionToken;
        redisTemplate.opsForSet().remove(onlineKey, userId);

        log.debug("Redis 오프라인 상태 설정: sessionToken={}, userId={}", sessionToken, userId);
    }

    /**
     * 온라인 사용자 확인
     *
     * @param sessionToken 세션 토큰
     * @param userId 사용자 ID
     * @return 온라인 여부
     */
    public boolean isOnline(String sessionToken, String userId) {
        String onlineKey = "chat:online:" + sessionToken;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(onlineKey, userId));
    }

    /**
     * 입력 중 상태 설정
     *
     * @param sessionToken 세션 토큰
     * @param userId 사용자 ID
     * @param typing 입력 중 여부
     */
    public void setTyping(String sessionToken, String userId, boolean typing) {
        String typingKey = "chat:typing:" + sessionToken;

        if (typing) {
            redisTemplate.opsForHash().put(typingKey, userId, "true");
            redisTemplate.expire(typingKey, 5, TimeUnit.SECONDS); // 5초 TTL
        } else {
            redisTemplate.opsForHash().delete(typingKey, userId);
        }

        log.debug("Redis 입력 중 상태 설정: sessionToken={}, userId={}, typing={}",
            sessionToken, userId, typing);
    }

    /**
     * RedisChatMessage를 Redis Map으로 변환
     */
    private Map<String, Object> convertToMap(RedisChatMessage message) {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", message.getMessageId().toString());
        map.put("sessionToken", message.getSessionToken());
        map.put("senderType", message.getSenderType().name());
        map.put("messageType", message.getMessageType().name());
        map.put("content", message.getContent());
        map.put("createdAt", message.getCreatedAt().toString());
        map.put("readStatus", message.isReadStatus());
        return map;
    }

    /**
     * Redis Map을 RedisChatMessage로 변환
     */
    private RedisChatMessage convertToMessage(Map<Object, Object> data) {
        try {
            return RedisChatMessage.builder()
                    .messageId(UUID.fromString(data.get("messageId").toString()))
                    .sessionToken(data.get("sessionToken").toString())
                    .senderType(SenderType.valueOf(data.get("senderType").toString()))
                    .messageType(MessageType.valueOf(data.get("messageType").toString()))
                    .content(data.get("content").toString())
                    .createdAt(LocalDateTime.parse(data.get("createdAt").toString()))
                    .readStatus(Boolean.parseBoolean(data.get("readStatus").toString()))
                    .build();
        } catch (Exception e) {
            log.error("Redis 메시지 변환 실패: {}", data, e);
            return null;
        }
    }
}
