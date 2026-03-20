package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.dto.RedisChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Pub/Sub Publisher
 *
 * <p>Redis 채널에 메시지를 발행하여 클러스터 환경의 모든 서버에 브로드캐스트</p>
 *
 * <h3>발행 채널</h3>
 * <ul>
 *   <li>chat:{sessionToken} - 채팅 메시지</li>
 *   <li>chat:{sessionToken}:typing - 입력 중 이벤트</li>
 *   <li>chat:{sessionToken}:presence - 온라인 상태 변경</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 메시지 브로드캐스트 (Redis Pub/Sub)
     *
     * @param sessionToken 세션 토큰
     * @param message Redis 메시지 DTO
     */
    public void publishMessage(String sessionToken, RedisChatMessage message) {
        String channel = "chat:" + sessionToken;
        redisTemplate.convertAndSend(channel, message);

        log.debug("Redis Pub: channel={}, messageId={}", channel, message.getMessageId());
    }

    /**
     * 입력 중 이벤트 브로드캐스트
     *
     * @param sessionToken 세션 토큰
     * @param event 입력 중 이벤트
     */
    public void publishTypingEvent(String sessionToken, Object event) {
        String channel = "chat:" + sessionToken + ":typing";
        redisTemplate.convertAndSend(channel, event);

        log.debug("Redis Pub (typing): channel={}", channel);
    }

    /**
     * 온라인 상태 브로드캐스트
     *
     * @param sessionToken 세션 토큰
     * @param event 온라인 상태 변경 이벤트
     */
    public void publishPresenceEvent(String sessionToken, Object event) {
        String channel = "chat:" + sessionToken + ":presence";
        redisTemplate.convertAndSend(channel, event);

        log.debug("Redis Pub (presence): channel={}", channel);
    }
}
