# 백엔드 수정사항 체크리스트

> 프론트엔드에서 발견된 문제 중 백엔드 수정이 필요한 항목들입니다.

---

## 🔴 긴급 - 메시지 전송 시 WebSocket 브로드캐스트 누락

### 문제 상황
- 채용담당자 또는 지원자가 메시지 전송 시, 상대방 화면에 실시간으로 업데이트되지 않음
- 페이지를 새로고침해야 메시지가 보임
- WebSocket 연결은 정상이지만 메시지가 수신되지 않음

### 원인 분석 (추정)

현재 백엔드 구현이 다음과 같을 것으로 추정됩니다:

```java
@PostMapping("/chat/session/{sessionToken}/send")
public ResponseEntity<?> sendMessage(
    @PathVariable String sessionToken,
    @RequestBody SendMessageRequest request
) {
    // 1. 메시지를 DB에 저장
    ChatMessage message = chatService.saveMessage(sessionToken, request);

    // 2. ❌ WebSocket 브로드캐스트가 누락됨!
    // messagingTemplate.convertAndSend(...) 호출이 없음

    // 3. HTTP 응답만 반환
    return ResponseEntity.ok(message);
}
```

**문제점**:
- 메시지를 DB에만 저장하고 WebSocket을 통해 브로드캐스트하지 않음
- 프론트엔드는 WebSocket으로 메시지를 받아야 실시간 업데이트가 가능
- 현재는 REST API 응답만 받고, WebSocket 메시지는 받지 못함

### 해결 방법

#### 필수 수정: 메시지 전송 시 WebSocket 브로드캐스트 추가

**Controller 또는 Service에 다음 로직 추가**:

```java
@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository messageRepository;

    /**
     * 메시지 저장 및 브로드캐스트
     */
    public ChatMessage sendMessage(String sessionToken, SendMessageRequest request) {
        // 1. 메시지 저장
        ChatMessage message = ChatMessage.builder()
            .sessionToken(sessionToken)
            .message(request.getMessage())
            .senderType(request.getSenderType())
            .sentAt(Instant.now())
            .build();

        ChatMessage savedMessage = messageRepository.save(message);

        // 2. ✅ WebSocket으로 브로드캐스트 (필수!)
        messagingTemplate.convertAndSend(
            "/topic/chat/" + sessionToken,
            savedMessage
        );

        log.info("메시지 브로드캐스트 완료: sessionToken={}, messageId={}",
            sessionToken, savedMessage.getMessageId());

        return savedMessage;
    }
}
```

**또는 Controller에서 직접**:

```java
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @PostMapping("/chat/session/{sessionToken}/send")
    public ResponseEntity<SendMessageResponse> sendRecruiterMessage(
        @PathVariable String sessionToken,
        @RequestBody RecruiterSendMessageRequest request
    ) {
        // 1. 메시지 저장
        ChatMessage message = chatService.saveMessage(sessionToken, request);

        // 2. ✅ WebSocket 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/chat/" + sessionToken,
            message
        );

        // 3. HTTP 응답
        return ResponseEntity.ok(new SendMessageResponse(message.getMessageId()));
    }

    @PostMapping("/applicant/chat/{sessionToken}/send")
    public ResponseEntity<SendMessageResponse> sendApplicantMessage(
        @PathVariable String sessionToken,
        @RequestBody ApplicantSendMessageRequest request
    ) {
        // 1. 메시지 저장
        ChatMessage message = chatService.saveMessage(sessionToken, request);

        // 2. ✅ WebSocket 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/chat/" + sessionToken,
            message
        );

        // 3. HTTP 응답
        return ResponseEntity.ok(new SendMessageResponse(message.getMessageId()));
    }
}
```

### 체크리스트

**긴급 수정 필요** (메시지 실시간 전송 불가):
- [ ] RecruiterSendMessage 핸들러에 WebSocket 브로드캐스트 추가
  - [ ] 메시지 저장 후 `messagingTemplate.convertAndSend()` 호출
  - [ ] 목적지: `/topic/chat/{sessionToken}`
  - [ ] 페이로드: 저장된 ChatMessage 객체
- [ ] ApplicantSendMessage 핸들러에 WebSocket 브로드캐스트 추가
  - [ ] 동일한 브로드캐스트 로직 적용
- [ ] 브로드캐스트 로그 추가
  - [ ] 메시지 ID, sessionToken 로깅
  - [ ] 에러 발생 시 로깅

**테스트 필수**:
- [ ] 채용담당자 → 지원자 메시지 전송 시 지원자 화면에 즉시 표시
- [ ] 지원자 → 채용담당자 메시지 전송 시 채용담당자 화면에 즉시 표시
- [ ] 네트워크 탭에서 WebSocket 프레임 확인
  - [ ] `/topic/chat/{sessionToken}` 구독 확인
  - [ ] 메시지 전송 후 WebSocket MESSAGE 프레임 수신 확인
- [ ] 콘솔 로그 확인
  - [ ] `[WebSocket] 📨 Message received on /topic/chat/{sessionToken}` 로그 출력
  - [ ] `[useChatWebSocket] ✅ Parsed message:` 로그 출력

### 추가 권장사항

#### 1. 메시지 전송 실패 시 롤백 처리

```java
@Transactional
public ChatMessage sendMessage(String sessionToken, SendMessageRequest request) {
    try {
        // 메시지 저장
        ChatMessage message = messageRepository.save(...);

        // WebSocket 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chat/" + sessionToken, message);

        return message;
    } catch (Exception e) {
        log.error("메시지 전송 실패: sessionToken={}", sessionToken, e);
        throw new MessageSendException("메시지 전송에 실패했습니다", e);
    }
}
```

#### 2. 메시지 DTO 일관성 확인

WebSocket으로 브로드캐스트하는 메시지 객체가 프론트엔드의 `ChatMessage` 타입과 일치해야 합니다:

```typescript
// 프론트엔드 ChatMessage 타입 (추정)
interface ChatMessage {
  messageId: string;
  sessionToken: string;
  senderType: 'RECRUITER' | 'APPLICANT';
  message: string;
  sentAt: string;  // ISO-8601 format
}
```

백엔드에서 브로드캐스트할 때 이 형식과 일치하는지 확인:

```java
@Data
@Builder
public class ChatMessage {
    private String messageId;
    private String sessionToken;
    private SenderType senderType;  // RECRUITER or APPLICANT
    private String message;
    private Instant sentAt;  // Jackson이 ISO-8601로 변환
}
```

#### 3. @MessageMapping 방식도 고려 (선택)

현재는 REST API + 수동 브로드캐스트 방식인데, STOMP의 @MessageMapping을 사용할 수도 있습니다:

```java
@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    /**
     * 클라이언트가 /app/chat/{sessionToken} 으로 메시지 전송 시 처리
     */
    @MessageMapping("/chat/{sessionToken}")
    public void handleChatMessage(
        @DestinationVariable String sessionToken,
        @Payload SendMessageRequest request,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        log.info("WebSocket 메시지 수신: sessionToken={}", sessionToken);

        // 1. 메시지 저장
        ChatMessage message = chatService.saveMessage(sessionToken, request);

        // 2. 브로드캐스트 (같은 토픽 구독자 모두에게)
        messagingTemplate.convertAndSend(
            "/topic/chat/" + sessionToken,
            message
        );
    }
}
```

하지만 현재 프론트엔드가 REST API를 사용하므로, 일단 REST API에 브로드캐스트만 추가하는 것이 우선입니다.

---

## 🟡 권장 - 타이핑 인디케이터 WebSocket 처리

### 배경
프론트엔드에서 타이핑 인디케이터 기능이 구현되었습니다. 백엔드에서 타이핑 이벤트를 받아서 상대방에게 브로드캐스트해야 합니다.

### 프론트엔드 구현 내용
- 사용자가 입력할 때 `/app/chat/{sessionToken}/typing` 으로 이벤트 전송
- 페이로드: `{ senderType: "RECRUITER" | "APPLICANT", typing: true | false }`
- `/topic/chat/{sessionToken}/typing` 구독하여 상대방 타이핑 상태 수신
- 3초간 입력 없으면 자동으로 `typing: false` 전송

### 백엔드 구현 필요사항

#### @MessageMapping으로 타이핑 이벤트 처리

```java
@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 타이핑 이벤트 처리
     * - 클라이언트가 /app/chat/{sessionToken}/typing 으로 전송
     * - 같은 sessionToken을 구독 중인 모든 클라이언트에게 브로드캐스트
     */
    @MessageMapping("/chat/{sessionToken}/typing")
    public void handleTypingEvent(
        @DestinationVariable String sessionToken,
        @Payload TypingEventRequest request
    ) {
        log.debug("타이핑 이벤트 수신: sessionToken={}, senderType={}, typing={}",
            sessionToken, request.getSenderType(), request.isTyping());

        // 타이핑 이벤트를 같은 토픽 구독자에게 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/chat/" + sessionToken + "/typing",
            request
        );
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class TypingEventRequest {
    private String senderType;  // "RECRUITER" or "APPLICANT"
    private boolean typing;
}
```

### 체크리스트

- [ ] TypingEventRequest DTO 생성
  - [ ] senderType (String)
  - [ ] typing (boolean)
- [ ] @MessageMapping("/chat/{sessionToken}/typing") 핸들러 추가
  - [ ] 페이로드 파싱
  - [ ] 로깅 추가
- [ ] 타이핑 이벤트 브로드캐스트
  - [ ] 목적지: `/topic/chat/{sessionToken}/typing`
  - [ ] 페이로드: TypingEventRequest

### 테스트

1. 두 브라우저로 테스트
2. **브라우저 A**: 메시지 입력창에 타이핑
3. **브라우저 B**: "상대방이 입력 중..." 메시지와 애니메이션 확인
4. **브라우저 A**: 3초간 입력 중단
5. **브라우저 B**: 타이핑 인디케이터 사라지는지 확인

### 참고사항

- 타이핑 이벤트는 DB에 저장할 필요 없음 (휘발성 데이터)
- 브로드캐스트만 하면 됨
- 성능 영향을 최소화하기 위해 로깅은 debug 레벨 권장

---

## 🟡 권장 - WebSocket 세션 관리 개선

### 배경
프론트엔드에서 WebSocket 중복 연결 문제가 발견되었습니다. 프론트엔드 수정으로 대부분 해결 가능하지만, 백엔드에서도 방어적으로 처리하면 더 안정적입니다.

### 현재 상황 (추정)
- 같은 sessionToken으로 여러 개의 WebSocket 연결이 가능할 것으로 추정
- 세션당 활성 연결 수 제한이 없음
- 클라이언트가 비정상 종료 시 좀비 연결이 남을 수 있음

### 권장 수정사항

#### 1. 세션당 최대 연결 수 제한

**목적**: 같은 sessionToken으로 중복 연결 방지

**구현 방법**:
```java
@Component
public class WebSocketSessionManager {

    // sessionToken -> Set<WebSocketSession>
    private final Map<String, Set<String>> sessionTokenToConnectionIds = new ConcurrentHashMap<>();

    // sessionToken당 최대 연결 수
    private static final int MAX_CONNECTIONS_PER_SESSION = 2;  // 지원자 1명 + 채용담당자 1명

    /**
     * 새 연결 등록
     * @return true if allowed, false if limit exceeded
     */
    public boolean registerConnection(String sessionToken, String connectionId) {
        Set<String> connections = sessionTokenToConnectionIds
            .computeIfAbsent(sessionToken, k -> ConcurrentHashMap.newKeySet());

        if (connections.size() >= MAX_CONNECTIONS_PER_SESSION) {
            log.warn("세션당 최대 연결 수 초과: sessionToken={}, 현재 연결 수={}",
                sessionToken, connections.size());
            // 옵션 1: 거부
            return false;

            // 옵션 2: 가장 오래된 연결 강제 종료 (선택)
            // String oldestConnection = connections.iterator().next();
            // closeConnection(oldestConnection);
            // connections.remove(oldestConnection);
        }

        connections.add(connectionId);
        log.info("WebSocket 연결 등록: sessionToken={}, connectionId={}, 총 연결 수={}",
            sessionToken, connectionId, connections.size());
        return true;
    }

    /**
     * 연결 해제
     */
    public void unregisterConnection(String sessionToken, String connectionId) {
        Set<String> connections = sessionTokenToConnectionIds.get(sessionToken);
        if (connections != null) {
            connections.remove(connectionId);
            log.info("WebSocket 연결 해제: sessionToken={}, connectionId={}, 남은 연결 수={}",
                sessionToken, connectionId, connections.size());

            if (connections.isEmpty()) {
                sessionTokenToConnectionIds.remove(sessionToken);
            }
        }
    }

    /**
     * 세션의 모든 연결 조회
     */
    public Set<String> getConnections(String sessionToken) {
        return sessionTokenToConnectionIds.getOrDefault(sessionToken, Collections.emptySet());
    }

    /**
     * 좀비 연결 정리 (스케줄러)
     */
    @Scheduled(fixedRate = 300000) // 5분마다
    public void cleanupStaleConnections() {
        // 활성 연결이 아닌 경우 제거
        // (구현은 WebSocket 프레임워크에 따라 다름)
    }
}
```

**적용 위치** (추정):
- `WebSocketEventListener` 또는 `StompConnectEvent` 핸들러
- `StompDisconnectEvent` 핸들러

**체크리스트**:
- [ ] WebSocketSessionManager 클래스 생성
- [ ] registerConnection 메서드 구현
- [ ] unregisterConnection 메서드 구현
- [ ] STOMP 연결 이벤트에 등록 로직 추가
- [ ] STOMP 연결 해제 이벤트에 해제 로직 추가
- [ ] 최대 연결 수 초과 시 처리 정책 결정 (거부 vs 기존 연결 종료)
- [ ] 좀비 연결 정리 스케줄러 구현

#### 2. 중복 연결 감지 및 처리

**목적**: 같은 사용자가 여러 탭/브라우저에서 접속 시 처리

**옵션 A**: 새 연결 허용 + 이전 연결 종료 (권장)
```java
public boolean registerConnection(String sessionToken, String connectionId, String userType) {
    Set<ConnectionInfo> connections = sessionTokenToConnections
        .computeIfAbsent(sessionToken, k -> ConcurrentHashMap.newKeySet());

    // 같은 userType (RECRUITER or APPLICANT)의 기존 연결 찾기
    Optional<ConnectionInfo> existingConnection = connections.stream()
        .filter(conn -> conn.getUserType().equals(userType))
        .findFirst();

    if (existingConnection.isPresent()) {
        log.info("중복 연결 감지: sessionToken={}, userType={}, 기존 연결 종료",
            sessionToken, userType);

        // 기존 연결 종료
        ConnectionInfo oldConn = existingConnection.get();
        closeConnection(oldConn.getConnectionId());
        connections.remove(oldConn);
    }

    // 새 연결 등록
    connections.add(new ConnectionInfo(connectionId, userType, Instant.now()));
    return true;
}

@Data
@AllArgsConstructor
class ConnectionInfo {
    private String connectionId;
    private String userType;  // "RECRUITER" or "APPLICANT"
    private Instant connectedAt;
}
```

**옵션 B**: 새 연결 거부
```java
if (existingConnection.isPresent()) {
    log.warn("중복 연결 시도 차단: sessionToken={}, userType={}", sessionToken, userType);
    return false;  // 새 연결 거부
}
```

**체크리스트**:
- [ ] 중복 연결 감지 로직 구현
- [ ] 중복 연결 처리 정책 결정 (옵션 A 또는 B)
- [ ] userType 구분 로직 추가 (RECRUITER vs APPLICANT)
- [ ] 기존 연결 강제 종료 메서드 구현 (옵션 A 선택 시)
- [ ] 클라이언트에 연결 종료 알림 전송 (옵션 A 선택 시)

#### 3. WebSocket 연결 모니터링

**목적**: 운영 중 WebSocket 상태 파악

**구현 예시**:
```java
@RestController
@RequestMapping("/api/admin/websocket")
public class WebSocketMonitorController {

    @Autowired
    private WebSocketSessionManager sessionManager;

    /**
     * 전체 활성 연결 조회
     */
    @GetMapping("/connections")
    public Map<String, Object> getActiveConnections() {
        return Map.of(
            "totalSessions", sessionManager.getTotalSessions(),
            "totalConnections", sessionManager.getTotalConnections(),
            "sessions", sessionManager.getAllSessions()
        );
    }

    /**
     * 특정 세션의 연결 조회
     */
    @GetMapping("/connections/{sessionToken}")
    public Set<String> getSessionConnections(@PathVariable String sessionToken) {
        return sessionManager.getConnections(sessionToken);
    }

    /**
     * 특정 연결 강제 종료
     */
    @DeleteMapping("/connections/{connectionId}")
    public void closeConnection(@PathVariable String connectionId) {
        sessionManager.forceCloseConnection(connectionId);
    }
}
```

**체크리스트**:
- [ ] WebSocket 모니터링 API 엔드포인트 구현
- [ ] 활성 연결 수 조회 API
- [ ] 세션별 연결 조회 API
- [ ] 연결 강제 종료 API
- [ ] 관리자 권한 체크 추가

#### 4. 연결 타임아웃 및 하트비트 설정

**목적**: 좀비 연결 방지

**Spring WebSocket 설정**:
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
            .setHeartbeatValue(new long[]{10000, 10000});  // 10초마다 하트비트
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setSessionCookieNeeded(false)
            .setClientLibraryUrl("//cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
            .setDisconnectDelay(5000);  // 5초 후 연결 정리
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
            .setMessageSizeLimit(128 * 1024)     // 128KB
            .setSendBufferSizeLimit(512 * 1024)  // 512KB
            .setSendTimeLimit(20000)             // 20초
            .setTimeToFirstMessage(30000);       // 첫 메시지 30초 제한
    }
}
```

**체크리스트**:
- [ ] 하트비트 설정 확인/수정 (10초 권장)
- [ ] 연결 타임아웃 설정 확인
- [ ] 메시지 크기 제한 설정
- [ ] SockJS 설정 최적화

#### 5. 연결 이벤트 로깅 강화

**목적**: 디버깅 및 모니터링

**구현 예시**:
```java
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private WebSocketSessionManager sessionManager;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionToken = headerAccessor.getFirstNativeHeader("X-Session-Token");
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket 연결: sessionId={}, sessionToken={}, user={}",
            sessionId, sessionToken, headerAccessor.getUser());

        // 연결 등록
        boolean allowed = sessionManager.registerConnection(sessionToken, sessionId);
        if (!allowed) {
            log.warn("연결 거부: 세션당 최대 연결 수 초과");
            // 연결 종료 (구현 필요)
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionToken = headerAccessor.getFirstNativeHeader("X-Session-Token");
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket 연결 해제: sessionId={}, sessionToken={}, closeStatus={}",
            sessionId, sessionToken, event.getCloseStatus());

        // 연결 해제
        sessionManager.unregisterConnection(sessionToken, sessionId);
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket 구독: sessionId={}, destination={}", sessionId, destination);
    }

    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket 구독 해제: sessionId={}", sessionId);
    }
}
```

**체크리스트**:
- [ ] 연결 이벤트 로깅 추가
- [ ] 연결 해제 이벤트 로깅 추가
- [ ] 구독 이벤트 로깅 추가
- [ ] 에러 이벤트 로깅 추가
- [ ] sessionToken, userType, timestamp 등 주요 정보 로깅

---

## 📊 모니터링 및 알림

### 권장 모니터링 메트릭
- [ ] 활성 WebSocket 연결 수
- [ ] 세션당 평균 연결 수
- [ ] 연결/해제 빈도
- [ ] 중복 연결 시도 횟수
- [ ] 좀비 연결 정리 횟수
- [ ] 메시지 전송 실패 횟수

### 알림 설정
- [ ] 세션당 연결 수 임계값 초과 시 알림
- [ ] 전체 연결 수 급증 시 알림
- [ ] 연결 실패율 증가 시 알림

---

## 🧪 테스트 체크리스트

### 기능 테스트
- [ ] 정상 연결 시나리오
- [ ] 중복 연결 시나리오 (같은 sessionToken)
- [ ] 최대 연결 수 초과 시나리오
- [ ] 연결 종료 및 재연결 시나리오
- [ ] 좀비 연결 정리 시나리오

### 부하 테스트
- [ ] 동시 연결 1000개 테스트
- [ ] 세션당 연결 수 제한 테스트
- [ ] 메모리 사용량 모니터링
- [ ] CPU 사용량 모니터링

### 에러 시나리오 테스트
- [ ] 네트워크 중단 후 복구
- [ ] 서버 재시작 시나리오
- [ ] 클라이언트 비정상 종료
- [ ] 동시 연결/해제 요청

---

## 우선순위

### 🔴 높음 (즉시 적용 권장)
1. 연결 이벤트 로깅 강화 (진단 필수)
2. 세션당 최대 연결 수 제한
3. 중복 연결 감지 및 처리

### 🟡 중간 (점진적 적용)
4. WebSocket 모니터링 API
5. 하트비트 및 타임아웃 최적화

### 🟢 낮음 (선택적 적용)
6. 좀비 연결 정리 스케줄러
7. 모니터링 대시보드

---

**작성일**: 2026-03-12
**관련 문서**: [fix.md](./fix.md) (프론트엔드 수정사항)
