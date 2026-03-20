package com.cookyuu.resume_chat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub Subscriber
 *
 * <p>Redis 채널에서 메시지를 수신하여 WebSocket으로 브로드캐스트</p>
 *
 * <h3>처리 채널</h3>
 * <ul>
 *   <li>chat:{sessionToken} - 채팅 메시지</li>
 *   <li>chat:{sessionToken}:typing - 입력 중 이벤트</li>
 *   <li>chat:{sessionToken}:presence - 온라인 상태 변경</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Redis 메시지 수신 시 WebSocket으로 브로드캐스트
     *
     * @param message Redis 메시지
     * @param pattern 구독 패턴
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            log.debug("Redis 메시지 수신: channel={}", channel);

            // 채널에 따라 처리
            if (channel.endsWith(":typing")) {
                handleTypingEvent(channel, body);
            } else if (channel.endsWith(":presence")) {
                handlePresenceEvent(channel, body);
            } else {
                handleChatMessage(channel, body);
            }
        } catch (Exception e) {
            log.error("Redis 메시지 처리 실패", e);
        }
    }

    /**
     * 채팅 메시지 처리
     */
    private void handleChatMessage(String channel, String body) {
        // Redis 채널에서 sessionToken 추출
        String sessionToken = channel.replace("chat:", "");

        // WebSocket으로 브로드캐스트
        String destination = "/topic/session/" + sessionToken;
        messagingTemplate.convertAndSend(destination, body);

        log.debug("WebSocket 브로드캐스트: destination={}", destination);
    }

    /**
     * 입력 중 이벤트 처리
     */
    private void handleTypingEvent(String channel, String body) {
        String sessionToken = channel.replace("chat:", "").replace(":typing", "");
        String destination = "/topic/session/" + sessionToken + "/typing";
        messagingTemplate.convertAndSend(destination, body);

        log.debug("입력 중 이벤트 브로드캐스트: destination={}", destination);
    }

    /**
     * 온라인 상태 변경 이벤트 처리
     */
    private void handlePresenceEvent(String channel, String body) {
        String sessionToken = channel.replace("chat:", "").replace(":presence", "");
        String destination = "/topic/session/" + sessionToken + "/presence";
        messagingTemplate.convertAndSend(destination, body);

        log.debug("온라인 상태 변경 브로드캐스트: destination={}", destination);
    }
}
