# Redis 기반 채팅 시스템 전환 계획

## 목표

현재 MySQL 기반 채팅 시스템을 Redis 기반으로 전환하여 실시간 성능을 극대화하고 WebSocket 메시지 브로드캐스트를 최적화합니다.

---

## 현재 구조 분석

### 현재 MySQL 기반 구조
```
ChatSession (MySQL) → ChatMessage (MySQL) → WebSocket 브로드캐스트
```

**문제점:**
- 메시지 전송 시마다 MySQL INSERT 쿼리 실행
- 실시간 메시지 조회 시 DB 부하
- WebSocket 브로드캐스트 전 DB 저장 대기 시간
- 대량 메시지 처리 시 성능 저하
- 읽음 상태 업데이트 빈번한 DB Write

---

## Redis 전환 목표 구조

### Phase 1: Hybrid 구조 (권장)
```
실시간 데이터 → Redis (Cache + Pub/Sub)
영구 데이터 → MySQL (Persistence)
```

**장점:**
- 실시간 성능 극대화
- 데이터 영속성 보장
- 점진적 전환 가능
- 롤백 용이

### Phase 2: Redis 중심 구조 (선택)
```
모든 채팅 데이터 → Redis
주기적 백업 → MySQL
```

**장점:**
- 최고 성능
- 단순한 아키텍처

**단점:**
- Redis 장애 시 데이터 손실 위험
- 메모리 관리 필요

---

## 구현 계획 (Phase 1 - Hybrid 권장)

### 1단계: Redis 설정 및 기본 구조

#### 1.1 Redis 설치 및 설정

**build.gradle**:
```gradle
dependencies {
    // Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-cache'

    // Redis Pub/Sub (WebSocket 클러스터링)
    implementation 'org.springframework.session:spring-session-data-redis'
}
```

**application.yml**:
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    lettuce:
      pool:
        max-active: 10
        max-idle: 10
        min-idle: 2
    timeout: 3000ms

  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10분
      cache-null-values: false
```

**RedisConfig.java**:
```java
@Configuration
@EnableCaching
@EnableRedisRepositories
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON 직렬화
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        serializer.setObjectMapper(mapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("chat:*"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(ChatRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
```

#### 1.2 Redis 데이터 구조 설계

**Key Naming Convention**:
```
chat:session:{sessionToken}                          → Hash (세션 정보)
chat:messages:{sessionToken}                         → List (메시지 목록, 최신 100개)
chat:message:{messageId}                             → Hash (메시지 상세)
chat:unread:{sessionToken}:{senderType}             → Set (읽지 않은 메시지 ID)
chat:online:{sessionToken}                          → Set (온라인 사용자)
chat:typing:{sessionToken}                          → Hash (입력 중 상태)
```

**Redis 데이터 예시**:
```redis
# 세션 정보
HSET chat:session:abc123 resumeSlug "uuid-123"
HSET chat:session:abc123 recruiterEmail "recruiter@example.com"
HSET chat:session:abc123 totalMessages 10

# 메시지 목록 (최신 100개만 캐싱)
LPUSH chat:messages:abc123 "msg-uuid-1"
LPUSH chat:messages:abc123 "msg-uuid-2"
LTRIM chat:messages:abc123 0 99  # 최신 100개만 유지

# 메시지 상세
HSET chat:message:msg-uuid-1 content "안녕하세요"
HSET chat:message:msg-uuid-1 senderType "RECRUITER"
HSET chat:message:msg-uuid-1 createdAt "2026-03-18T10:00:00"

# 읽지 않은 메시지
SADD chat:unread:abc123:APPLICANT "msg-uuid-1"
SADD chat:unread:abc123:APPLICANT "msg-uuid-2"

# 온라인 사용자
SADD chat:online:abc123 "applicant-uuid"
EXPIRE chat:online:abc123 300  # 5분 TTL

# 입력 중 상태
HSET chat:typing:abc123 "applicant-uuid" "true"
EXPIRE chat:typing:abc123 5  # 5초 TTL
```

---

### 2단계: Redis Repository 구현

#### 2.1 Redis 채팅 메시지 Repository

**RedisChatMessage.java** (Redis 전용 DTO):
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisChatMessage implements Serializable {
    private UUID messageId;
    private String sessionToken;
    private SenderType senderType;
    private MessageType messageType;
    private String content;
    private LocalDateTime createdAt;
    private boolean readStatus;

    // MySQL ChatMessage로 변환
    public ChatMessage toEntity(ChatSession session) {
        return ChatMessage.builder()
                .messageId(messageId)
                .session(session)
                .senderType(senderType)
                .messageType(messageType)
                .content(content)
                .readStatus(readStatus)
                .build();
    }

    // MySQL ChatMessage에서 생성
    public static RedisChatMessage from(ChatMessage message) {
        return RedisChatMessage.builder()
                .messageId(message.getMessageId())
                .sessionToken(message.getSession().getSessionToken())
                .senderType(message.getSenderType())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .readStatus(message.isReadStatus())
                .build();
    }
}
```

**ChatRedisRepository.java**:
```java
@Repository
@RequiredArgsConstructor
public class ChatRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_CACHED_MESSAGES = 100;
    private static final long MESSAGE_TTL = 3600; // 1시간

    /**
     * 메시지 저장 (Redis)
     */
    public void saveMessage(RedisChatMessage message) {
        String messageKey = "chat:message:" + message.getMessageId();
        String messagesKey = "chat:messages:" + message.getSessionToken();

        // 메시지 상세 저장
        redisTemplate.opsForHash().putAll(messageKey, convertToMap(message));
        redisTemplate.expire(messageKey, MESSAGE_TTL, TimeUnit.SECONDS);

        // 메시지 목록에 추가 (최신 메시지가 앞으로)
        redisTemplate.opsForList().leftPush(messagesKey, message.getMessageId().toString());
        redisTemplate.opsForList().trim(messagesKey, 0, MAX_CACHED_MESSAGES - 1);
    }

    /**
     * 세션의 최근 메시지 조회
     */
    public List<RedisChatMessage> getRecentMessages(String sessionToken, int limit) {
        String messagesKey = "chat:messages:" + sessionToken;
        List<Object> messageIds = redisTemplate.opsForList().range(messagesKey, 0, limit - 1);

        if (messageIds == null || messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        return messageIds.stream()
                .map(id -> getMessage(UUID.fromString(id.toString())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 특정 메시지 조회
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
     */
    public long getUnreadCount(String sessionToken, SenderType receiverType) {
        String unreadKey = "chat:unread:" + sessionToken + ":" + receiverType;
        Long count = redisTemplate.opsForSet().size(unreadKey);
        return count != null ? count : 0;
    }

    /**
     * 메시지 읽음 처리
     */
    public void markAsRead(String sessionToken, SenderType receiverType, UUID messageId) {
        String unreadKey = "chat:unread:" + sessionToken + ":" + receiverType;
        redisTemplate.opsForSet().remove(unreadKey, messageId.toString());
    }

    /**
     * 읽지 않은 메시지 추가
     */
    public void addUnreadMessage(String sessionToken, SenderType senderType, UUID messageId) {
        // 수신자 타입 결정 (발신자 반대)
        SenderType receiverType = senderType == SenderType.APPLICANT
                ? SenderType.RECRUITER
                : SenderType.APPLICANT;

        String unreadKey = "chat:unread:" + sessionToken + ":" + receiverType;
        redisTemplate.opsForSet().add(unreadKey, messageId.toString());
    }

    /**
     * 온라인 상태 설정
     */
    public void setOnline(String sessionToken, String userId) {
        String onlineKey = "chat:online:" + sessionToken;
        redisTemplate.opsForSet().add(onlineKey, userId);
        redisTemplate.expire(onlineKey, 300, TimeUnit.SECONDS); // 5분 TTL
    }

    /**
     * 오프라인 상태 설정
     */
    public void setOffline(String sessionToken, String userId) {
        String onlineKey = "chat:online:" + sessionToken;
        redisTemplate.opsForSet().remove(onlineKey, userId);
    }

    /**
     * 온라인 사용자 확인
     */
    public boolean isOnline(String sessionToken, String userId) {
        String onlineKey = "chat:online:" + sessionToken;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(onlineKey, userId));
    }

    /**
     * 입력 중 상태 설정
     */
    public void setTyping(String sessionToken, String userId, boolean typing) {
        String typingKey = "chat:typing:" + sessionToken;

        if (typing) {
            redisTemplate.opsForHash().put(typingKey, userId, "true");
            redisTemplate.expire(typingKey, 5, TimeUnit.SECONDS); // 5초 TTL
        } else {
            redisTemplate.opsForHash().delete(typingKey, userId);
        }
    }

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

    private RedisChatMessage convertToMessage(Map<Object, Object> data) {
        return RedisChatMessage.builder()
                .messageId(UUID.fromString(data.get("messageId").toString()))
                .sessionToken(data.get("sessionToken").toString())
                .senderType(SenderType.valueOf(data.get("senderType").toString()))
                .messageType(MessageType.valueOf(data.get("messageType").toString()))
                .content(data.get("content").toString())
                .createdAt(LocalDateTime.parse(data.get("createdAt").toString()))
                .readStatus(Boolean.parseBoolean(data.get("readStatus").toString()))
                .build();
    }
}
```

---

### 3단계: Redis Pub/Sub 메시지 브로드캐스트

#### 3.1 Redis Pub/Sub Publisher

**ChatRedisPublisher.java**:
```java
@Service
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 메시지 브로드캐스트 (Redis Pub/Sub)
     */
    public void publishMessage(String sessionToken, RedisChatMessage message) {
        String channel = "chat:" + sessionToken;
        redisTemplate.convertAndSend(channel, message);
    }

    /**
     * 입력 중 이벤트 브로드캐스트
     */
    public void publishTypingEvent(String sessionToken, ChatDto.TypingEvent event) {
        String channel = "chat:" + sessionToken + ":typing";
        redisTemplate.convertAndSend(channel, event);
    }

    /**
     * 온라인 상태 브로드캐스트
     */
    public void publishPresenceEvent(String sessionToken, PresenceUpdate event) {
        String channel = "chat:" + sessionToken + ":presence";
        redisTemplate.convertAndSend(channel, event);
    }
}
```

#### 3.2 Redis Pub/Sub Subscriber

**ChatRedisSubscriber.java**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Redis 메시지 수신 시 WebSocket으로 브로드캐스트
     */
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            log.debug("Received message from Redis: channel={}, body={}", channel, body);

            // 채널에 따라 처리
            if (channel.endsWith(":typing")) {
                handleTypingEvent(channel, body);
            } else if (channel.endsWith(":presence")) {
                handlePresenceEvent(channel, body);
            } else {
                handleChatMessage(channel, body);
            }
        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }

    private void handleChatMessage(String channel, String body) {
        // Redis 채널에서 sessionToken 추출
        String sessionToken = channel.replace("chat:", "");

        // WebSocket으로 브로드캐스트
        messagingTemplate.convertAndSend("/topic/session/" + sessionToken, body);
    }

    private void handleTypingEvent(String channel, String body) {
        String sessionToken = channel.replace("chat:", "").replace(":typing", "");
        messagingTemplate.convertAndSend("/topic/session/" + sessionToken + "/typing", body);
    }

    private void handlePresenceEvent(String channel, String body) {
        String sessionToken = channel.replace("chat:", "").replace(":presence", "");
        messagingTemplate.convertAndSend("/topic/session/" + sessionToken + "/presence", body);
    }
}
```

---

### 4단계: ChatService 수정 (Hybrid 모드)

#### 4.1 메시지 전송 플로우

**수정된 ChatService.java**:
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;  // MySQL
    private final ChatRedisRepository chatRedisRepository;      // Redis
    private final ChatRedisPublisher chatRedisPublisher;        // Redis Pub/Sub

    /**
     * 메시지 전송 (Hybrid 방식)
     * 1. Redis에 즉시 저장 및 브로드캐스트
     * 2. MySQL에 비동기 저장 (영속성)
     */
    public ChatDto.ApplicantSendMessageResponse sendMessageByApplicant(
            UUID applicantUuid,
            String sessionToken,
            ChatDto.ApplicantSendMessageRequest request) {

        // 1. 세션 조회 및 권한 확인
        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 2. 메시지 생성
        ChatMessage message = ChatMessage.createMessage(
                session,
                SenderType.APPLICANT,
                request.getMessage()
        );

        // 3. Redis에 즉시 저장
        RedisChatMessage redisMessage = RedisChatMessage.from(message);
        chatRedisRepository.saveMessage(redisMessage);
        chatRedisRepository.addUnreadMessage(sessionToken, SenderType.APPLICANT, message.getMessageId());

        // 4. Redis Pub/Sub으로 즉시 브로드캐스트 (클러스터링 지원)
        chatRedisPublisher.publishMessage(sessionToken, redisMessage);

        // 5. MySQL에 비동기 저장 (영속성 보장)
        CompletableFuture.runAsync(() -> {
            chatMessageRepository.save(message);
            session.incrementMessageCount();
        });

        // 6. 이메일 알림 (비동기)
        emailService.scheduleNewMessageNotification(
                session.getResume().getApplicant().getEmail(),
                session.getRecruiterName(),
                session.getSessionToken()
        );

        return ChatDto.ApplicantSendMessageResponse.from(session, message);
    }

    /**
     * 메시지 조회 (Cache-Aside 패턴)
     * 1. Redis에서 최근 100개 조회
     * 2. 캐시 미스 시 MySQL 조회 후 Redis 캐싱
     */
    public ChatDto.ChatDetailResponse getSessionMessages(
            UUID applicantUuid,
            String sessionToken) {

        // 1. 세션 조회 및 권한 확인
        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 2. Redis에서 최근 메시지 조회
        List<RedisChatMessage> redisMessages = chatRedisRepository.getRecentMessages(sessionToken, 100);

        List<ChatMessage> messages;

        if (!redisMessages.isEmpty()) {
            // 2-1. Redis 캐시 히트
            messages = redisMessages.stream()
                    .map(rm -> rm.toEntity(session))
                    .collect(Collectors.toList());
        } else {
            // 2-2. Redis 캐시 미스 → MySQL 조회
            messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);

            // Redis에 캐싱 (비동기)
            CompletableFuture.runAsync(() -> {
                messages.stream()
                        .map(RedisChatMessage::from)
                        .forEach(chatRedisRepository::saveMessage);
            });
        }

        // 3. 읽음 처리 (Redis + MySQL)
        messages.stream()
                .filter(msg -> msg.getSenderType() == SenderType.RECRUITER && !msg.isReadStatus())
                .forEach(msg -> {
                    // Redis 읽음 처리
                    chatRedisRepository.markAsRead(sessionToken, SenderType.APPLICANT, msg.getMessageId());

                    // MySQL 읽음 처리 (비동기)
                    CompletableFuture.runAsync(() -> msg.markAsRead());
                });

        // 4. 읽지 않은 메시지 수 (Redis에서 조회)
        long unreadCount = chatRedisRepository.getUnreadCount(sessionToken, SenderType.APPLICANT);

        return ChatDto.ChatDetailResponse.of(session, messages, unreadCount);
    }
}
```

---

### 5단계: WebSocket 연결 시 Redis 활용

#### 5.1 WebSocket 이벤트 리스너

**수정된 WebSocketEventListener.java**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final ChatRedisRepository chatRedisRepository;
    private final ChatRedisPublisher chatRedisPublisher;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionToken = headerAccessor.getFirstNativeHeader("X-Chat-Session-Token");
        String userId = getUserIdFromPrincipal(headerAccessor.getUser());

        if (sessionToken != null && userId != null) {
            // Redis에 온라인 상태 저장
            chatRedisRepository.setOnline(sessionToken, userId);

            // Presence 이벤트 브로드캐스트
            PresenceUpdate presenceUpdate = PresenceUpdate.builder()
                    .eventType(PresenceEventType.CONNECTED)
                    .userIdentifier(userId)
                    .timestamp(LocalDateTime.now())
                    .build();

            chatRedisPublisher.publishPresenceEvent(sessionToken, presenceUpdate);

            log.info("User connected: sessionToken={}, userId={}", sessionToken, userId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionToken = headerAccessor.getFirstNativeHeader("X-Chat-Session-Token");
        String userId = getUserIdFromPrincipal(headerAccessor.getUser());

        if (sessionToken != null && userId != null) {
            // Redis에서 온라인 상태 제거
            chatRedisRepository.setOffline(sessionToken, userId);

            // Presence 이벤트 브로드캐스트
            PresenceUpdate presenceUpdate = PresenceUpdate.builder()
                    .eventType(PresenceEventType.DISCONNECTED)
                    .userIdentifier(userId)
                    .timestamp(LocalDateTime.now())
                    .build();

            chatRedisPublisher.publishPresenceEvent(sessionToken, presenceUpdate);

            log.info("User disconnected: sessionToken={}, userId={}", sessionToken, userId);
        }
    }

    private String getUserIdFromPrincipal(Principal principal) {
        if (principal == null) return null;
        return principal.getName();
    }
}
```

---

### 6단계: 입력 중 표시 (Typing Indicator)

#### 6.1 WebSocket Controller 수정

**수정된 ChatWebSocketController.java**:
```java
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatRedisRepository chatRedisRepository;
    private final ChatRedisPublisher chatRedisPublisher;

    /**
     * 입력 중 이벤트 처리
     */
    @MessageMapping("/chat/{sessionToken}/typing")
    public void handleTyping(
            @DestinationVariable String sessionToken,
            ChatDto.TypingEvent event,
            Principal principal) {

        String userId = principal.getName();

        // Redis에 입력 중 상태 저장
        chatRedisRepository.setTyping(sessionToken, userId, event.getTyping());

        // Redis Pub/Sub으로 브로드캐스트
        chatRedisPublisher.publishTypingEvent(sessionToken, event);
    }
}
```

---

## 마이그레이션 전략

### 1단계: Redis 인프라 구축
- [ ] Redis 서버 설치 및 설정
- [ ] Redis 클러스터 구성 (프로덕션 환경)
- [ ] Redis 모니터링 설정

### 2단계: 코드 구현
- [ ] build.gradle에 Redis 의존성 추가
- [ ] RedisConfig 생성
- [ ] RedisChatMessage DTO 생성
- [ ] ChatRedisRepository 생성
- [ ] ChatRedisPublisher/Subscriber 생성

### 3단계: ChatService Hybrid 모드 전환
- [ ] sendMessage() 메서드 수정 (Redis + MySQL)
- [ ] getMessages() 메서드 수정 (Cache-Aside)
- [ ] 읽음 처리 로직 수정 (Redis + MySQL)

### 4단계: WebSocket 통합
- [ ] WebSocket 이벤트 리스너 수정
- [ ] Typing Indicator Redis 연동
- [ ] Presence 상태 Redis 연동

### 5단계: 테스트
- [ ] 단위 테스트 (Redis Repository)
- [ ] 통합 테스트 (ChatService Hybrid)
- [ ] 성능 테스트 (부하 테스트)
- [ ] 장애 테스트 (Redis 다운 시나리오)

### 6단계: 배포
- [ ] 스테이징 환경 배포
- [ ] 성능 모니터링
- [ ] 프로덕션 배포
- [ ] 롤백 계획 준비

---

## 성능 개선 예상 효과

### Before (MySQL Only)
```
메시지 전송: ~50ms (DB INSERT + WebSocket)
메시지 조회: ~30ms (DB SELECT)
읽음 처리: ~20ms (DB UPDATE)
동시 접속: 100명 (DB 병목)
```

### After (Redis + MySQL Hybrid)
```
메시지 전송: ~5ms (Redis + Pub/Sub, MySQL 비동기)
메시지 조회: ~2ms (Redis 캐시 히트)
읽음 처리: ~1ms (Redis SET)
동시 접속: 10,000명+ (Redis 처리)
```

**개선율**: **10배 ~ 50배 성능 향상**

---

## Redis 메모리 관리

### 메모리 사용량 예측
```
메시지 1개: ~1KB
세션 100개, 각 100개 메시지 = 10MB
세션 1000개 = 100MB
세션 10000개 = 1GB
```

### TTL 정책
- 메시지: 1시간 (오래된 메시지는 MySQL 조회)
- 온라인 상태: 5분
- 입력 중 상태: 5초
- 읽지 않은 메시지: 무제한 (중요 데이터)

### 메모리 절약 전략
- 최근 100개 메시지만 캐싱
- LRU eviction policy 설정
- 주기적 Cleanup Job

---

## 장애 대응

### Redis 장애 시나리오

**Fallback to MySQL**:
```java
public List<RedisChatMessage> getRecentMessages(String sessionToken, int limit) {
    try {
        return chatRedisRepository.getRecentMessages(sessionToken, limit);
    } catch (RedisConnectionFailureException e) {
        log.warn("Redis connection failed, fallback to MySQL");
        // MySQL에서 조회
        return chatMessageRepository.findTop100BySessionOrderByCreatedAtDesc(session)
                .stream()
                .map(RedisChatMessage::from)
                .collect(Collectors.toList());
    }
}
```

**Circuit Breaker 적용** (Resilience4j):
```java
@CircuitBreaker(name = "redis", fallbackMethod = "fallbackToMySQL")
public List<RedisChatMessage> getRecentMessages(String sessionToken, int limit) {
    return chatRedisRepository.getRecentMessages(sessionToken, limit);
}
```

---

## 체크리스트

### Phase 1: Hybrid 구조 (권장) ✅
- [ ] Redis 서버 설치
- [ ] Redis 의존성 추가
- [ ] RedisConfig 생성
- [ ] ChatRedisRepository 구현
- [ ] ChatRedisPublisher/Subscriber 구현
- [ ] ChatService Hybrid 모드 전환
- [ ] WebSocket Redis 통합
- [ ] 테스트 작성
- [ ] 성능 측정
- [ ] 배포

### Phase 2: 모니터링 및 최적화
- [ ] Redis 모니터링 대시보드
- [ ] 메모리 사용량 추적
- [ ] Hit/Miss Rate 측정
- [ ] 성능 튜닝

---

**문서 버전**: 1.0
**작성일**: 2026-03-18
**작성자**: Resume Chat Team
