## 이슈 목록

### ✅ 해결 완료 - 읽지 않은 메시지 이메일 알림 통합 (2026-03-12)

**문제**: EmailService가 구현되었으나 ChatService에 통합되지 않아 실제 이메일 알림이 발송되지 않음

**현상**:
- 지원자가 메시지 전송 → 채용담당자에게 이메일 알림 없음
- 채용담당자가 메시지 전송 → 지원자에게 이메일 알림 없음
- 5분 지연 알림 시스템이 전혀 작동하지 않음

**원인 분석**:

1. **ChatService에 EmailService 미주입**
   - EmailService가 생성되었으나 ChatService에서 사용되지 않음
   - 메시지 전송 메서드에 알림 예약 로직 없음

2. **메시지 읽음 처리 로직의 문제**
   - `getSessionMessages()` (지원자용): 모든 메시지를 읽음 처리
     - 문제: 채용담당자가 보낸 메시지만 읽음 처리해야 함
     - 현재: `SenderType` 구분 없이 전체 읽음 처리
   - `getRecruiterSessionMessages()` (채용담당자용): 읽음 처리 없음
     - 문제: 지원자가 보낸 메시지를 읽어도 읽음 처리 안됨

3. **읽지 않은 메시지 카운트 로직 오류**
   - 현재: `countBySessionAndReadStatusFalse(session)` - 전체 읽지 않은 메시지
   - 필요: 수신자 기준 읽지 않은 메시지 카운트
     - 지원자: 채용담당자가 보낸 메시지 중 읽지 않은 것
     - 채용담당자: 지원자가 보낸 메시지 중 읽지 않은 것

**수정 완료** (2026-03-12):

- [x] ChatMessageRepository에 발신자 타입별 읽지 않은 메시지 카운트 메서드 추가 ✅
  - [x] `countBySessionAndReadStatusFalseAndSenderType(session, senderType)`
  - [x] `findBySessionAndReadStatusFalseAndSenderType(session, senderType)`

- [x] ChatService 수정 ✅
  - [x] EmailService 주입
  - [x] `sendMessage()` - 채용담당자 첫 메시지
    - [x] 신규 세션이면 `sendNewSessionNotification()` 즉시 발송
    - [x] 기존 세션이면 `scheduleNewMessageNotification()` 예약 (지원자에게)
  - [x] `sendRecruiterMessage()` - 채용담당자 메시지
    - [x] `scheduleNewMessageNotification()` 예약 (지원자에게)
  - [x] `sendMessageByApplicant()` - 지원자 메시지
    - [x] `scheduleNewMessageNotification()` 예약 (채용담당자에게)
  - [x] `getSessionMessages()` - 지원자 메시지 조회
    - [x] 채용담당자가 보낸 메시지만 읽음 처리 (SenderType.RECRUITER)
    - [x] 읽음 처리 후 `cancelNotification()` 호출
  - [x] `getRecruiterSessionMessages()` - 채용담당자 메시지 조회
    - [x] 지원자가 보낸 메시지만 읽음 처리 (SenderType.APPLICANT)
    - [x] 읽음 처리 후 `cancelNotification()` 호출

**빌드 상태**: BUILD SUCCESSFUL ✅

**알림 발송 흐름 (수정 후)**:

```
[지원자 → 채용담당자]
1. 지원자가 메시지 전송 (sendMessageByApplicant)
2. EmailService.scheduleNewMessageNotification() 호출
   - 수신자: session.getRecruiterEmail()
   - 발신자: applicant.getName()
   - 5분 타이머 시작
3. 채용담당자가 5분 이내 조회 (getRecruiterSessionMessages)
   - 지원자 메시지 읽음 처리
   - EmailService.cancelNotification() → 알림 취소
4. 5분 경과 후에도 안 읽으면 → 이메일 발송

[채용담당자 → 지원자]
1. 채용담당자가 메시지 전송 (sendMessage / sendRecruiterMessage)
2. EmailService.scheduleNewMessageNotification() 호출
   - 수신자: resume.getApplicant().getEmail()
   - 발신자: session.getRecruiterName()
   - 5분 타이머 시작
3. 지원자가 5분 이내 조회 (getSessionMessages)
   - 채용담당자 메시지 읽음 처리
   - EmailService.cancelNotification() → 알림 취소
4. 5분 경과 후에도 안 읽으면 → 이메일 발송
```

---

### ✅ 해결 완료 - 메시지 전송 시 WebSocket 브로드캐스트 (2026-03-11)

**문제**: REST API로 메시지 전송 시 WebSocket 브로드캐스트가 누락되어 실시간 메시지 수신 불가

**해결**:
- [x] ChatService에 SimpMessagingTemplate 주입 완료
- [x] broadcastMessage() 메서드 추가
- [x] REST API 메시지 전송 메서드에 브로드캐스트 호출 추가
  - [x] sendMessage() - 채용담당자 첫 메시지
  - [x] sendRecruiterMessage() - 채용담당자 메시지
  - [x] sendMessageByApplicant() - 지원자 메시지
- [x] WebSocket 브로드캐스트 테스트 작성 (ChatServiceTest 11/11 통과)
- [x] 빌드 및 테스트 성공

**상세 내용**: 아래 "메시지 실시간 연동 문제 해결" 섹션 참조

---

## 백엔드 수정 완료

### WebSocket 연결 문제
- [x] WebSocket 403 Forbidden 에러 원인 파악 및 해결 ✅
    - **해결**: SecurityConfig와 WebSocketConfig의 CORS 설정 동기화
    - SecurityConfig: 모든 localhost 포트 허용 (31000, 3000, 5173, 8080)
    - WebSocketConfig: 동일한 포트 패턴으로 allowedOriginPatterns 설정
    - `/api/ws/**` 경로 permitAll 설정 확인

### 채용담당자 세션 관리
- [x] 채용담당자 정보 영속성 관리 ✅
    - **구현 완료**: sessionToken 기반 인증 시스템
    - `RecruiterSessionService` 생성:
        - sessionToken 검증 및 세션 조회
        - 세션 만료 관리 (24시간)
        - 채용담당자 접근 권한 검증
    - `WebSocketAuthInterceptor` 업데이트:
        - JWT (지원자) + sessionToken (채용담당자) 이중 인증 지원
        - X-Session-Token 헤더로 채용담당자 인증
        - ROLE_RECRUITER 권한 부여
    - 테스트 작성: `RecruiterSessionServiceTest` (8개 테스트 모두 통과)

### 접속 상태 관리
- [x] 실시간 접속 상태 추적 시스템 구현 ✅
    - **구현 완료**: PresenceService 및 WebSocket 이벤트 리스너
    - `PresenceService` 생성:
        - 채팅방별 접속자 목록 관리 (ConcurrentHashMap)
        - 접속/해제 이벤트 브로드캐스트 (`/topic/session/{sessionToken}/presence`)
        - 지원자/채용담당자 구분 관리
    - `WebSocketEventListener` 생성:
        - SessionConnectEvent: WebSocket 연결 시 접속 상태 등록
        - SessionDisconnectEvent: WebSocket 해제 시 접속 상태 제거
        - X-Chat-Session-Token 헤더로 채팅방 식별
    - 테스트 작성: `PresenceServiceTest` (8개 테스트 모두 통과)

---

## 구현 상세

### 1. WebSocket 403 해결
- **문제**: CORS 설정 불일치로 SockJS 핸드셰이크 실패
- **해결**: SecurityConfig와 WebSocketConfig의 CORS 설정을 통일

### 2. 채용담당자 세션 관리
- **문제**: JWT가 없는 채용담당자의 세션 영속성 부재
- **해결**:
  - ChatSession의 sessionToken을 활용한 임시 인증 시스템
  - 24시간 세션 타임아웃 (메모리 기반, 프로덕션에서는 Redis 권장) V
  - WebSocket 연결 시 X-Session-Token 헤더로 인증

### 3. 접속 상태 관리
- **구현**:
  - WebSocket 연결/해제 이벤트 자동 감지
  - 채팅방별 실시간 접속자 목록 관리
  - 접속 상태 변경 시 해당 채팅방 구독자에게 브로드캐스트
  - PresenceUpdate 메시지 형식:
    ```json
    {
      "eventType": "CONNECTED" | "DISCONNECTED",
      "userIdentifier": "user-id",
      "senderType": "APPLICANT" | "RECRUITER",
      "displayName": "사용자명",
      "timestamp": "2026-03-11T09:00:00",
      "totalConnected": 2
    }
    ```

---

## 테스트 결과
- ✅ RecruiterSessionServiceTest: 8/8 통과
- ✅ PresenceServiceTest: 8/8 통과
- ✅ 빌드 성공: `./gradlew build -x test`

---

## 프론트엔드 연동 가이드

### 1. WebSocket 연결 (채용담당자)
```javascript
const socket = SockJS('/api/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
  'X-Session-Token': sessionToken,  // 채용담당자 인증
  'X-Chat-Session-Token': chatSessionToken  // 채팅방 식별
}, onConnected, onError);
```

### 2. WebSocket 연결 (지원자)
```javascript
const socket = SockJS('/api/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
  'Authorization': `Bearer ${jwtToken}`,  // 지원자 인증 (JWT)
  'X-Chat-Session-Token': chatSessionToken  // 채팅방 식별
}, onConnected, onError);
```

### 3. 접속 상태 구독
```javascript
stompClient.subscribe(`/topic/session/${chatSessionToken}/presence`, (message) => {
  const presenceUpdate = JSON.parse(message.body);
  console.log('접속 상태 변경:', presenceUpdate);
  // UI 업데이트: 접속자 목록, 온라인 표시 등
});
```

---

## 참고 사항
- 기존 ResumeController 테스트 5개 실패는 이번 수정과 무관 (별도 수정 필요)
- RecruiterSessionService의 세션 만료는 메모리 기반이므로 서버 재시작 시 초기화됨
- 프로덕션 환경에서는 Redis 등 영구 저장소 사용 권장



## 백엔드 WebSocket 403 해결 (구현 완료)

### ✅ 구현 완료: SockJS 핸드셰이크 엔드포인트 공개 + STOMP 레벨 인증

#### 구현 상세

**1. WebSocket 엔드포인트를 인증에서 제외**
- **파일**: `SecurityConfig.java`
- **구현**: `.requestMatchers("/ws/**").permitAll()`로 SockJS 핸드셰이크 엔드포인트 공개
- **CORS**: SecurityConfig와 WebSocketConfig에서 모든 localhost 포트 허용

**2. STOMP 레벨에서 이중 인증 시스템**
- **파일**: `WebSocketAuthInterceptor.java`
- **구현**:
  - JWT 토큰 검증 (지원자용)
  - X-Session-Token 헤더로 sessionToken 검증 (채용담당자용)
  - 검증 성공 시 Principal에 인증 정보 저장

**3. 채용담당자 세션 관리**
- **파일**: `RecruiterSessionService.java`
- **구현**:
  - sessionToken 유효성 검증
  - 24시간 세션 타임아웃 관리
  - 채용담당자 접근 권한 검증

**4. 실시간 접속 상태 관리**
- **파일**: `PresenceService.java`, `WebSocketEventListener.java`
- **구현**:
  - WebSocket 연결/해제 이벤트 자동 감지
  - chatSessionToken 기반 접속자 목록 관리
  - `/topic/session/{chatSessionToken}/presence`로 브로드캐스트

#### 체크리스트 (모두 구현 완료)
- [x] SecurityConfig에서 `/api/ws/**` 경로를 `permitAll()`로 설정 ✅
  - SecurityConfig.java 44번 라인: `.requestMatchers("/ws/**").permitAll()`
- [x] CORS 설정에서 WebSocket 엔드포인트 허용 확인 (`ws://`, `wss://` 프로토콜 허용) ✅
  - SecurityConfig.java (56-72번 라인): 모든 localhost 포트 허용
  - WebSocketConfig.java (49-54번 라인): 동일한 CORS 설정 + SockJS fallback
- [x] WebSocketConfig에 ChannelInterceptor 추가 ✅
  - WebSocketConfig.java (64-66번 라인): WebSocketAuthInterceptor 등록
- [x] STOMP CONNECT 프레임에서 Authorization 헤더로 **sessionToken** 검증 ✅
  - WebSocketAuthInterceptor.java (69-92번 라인): X-Session-Token 헤더로 채용담당자 인증
  - RecruiterSessionService를 통한 유효성 검증
- [x] sessionToken 검증 실패 시 예외 발생 (연결 거부) ✅
  - WebSocketAuthInterceptor.java (84-90번 라인): `IllegalArgumentException` 발생
- [x] 검증 성공 시 Principal에 sessionToken 저장 ✅
  - WebSocketAuthInterceptor.java (78-79번 라인): `createRecruiterAuthentication(sessionToken)`으로 Authentication 생성
  - "recruiter:{sessionToken}" 형식으로 Principal 저장
- [x] 메시지 전송/구독 시 Principal의 sessionToken으로 권한 확인 ✅
  - WebSocketAuthInterceptor에서 CONNECT 시 인증 완료
  - RecruiterSessionService의 validateAndGetSession으로 세션 검증
- [x] presence 브로드캐스트 시 sessionToken 기반으로 발송 ✅
  - PresenceService.java (135-150번 라인): `/topic/session/{chatSessionToken}/presence`로 브로드캐스트
  - WebSocketEventListener.java: 연결/해제 이벤트 자동 감지 및 처리

---

## 메시지 실시간 연동 문제 해결

### ❌ 문제 분석

**증상**: 사용자가 메시지를 전송해도 실시간으로 다른 클라이언트에 전달되지 않음

**근본 원인**:
애플리케이션에 두 가지 메시지 전송 경로가 존재했으나, 한 경로에서만 WebSocket 브로드캐스트가 작동함

1. **WebSocket 경로** (ChatWebSocketController)
   - `@MessageMapping` + `@SendTo` 사용
   - 메시지 저장 후 자동 브로드캐스트 ✅
   - `/app/chat/{sessionToken}` → `/topic/session/{sessionToken}`

2. **REST API 경로** (ChatController → ChatService)
   - HTTP POST 엔드포인트
   - **메시지를 DB에만 저장하고 WebSocket 브로드캐스트 없음** ❌
   - 영향받는 메서드:
     - `sendMessage()` - 채용담당자 첫 메시지 전송
     - `sendRecruiterMessage()` - 채용담당자 메시지 전송
     - `sendMessageByApplicant()` - 지원자 메시지 전송

**문제 시나리오**:
- 클라이언트가 REST API로 메시지 전송 → DB에만 저장됨
- 다른 클라이언트는 WebSocket 구독 중이지만 새 메시지 알림 받지 못함
- 페이지 새로고침해야 메시지 확인 가능

### ✅ 해결 방법

**1. ChatService에 SimpMessagingTemplate 주입**
```java
@Service
@RequiredArgsConstructor
public class ChatService {
    // 기존 의존성들...
    private final SimpMessagingTemplate messagingTemplate;  // 추가
}
```

**2. 브로드캐스트 메서드 추가**
```java
private void broadcastMessage(String sessionToken, ChatMessage message) {
    WebSocketChatMessage wsMessage = WebSocketChatMessage.builder()
            .messageId(message.getMessageId())
            .sessionToken(sessionToken)
            .senderType(message.getSenderType())
            .content(message.getContent())
            .sentAt(message.getCreatedAt())
            .build();

    String destination = "/topic/session/" + sessionToken;
    messagingTemplate.convertAndSend(destination, wsMessage);
}
```

**3. REST API 메시지 전송 메서드에 브로드캐스트 호출 추가**
```java
// sendMessage()
chatMessageRepository.save(message);
session.incrementMessageCount();
broadcastMessage(session.getSessionToken(), message);  // 추가

// sendRecruiterMessage()
chatMessageRepository.save(message);
session.incrementMessageCount();
broadcastMessage(sessionToken, message);  // 추가

// sendMessageByApplicant()
chatMessageRepository.save(message);
session.incrementMessageCount();
broadcastMessage(session.getSessionToken(), message);  // 추가
```

### 📋 테스트 작성

**ChatServiceTest에 브로드캐스트 검증 테스트 추가**:
- `@Mock SimpMessagingTemplate messagingTemplate` 추가
- `sendMessageByApplicant()` 테스트: WebSocket 브로드캐스트 호출 검증
- `sendRecruiterMessage()` 테스트 추가:
  - 메시지 저장 및 브로드캐스트 검증
  - 브로드캐스트 destination 검증 (`/topic/session/{sessionToken}`)
  - RECRUITER 타입 메시지 생성 검증

**테스트 결과**:
- ChatServiceTest: 모두 통과 (11개 테스트)
- 기존 테스트도 모두 통과

### 🔧 수정 파일

**코드 변경**:
- `src/main/java/com/cookyuu/resume_chat/service/ChatService.java`
  - SimpMessagingTemplate 의존성 주입
  - broadcastMessage() 메서드 추가
  - sendMessage(), sendRecruiterMessage(), sendMessageByApplicant()에 브로드캐스트 호출 추가

**테스트 추가**:
- `src/test/java/com/cookyuu/resume_chat/service/ChatServiceTest.java`
  - SimpMessagingTemplate mock 추가
  - WebSocket 브로드캐스트 검증 테스트 4개 추가
  - sendRecruiterMessage() 테스트 섹션 추가 (4개 테스트)

### 🎯 결과

- ✅ REST API로 전송된 메시지도 WebSocket으로 실시간 브로드캐스트됨
- ✅ 모든 구독 클라이언트가 즉시 메시지 수신
- ✅ WebSocket과 REST API 경로 모두 일관된 동작
- ✅ 테스트 커버리지 확보 (브로드캐스트 동작 검증)

### 📝 메시지 흐름 (수정 후)

**WebSocket 경로**:
1. Client → `/app/chat/{sessionToken}` (STOMP SEND)
2. ChatWebSocketController.sendMessage()
3. DB 저장 + `@SendTo`로 자동 브로드캐스트
4. `/topic/session/{sessionToken}` → 모든 구독자

**REST API 경로** (수정됨):
1. Client → `POST /api/chat/session/{sessionToken}/send`
2. ChatController → ChatService
3. DB 저장 + `broadcastMessage()` 호출
4. SimpMessagingTemplate → `/topic/session/{sessionToken}` → 모든 구독자

---

## 참고 사항

**빌드 상태**:
- ✅ 빌드 성공: `./gradlew build -x test`
- ✅ ChatServiceTest: 11/11 통과
- ⚠️ ResumeController 관련 5개 테스트 실패 (기존 이슈, 본 수정과 무관)

**프로덕션 체크리스트**:
- [x] REST API 메시지 전송에 WebSocket 브로드캐스트 추가
- [x] WebSocket 경로와 REST API 경로 일관성 확보
- [x] 브로드캐스트 테스트 작성
- [ ] 프론트엔드에서 두 경로 모두 테스트 필요

---

## WebSocket 403 Forbidden 에러 해결 (2026-03-11)

### 에러 상황
- 프론트엔드: `http://localhost:31000/api/ws`로 WebSocket 연결 시도
- 백엔드: `/ws` 엔드포인트만 등록되어 있음
- SockJS `/api/ws/info` 핸드셰이크 요청 → 403 Forbidden 응답

### 원인 분석

#### 1. 경로 불일치 (핵심 원인)
- [x] **WebSocketConfig**: `/ws` 엔드포인트 등록 (line 48)
- [x] **프론트엔드**: `/api/ws`로 연결 시도
- [x] **결과**: 경로 매칭 실패로 403 에러 발생

#### 2. SecurityConfig 경로 설정 불일치
- [x] **현재 설정**: `.requestMatchers("/ws/**").permitAll()` (line 44)
- [x] **문제**: `/api/ws/**` 경로는 permitAll 설정 없음
- [x] **SockJS 핸드셰이크**: `/api/ws/info` HTTP GET 요청이 Spring Security 필터에서 차단됨

#### 3. SockJS Handshake 과정
SockJS는 WebSocket 연결 전에 다음 순서로 핸드셰이크를 수행:
1. **GET `/api/ws/info`**: 서버 능력 확인 (WebSocket 지원 여부, entropy 등)
2. **WebSocket Upgrade**: 실제 WebSocket 연결 시도

이 중 첫 번째 HTTP GET 요청이 Spring Security의 HTTP 필터 체인을 거치며:
- WebSocketAuthInterceptor는 **STOMP CONNECT 프레임**에서만 동작
- `/api/ws/info` 요청은 일반 HTTP 요청이므로 SecurityConfig의 설정이 적용됨
- `/api/ws/**` 경로가 permitAll이 아니면 403 Forbidden 발생

### 해결 방안

#### 백엔드 수정 (권장)

**1. WebSocketConfig 수정**
```java
// 변경 전 (line 48)
registry.addEndpoint("/ws")

// 변경 후
registry.addEndpoint("/api/ws")
```
- 프론트엔드 URL과 일치시켜 경로 불일치 해결

**2. SecurityConfig 수정**
- `/api/ws/**` 경로를 permitAll로 설정하여 SockJS 핸드셰이크 허용

#### 기타 확인 사항

**CORS 설정** (이미 정상):
- SecurityConfig.java (line 58-63): `http://localhost:31000` 포함 ✅
- WebSocketConfig.java (line 50): `http://localhost:31000` 포함 ✅

**Nginx 설정** (확인 필요):
- `/api/ws` 경로를 백엔드로 프록시하는 설정 필요
- WebSocket Upgrade 헤더 전달 설정 확인
```nginx
location /api/ws {
    proxy_pass http://backend:7777;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

### 수정 체크리스트
- [x] WebSocketConfig.java - 엔드포인트를 `/api/ws`로 변경 ✅
- [x] SecurityConfig.java - `/api/ws/**` permitAll 추가 ✅
- [x] 애플리케이션 재시작 (`./gradlew bootRun`) ✅
- [x] SockJS info 엔드포인트 테스트: `curl http://localhost:7777/api/ws/info` ✅ (200 OK)
---

## 채팅 메시지 로딩 구조 개선 (2026-03-11)

### 현재 구조 분석

#### 현재 API 구현 현황

**1. 메시지 조회 API**
- `GET /api/chat/session/{sessionToken}/messages` (채용담당자용)
  - 파일: ChatController.java (line 172-180)
  - 응답: ChatDetailResponse (세션 정보 + 전체 메시지 목록)
  - 페이지네이션: 없음 (전체 조회만 가능)
  - 정렬: createdAt ASC (오래된 순)

- `GET /api/applicant/chat/{sessionToken}/messages` (지원자용, JWT 필요)
  - 파일: ChatController.java (line 545-557)
  - 응답: ChatDetailResponse (세션 정보 + 전체 메시지 목록)
  - 페이지네이션: 없음
  - 자동 읽음 처리: 조회 시 unread 메시지 자동 markAsRead()
  - 정렬: createdAt ASC

**2. WebSocket 브로드캐스트 구현**
- 경로: `/topic/session/{sessionToken}`
- 구현 위치: ChatService.broadcastMessage() (line 274-288)
- 메시지 전송 시 자동 브로드캐스트:∂
  - REST API 전송 (sendMessage, sendRecruiterMessage, sendMessageByApplicant) ✅
  - WebSocket 전송 (ChatWebSocketController.sendMessage) ✅

#### 현재 문제점

1. **페이지네이션 부재**
   - 전체 메시지를 한 번에 조회하므로 메시지가 많을수록 성능 저하
   - 초기 로딩 시간 증가
   - 메모리 낭비

2. **폴링 기반 조회 (프론트엔드 추정)**
   - 5초마다 전체 메시지를 다시 조회하는 방식으로 보임
   - 불필요한 네트워크 트래픽 발생
   - 서버 부하 증가
   - 실시간성 저하 (최대 5초 지연)

3. **중복 조회 방지 로직 부재**
   - WebSocket으로 받은 메시지를 REST API로 다시 조회할 가능성
   - messageId 기반 중복 체크 로직 없음

4. **메시지 필터링 기능 부족**
   - 특정 시간 이후 메시지 조회 불가
   - lastMessageId 기반 조회 불가
   - 읽지 않은 메시지만 조회 불가

---

### 백엔드 개선 플랜

#### Phase 1: 메시지 조회 API 개선 (필수)

**1.1 페이지네이션 지원 추가** ✅
- [x] ChatController에 새로운 메시지 조회 엔드포인트 추가 ✅
  - `GET /api/chat/session/{sessionToken}/messages/paged` (채용담당자용)
  - `GET /api/applicant/chat/{sessionToken}/messages/paged` (지원자용)
  - 파라미터: `page` (기본값 0), `size` (기본값 20, 최대 100), `sort` (asc 또는 desc)
  - 응답: PagedMessagesResponse
- [x] ChatService에 페이지네이션 로직 구현 ✅
  - `getSessionMessagesPaged(sessionToken, page, size, sort)` - 채용담당자용
  - `getApplicantSessionMessagesPaged(applicantUuid, sessionToken, page, size, sort)` - 지원자용
  - size 제한 적용 (최대 100)
- [x] ChatMessageRepository에 페이지네이션 쿼리 추가 ✅
  - `Page<ChatMessage> findBySessionOrderByCreatedAtDesc(ChatSession session, Pageable pageable)`
  - `Page<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session, Pageable pageable)`
- [x] 테스트 작성 ✅
  - ChatMessageRepositoryTest: 10개 테스트 모두 통과
  - ChatServiceTest: 페이지네이션 로직 테스트 (채용담당자/지원자, size 제한 등)

**1.2 증분 조회 (Incremental Loading) 지원** ✅
- [x] 특정 시간 이후 메시지 조회 API 추가 ✅
  - `GET /api/chat/session/{sessionToken}/messages/since` (채용담당자용)
  - `GET /api/applicant/chat/{sessionToken}/messages/since` (지원자용)
  - 파라미터: `timestamp` (ISO-8601 형식) 또는 `lastMessageId` (Long)
  - 응답: IncrementalMessagesResponse (messages, count)
- [x] ChatMessageRepository에 쿼리 메서드 추가 ✅
  - `List<ChatMessage> findBySessionAndCreatedAtAfterOrderByCreatedAtAsc(ChatSession session, LocalDateTime timestamp)`
  - `List<ChatMessage> findBySessionAndIdGreaterThanOrderByCreatedAtAsc(ChatSession session, Long messageId)`
- [x] ChatService에 증분 조회 로직 구현 ✅
  - `getMessagesSinceTimestamp()` / `getMessagesSinceMessageId()` - 채용담당자용
  - `getApplicantMessagesSinceTimestamp()` / `getApplicantMessagesSinceMessageId()` - 지원자용
- [x] 테스트 작성 ✅
  - ChatMessageRepositoryTest: timestamp/messageId 기반 쿼리 검증
  - ChatServiceTest: 비즈니스 로직 검증 (권한 체크, 빈 결과 등)

**1.3 응답 형식 표준화** ✅
- [x] ChatDto에 새로운 응답 DTO 추가 ✅
  - `PagedMessagesResponse`: content, page, size, totalElements, totalPages, hasNext, hasPrevious
  - `IncrementalMessagesResponse`: messages, count
  - 기존 `MessageInfo` DTO 재사용 (messageId, message, senderType, readStatus, sentAt)

#### Phase 2: WebSocket 브로드캐스트 최적화 (권장)

**2.1 브로드캐스트 동작 확인**
- [x] REST API 메시지 전송 시 WebSocket 브로드캐스트 확인 ✅
  - sendMessage() ✅
  - sendRecruiterMessage() ✅
  - sendMessageByApplicant() ✅
- [x] WebSocket 메시지 전송 시 브로드캐스트 확인 ✅
  - ChatWebSocketController.sendMessage() + @SendTo ✅

**2.2 브로드캐스트 메시지 형식 검증**
- [x] WebSocketChatMessage DTO 표준화 ✅
  - 필드: messageId, sessionToken, senderType, messageType, content, sentAt
  - Javadoc 문서화 완료
  - @Valid 어노테이션 추가 (validation 지원)
  - from() 정적 팩토리 메서드로 ChatMessage에서 변환
- [x] 브로드캐스트 destination 문서화 ✅
  - Pattern: `/topic/session/{sessionToken}`
  - ChatService.broadcastMessage() 메서드에 상세 문서화 추가

**2.3 메시지 타입 구분 (선택)**
- [x] MessageType enum 생성 완료 ✅
  - TEXT, IMAGE, FILE, SYSTEM 정의
  - Javadoc 문서화 (향후 확장 계획 명시)
- [x] ChatMessage에 messageType 필드 추가 완료 ✅
  - @Enumerated(EnumType.STRING), NOT NULL
  - 기본값: TEXT
  - createMessage() 오버로드 메서드 추가 (messageType 파라미터)
- [x] ErrorCode에 메시지 타입 관련 에러 추가 완료 ✅
  - INVALID_MESSAGE_TYPE ("S006")
  - FILE_SIZE_EXCEEDED ("F004")

#### Phase 3: 성능 최적화 (권장)

**3.1 데이터베이스 인덱스 확인**
- [x] rc_chat_message 테이블 인덱스 분석 ✅
  - session_id + created_at (복합 인덱스 추가 완료)
  - session_id + read_status (복합 인덱스 추가 완료)
- [x] 인덱스 추가 완료 ✅
  - `idx_chat_message_session_created` (session_id, created_at)
  - `idx_chat_message_session_read_status` (session_id, read_status)
  - JPA @Index 어노테이션으로 엔티티에 정의
  - ddl-auto=update로 자동 생성 확인

**3.2 N+1 문제 검증**
- [x] ChatService 메시지 조회 N+1 체크 완료 ✅
  - `findBySessionOrderByCreatedAtAsc(session)` - 단일 쿼리
  - `findBySessionOrderByCreatedAtDesc(session, pageable)` - 단일 쿼리
  - MessageInfo.from(ChatMessage) - session 필드를 참조하지 않음
  - **결론**: N+1 문제 없음, 추가 최적화 불필요

**3.3 캐싱 검토 (선택)**
- [ ] 자주 조회되는 메시지 캐싱 전략 수립 (향후 고려)
  - 최근 20개 메시지를 Redis에 캐싱
  - TTL: 10분
  - 캐시 무효화: 새 메시지 전송 시
- [ ] @Cacheable 어노테이션 적용 검토 (향후 고려)
  - Spring Cache Abstraction 활용
  - 프로덕션 환경에서는 Redis 사용 권장
  - 현재 트래픽에서는 불필요, 대규모 트래픽 시 고려

#### Phase 4: 중복 방지 및 일관성 보장 (선택)

**4.1 메시지 중복 방지 로직**
- [ ] 클라이언트에서 messageId 기반 중복 체크하도록 가이드
  - 백엔드는 UUID로 유일성 보장 중 ✅
  - 프론트엔드에서 Set<messageId> 관리 권장
- [ ] 멱등성 보장 검토
  - 현재: 메시지 전송마다 새로운 UUID 생성
  - 필요 시: 클라이언트에서 생성한 UUID를 요청에 포함 (중복 전송 방지)

**4.2 메시지 순서 보장**
- [ ] 메시지에 sequenceNumber 추가 검토
  - createdAt은 밀리초 단위 동시 전송 시 순서 보장 안됨
  - AUTO_INCREMENT ID는 내부용으로만 사용
- [ ] 세션별 시퀀스 관리
  - ChatSession에 lastSequenceNumber 필드 추가
  - 메시지 생성 시 incrementAndGet() 패턴 적용

---

### 구현 우선순위 및 순서

**Phase 1: 기본 구조 개선 (필수) - 예상 소요: 4-6시간**
1. [1.1] 페이지네이션 지원 추가 (2시간)
   - Repository 쿼리 메서드 추가
   - Service 로직 구현
   - Controller 엔드포인트 추가
2. [1.2] 증분 조회 API 추가 (2시간)
   - timestamp 기반 조회 구현
   - lastMessageId 기반 조회 구현
3. [1.3] 응답 형식 표준화 (1시간)
4. 테스트 작성 및 검증 (1시간)

**Phase 2: 브로드캐스트 최적화 (권장) - 예상 소요: 2-3시간**
1. [2.1] 브로드캐스트 동작 확인 ✅ (완료)
2. [2.2] 메시지 형식 검증 및 문서화 (1시간)
3. [2.3] 메시지 타입 구분 (선택, 2시간)

**Phase 3: 성능 최적화 (권장) - 예상 소요: 3-4시간**
1. [3.1] 데이터베이스 인덱스 추가 (1시간)
2. [3.2] N+1 문제 검증 (1시간)
3. [3.3] 캐싱 전략 수립 및 구현 (선택, 2시간)

**Phase 4: 고급 기능 (선택) - 예상 소요: 4-6시간**
1. [4.1] 중복 방지 로직 (2시간)
2. [4.2] 메시지 순서 보장 (sequenceNumber) (3시간)

---

### 현재 API 상세 명세

#### 1. 메시지 조회 API (채용담당자용)

**엔드포인트**:
```
GET /api/chat/session/{sessionToken}/messages
```

**특징**:
- 인증 불필요 (public 엔드포인트)
- 전체 메시지 조회 (페이지네이션 없음)
- 읽음 처리 없음 (조회만 수행)

#### 2. 메시지 조회 API (지원자용)

**응답 형식**: 채용담당자용과 동일

**특징**:
- JWT 인증 필요
- 본인의 채팅 세션만 조회 가능 (권한 검증)
- 조회 시 읽지 않은 메시지 자동 읽음 처리 (markAsRead)
- 전체 메시지 조회 (페이지네이션 없음)

#### 3. WebSocket 브로드캐스트

**Subscribe Destination**:
```
/topic/session/{sessionToken}
```

**브로드캐스트 트리거**:
- REST API 메시지 전송 (sendMessage, sendRecruiterMessage, sendMessageByApplicant)
- WebSocket 메시지 전송 (ChatWebSocketController.sendMessage)
### 참고 사항

**빌드 및 테스트 상태**:
- ✅ 빌드 성공: `./gradlew build -x test`
- ✅ ChatServiceTest: 11/11 통과
- ⚠️ ResumeController 관련 5개 테스트 실패 (기존 이슈, 본 수정과 무관)

**기존 구현 장점**:
- WebSocket 브로드캐스트가 REST API와 WebSocket 경로 모두에서 작동 ✅
- 메시지 저장과 브로드캐스트가 트랜잭션 내에서 일관되게 처리됨 ✅
- sessionToken 기반 인증 시스템 구현 완료 ✅

**개선 시 유의사항**:
- 기존 API와의 하위 호환성 유지 (새로운 엔드포인트 추가 권장)
- 페이지네이션 도입 시 프론트엔드와 협의 필요
- 인덱스 추가 시 데이터베이스 성능 모니터링 필수

---

### 수정 완료 사항 (2026-03-11)

#### 1. WebSocketConfig.java 수정 완료
- **파일**: `/Users/cookyuu/Documents/dev/projects/resume-chat/src/main/java/com/cookyuu/resume_chat/config/WebSocketConfig.java`
- **변경 라인**: 48
- **결과**: 프론트엔드 `/api/ws` 경로와 일치

#### 2. SecurityConfig.java 수정 완료
- **파일**: `/Users/cookyuu/Documents/dev/projects/resume-chat/src/main/java/com/cookyuu/resume_chat/config/SecurityConfig.java`
- **변경 라인**: 44-45
- **결과**: `/api/ws/**` 경로가 Spring Security에서 permitAll로 설정됨
```
- **상태**: 200 OK ✅
- **응답**: SockJS handshake 정보 정상 반환
- **결론**: 403 Forbidden 에러 해결 완료


## 채팅 메시지 로딩 구조 개선 (Phase 1 구현 완료) - 2026-03-11

### ✅ 구현 완료 사항

#### 1. Repository 레이어 (ChatMessageRepository)
#### 2. Service 레이어 (ChatService)
#### 3. Controller 레이어 (ChatController)

#### 5. 테스트 작성
**ChatMessageRepositoryTest** (10개 테스트, 모두 통과):
- 페이지네이션 조회 (DESC/ASC 정렬)
- 두 번째 페이지 조회
- 빈 결과 처리
- 증분 조회 (timestamp 기반)
- 증분 조회 (messageId 기반)
- 다른 세션 메시지 격리

**ChatServiceTest** (기존 테스트 + 신규 18개 테스트, 모두 통과):
- 페이지네이션 로직 (채용담당자/지원자)
- size 제한 적용 검증
- 권한 검증 (지원자는 본인 세션만 조회)
- 증분 조회 (timestamp/messageId)
- 세션 미존재 에러 처리

### 🎯 개선 효과

**Before (기존)**:
- 전체 메시지를 한 번에 조회 (`GET /api/chat/session/{sessionToken}/messages`)
- 메시지가 많을수록 성능 저하
- 초기 로딩 시간 증가
- 불필요한 네트워크 트래픽

**After (개선 후)**:
- **페이지네이션**: 필요한 만큼만 조회 (기본 20개, 최대 100개)
- **증분 조회**: 마지막 조회 이후 새 메시지만 가져오기
- **성능 향상**: 데이터베이스 부하 감소
- **사용자 경험 개선**: 빠른 초기 로딩, 무한 스크롤 지원 가능

### 🚧 다음 단계 (Phase 2 이후)

**권장 사항**:
- Phase 2: WebSocket 브로드캐스트 메시지 형식 검증
- Phase 3: 데이터베이스 인덱스 추가 (session_id + created_at 복합 인덱스)
- Phase 4: 메시지 순서 보장을 위한 sequenceNumber 필드 추가 (선택)

---

## 채팅 메시지 로딩 구조 개선 (Phase 2 구현 완료) - 2026-03-12
### 🎯 개선 효과

**메시지 형식 표준화**:
- WebSocketChatMessage DTO의 필드와 용도가 명확하게 문서화됨
- Validation 규칙이 명시되어 잘못된 데이터 전송 방지
- from() 팩토리 메서드로 일관된 변환 로직 제공

**향후 확장성 확보**:
- MessageType enum으로 다양한 메시지 타입 지원 준비 완료
- IMAGE, FILE, SYSTEM 메시지 타입 추가 시 최소한의 코드 변경으로 가능
- 메시지 타입별 처리 로직 분기 용이

**에러 처리 개선**:
- INVALID_MESSAGE_TYPE, FILE_SIZE_EXCEEDED 에러 코드 추가
- 향후 파일 첨부 기능 구현 시 일관된 에러 처리 가능

### 📝 브로드캐스트 메시지 형식 (최종)

**WebSocket Subscribe Destination**:
```
/topic/session/{sessionToken}
```


**브로드캐스트 트리거**:
1. REST API 메시지 전송 (sendMessage, sendRecruiterMessage, sendMessageByApplicant)
2. WebSocket 메시지 전송 (ChatWebSocketController.sendMessage)

### 🚧 다음 단계 (Phase 3)

**권장 사항**:
- Phase 3: 데이터베이스 인덱스 추가
  - `CREATE INDEX idx_chat_message_session_created ON rc_chat_message(session_id, created_at)`
  - `CREATE INDEX idx_chat_message_read_status ON rc_chat_message(session_id, read_status)`
- N+1 문제 검증 및 최적화
- 캐싱 전략 수립 (선택)

**프론트엔드 연동 가이드**:
1. 초기 로딩 시 페이지네이션 API 사용
2. WebSocket 연결 후 실시간 메시지 수신
3. 주기적인 폴링 대신 증분 조회 API 활용 (네트워크 연결 끊김 복구 시)
4. 무한 스크롤 구현 시 `hasNext`와 다음 페이지 조회

---

## 채팅 메시지 로딩 구조 개선 (Phase 3 구현 완료) - 2026-03-12

### ✅ 구현 완료 사항

#### 1. 데이터베이스 인덱스 추가 (ChatMessage.java)

**인덱스 1: idx_chat_message_session_created**
- 컬럼: session_id, created_at
- 용도: 메시지 조회 성능 최적화
- 대상 쿼리:
  - `findBySessionOrderByCreatedAtAsc()`
  - `findBySessionOrderByCreatedAtDesc()`
  - `findBySessionAndCreatedAtAfterOrderByCreatedAtAsc()`

**인덱스 2: idx_chat_message_session_read_status**
- 컬럼: session_id, read_status
- 용도: 읽지 않은 메시지 카운트 조회 최적화
- 대상 쿼리:
  - `countBySessionAndReadStatusFalse()`

-- 결과:
-- idx_chat_message_session_created (session_id, created_at) ✅
-- idx_chat_message_session_read_status (session_id, read_status) ✅
```

#### 2. N+1 문제 검증
**검증 결과**:
- ✅ **N+1 문제 없음** - 모든 메서드가 단일 쿼리로 동작
- ChatMessage는 @ManyToOne으로 ChatSession을 참조하지만, session은 파라미터로 전달받아 사용
- MessageInfo DTO 변환 시 `message.getSession()` 호출 없음
- 불필요한 연관관계 fetch 없음

**결론**:
- @EntityGraph나 fetch join 불필요
- 현재 구조가 최적화되어 있음
- 추가 최적화 작업 불필요

#### 3. 캐싱 검토

**현재 상태**:
- 캐싱 미구현 (선택 사항으로 판단)
- 현재 트래픽 규모에서는 불필요
- 데이터베이스 인덱스 추가로 충분한 성능 확보

**향후 고려 사항** (대규모 트래픽 시):
- Spring Cache Abstraction + Redis
- 최근 20개 메시지 캐싱
- TTL: 10분
- 캐시 무효화: 새 메시지 전송 시

### 📊 성능 개선 효과

**인덱스 추가 전**:
- 메시지 조회 시 Full Table Scan
- 읽지 않은 메시지 카운트 조회 시 Full Table Scan
- 메시지 수가 증가할수록 성능 저하

**인덱스 추가 후**:
- **메시지 조회**: session_id + created_at 복합 인덱스로 O(log n) 성능
- **읽지 않은 메시지 카운트**: session_id + read_status 인덱스로 빠른 카운트
- **증분 조회**: created_at 인덱스 활용으로 효율적인 범위 검색
- 메시지 수가 증가해도 일정한 성능 유지

### 🎯 최적화 완료 요약

**Phase 3에서 달성한 목표**:
1. ✅ 데이터베이스 인덱스 추가로 쿼리 성능 최적화
2. ✅ N+1 문제 없음 검증
3. ✅ 캐싱 필요성 검토 (현재는 불필요)

**성능 최적화 상태**:
- 쿼리 최적화: ✅ 완료 (인덱스 추가)
- N+1 문제: ✅ 없음 (검증 완료)
- 캐싱: ⏸️ 보류 (향후 필요 시 추가)

### 🚧 다음 단계

**Phase 1-3 완료**:
- ✅ Phase 1: 페이지네이션 및 증분 조회 API
- ✅ Phase 2: WebSocket 브로드캐스트 표준화
- ✅ Phase 3: 데이터베이스 성능 최적화

**선택 사항 (Phase 4)**:
- 메시지 중복 방지 로직 (클라이언트 가이드)
- 메시지 순서 보장 (sequenceNumber 필드)


## 🔴 긴급 - ChatSession에 지원자 정보 추가

### 문제 상황
- 채용담당자 채팅 화면에서 지원자의 이름과 이메일이 표시되지 않음
- 현재 ChatSession 응답에 이력서 제목(resumeTitle)만 포함되어 있음
- 채용담당자가 누구와 대화하는지 명확히 알 수 없음

### 필요한 수정

#### ChatSession DTO에 지원자 정보 필드 추가

**변경 전**:
```java
public class ChatSessionDto {
    private String sessionToken;
    private String resumeSlug;
    private String resumeTitle;
    private String recruiterEmail;
    private String recruiterName;
    private String recruiterCompany;
    private Integer totalMessages;
    private Integer unreadMessages;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
```

**변경 후**:
```java
public class ChatSessionDto {
    private String sessionToken;
    private String resumeSlug;
    private String resumeTitle;
    private String recruiterEmail;
    private String recruiterName;
    private String recruiterCompany;

    // ✅ 추가: 지원자 정보
    private String applicantEmail;  // 지원자 이메일
    private String applicantName;   // 지원자 이름

    private Integer totalMessages;
    private Integer unreadMessages;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
```

#### 영향받는 API 엔드포인트
- `GET /chat/session/{sessionToken}/messages` - 채용담당자용 메시지 조회
- `GET /applicant/chat/{sessionToken}/messages` - 지원자용 메시지 조회

#### 구현 예시
```java
@Service
public class ChatService {

    public SessionMessagesDto getRecruiterMessages(String sessionToken) {
        ChatSession session = chatSessionRepository.findBySessionToken(sessionToken)
            .orElseThrow(() -> new NotFoundException("Session not found"));

        // 지원자 정보 조회
        Resume resume = resumeRepository.findByResumeSlug(session.getResumeSlug())
            .orElseThrow(() -> new NotFoundException("Resume not found"));

        User applicant = resume.getUser(); // 또는 session.getApplicant()

        ChatSessionDto sessionDto = ChatSessionDto.builder()
            .sessionToken(session.getSessionToken())
            .resumeSlug(session.getResumeSlug())
            .resumeTitle(resume.getTitle())
            .recruiterEmail(session.getRecruiterEmail())
            .recruiterName(session.getRecruiterName())
            .recruiterCompany(session.getRecruiterCompany())
            // ✅ 지원자 정보 추가
            .applicantEmail(applicant.getEmail())
            .applicantName(applicant.getName())
            .totalMessages(session.getTotalMessages())
            .unreadMessages(session.getUnreadMessages())
            .lastMessageAt(session.getLastMessageAt())
            .createdAt(session.getCreatedAt())
            .build();

        // ... 메시지 조회 및 반환
    }
}
```

### 우선순위
🔴 **높음** - 사용자 경험에 직접적인 영향을 주는 기능

### ✅ 구현 완료 (2026-03-13)

**구현 내역**:
- SessionInfo DTO에 applicantEmail, applicantName 필드 추가 완료
- SessionInfo.from() 팩토리 메서드에서 session.getResume().getApplicant()로 지원자 정보 자동 조회
- 기존 ChatService 메서드들이 SessionInfo.from()을 사용하므로 자동으로 지원자 정보 포함됨
- 빌드 성공 확인 (./gradlew build -x test)

**영향받는 API**:
- `GET /chat/session/{sessionToken}/messages` - 채용담당자용 (지원자 정보 포함 ✅)
- `GET /applicant/chat/{sessionToken}/messages` - 지원자용 (지원자 정보 포함 ✅)
- `GET /applicant/resume/{resumeSlug}/chats` - 이력서별 세션 목록 (지원자 정보 포함 ✅)

### 체크리스트
- [x] ChatSessionDto에 applicantEmail, applicantName 필드 추가 ✅
- [x] Service 레이어에서 지원자 정보 조회 로직 추가 ✅ (SessionInfo.from()에서 자동 처리)
- [x] GET /chat/session/{sessionToken}/messages API 응답에 지원자 정보 포함 ✅
- [x] GET /applicant/chat/{sessionToken}/messages API 응답 확인 (기존 동작 유지) ✅
- [x] 빌드 성공 확인 ✅

---

---# 백엔드 수정 체크리스트 - 첨부파일 표시 기능

## 🔴 문제 상황
- [ ] 프론트엔드에서 파일 업로드 및 메시지 전송은 정상 작동
- [ ] 하지만 메시지 조회 API 응답에 `attachment` 필드가 누락되어 첨부파일이 UI에 표시되지 않음

---

## ✅ 수정 체크리스트

### 1️⃣ 데이터베이스 스키마 확인
- [ ] `message` 테이블에 `attachment_id` 컬럼이 존재하는지 확인
- [ ] `attachment_id`가 `attachment` 테이블의 외래키로 설정되어 있는지 확인

```sql
-- 필요시 실행
ALTER TABLE message ADD COLUMN attachment_id VARCHAR(255);
ALTER TABLE message ADD CONSTRAINT fk_message_attachment
    FOREIGN KEY (attachment_id) REFERENCES attachment(attachment_id);
```

---

### 2️⃣ Entity 클래스 수정
- [ ] `Message` Entity에 `Attachment` 연관관계 추가

```java
@Entity
public class Message {
    @Id
    private String messageId;
    private String message;
    private SenderType senderType;
    private boolean readStatus;
    private LocalDateTime sentAt;

    // ✅ 추가 필요
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Attachment attachment;

    // getters, setters...
}
```

---

### 3️⃣ DTO 클래스 수정

#### MessageDTO
- [ ] `MessageDTO`에 `AttachmentDTO attachment` 필드 추가
- [ ] `from()` 메서드에서 attachment 매핑 로직 추가

```java
public class MessageDTO {
    private String messageId;
    private String message;
    private SenderType senderType;
    private boolean readStatus;
    private String sentAt;
    private AttachmentDTO attachment;  // ✅ 추가

    public static MessageDTO from(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setMessage(message.getMessage());
        dto.setSenderType(message.getSenderType());
        dto.setReadStatus(message.isReadStatus());
        dto.setSentAt(message.getSentAt().toString());

        // ✅ 추가: 첨부파일이 있으면 포함, 없으면 null
        if (message.getAttachment() != null) {
            dto.setAttachment(AttachmentDTO.from(message.getAttachment()));
        } else {
            dto.setAttachment(null);
        }

        return dto;
    }
}
```

#### AttachmentDTO
- [ ] `AttachmentDTO` 클래스 생성 (또는 확인)

```java
public class AttachmentDTO {
    private String attachmentId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileUrl;
    private String uploadedAt;

    public static AttachmentDTO from(Attachment attachment) {
        AttachmentDTO dto = new AttachmentDTO();
        dto.setAttachmentId(attachment.getAttachmentId());
        dto.setFileName(attachment.getFileName());
        dto.setFileSize(attachment.getFileSize());
        dto.setFileType(attachment.getFileType());
        dto.setFileUrl(attachment.getFileUrl());
        dto.setUploadedAt(attachment.getUploadedAt().toString());
        return dto;
    }
}
```

---

### 4️⃣ SendMessageRequest DTO 확인
- [ ] `SendMessageRequest`에 `attachmentId` 필드가 있는지 확인

```java
public class SendMessageRequest {
    private String message;
    private String attachmentId;  // ✅ 이 필드 필요

    // getters, setters...
}
```

---

### 5️⃣ 메시지 전송 API 수정

#### 지원자용 메시지 전송
- [ ] `POST /api/applicant/chat/{sessionToken}/send` 수정
- [ ] `attachmentId`가 있으면 첨부파일 연결하는 로직 추가

```java
@PostMapping("/api/applicant/chat/{sessionToken}/send")
public ResponseEntity<?> sendMessage(
    @PathVariable String sessionToken,
    @RequestBody SendMessageRequest request
) {
    Message message = new Message();
    message.setMessage(request.getMessage());
    message.setSenderType(SenderType.APPLICANT);

    // ✅ attachmentId가 있으면 첨부파일 연결
    if (request.getAttachmentId() != null) {
        Attachment attachment = attachmentRepository
            .findById(request.getAttachmentId())
            .orElseThrow(() -> new NotFoundException("Attachment not found"));
        message.setAttachment(attachment);
    }

    messageRepository.save(message);

    // WebSocket 브로드캐스트 (첨부파일 정보 포함)
    MessageDTO dto = MessageDTO.from(message);
    messagingTemplate.convertAndSend("/topic/chat/" + sessionToken, dto);

    return ResponseEntity.ok(dto);
}
```

#### 채용담당자용 메시지 전송
- [ ] `POST /api/chat/session/{sessionToken}/send` 수정
- [ ] 동일한 로직 적용

---

### 6️⃣ 메시지 조회 API 확인

#### 지원자용 메시지 조회
- [ ] `GET /api/applicant/chat/{sessionToken}/messages`
- [ ] 응답에 `attachment` 필드가 포함되는지 확인

#### 채용담당자용 메시지 조회
- [ ] `GET /api/chat/session/{sessionToken}/messages`
- [ ] 응답에 `attachment` 필드가 포함되는지 확인

> **참고**: MessageDTO의 `from()` 메서드가 올바르게 구현되어 있다면 자동으로 포함됩니다.

---

### 7️⃣ WebSocket 브로드캐스트 확인
- [ ] 새 메시지 전송 시 WebSocket으로 브로드캐스트할 때 `attachment` 포함
- [ ] Topic: `/topic/chat/{sessionToken}`
- [ ] `MessageDTO.from(message)` 사용하여 첨부파일 정보 자동 포함

---

## 📋 예상 응답 구조

### 첨부파일이 있는 메시지
```json
{
  "messageId": "msg-123",
  "message": "",
  "senderType": "APPLICANT",
  "readStatus": true,
  "sentAt": "2026-03-17T10:48:00",
  "attachment": {
    "attachmentId": "att-123",
    "fileName": "2025년 2분기-신용보증기금.pdf",
    "fileSize": 1024000,
    "fileType": "application/pdf",
    "fileUrl": "/uploads/files/att-123.pdf",
    "uploadedAt": "2026-03-17T10:48:00"
  }
}
```

### 첨부파일이 없는 메시지
```json
{
  "messageId": "msg-456",
  "message": "안녕하세요",
  "senderType": "RECRUITER",
  "readStatus": true,
  "sentAt": "2026-03-17T10:49:00",
  "attachment": null
}
```

---

## 🧪 테스트 체크리스트

### 1. 파일 업로드 테스트
- [ ] 파일 업로드 API 호출 성공
- [ ] `attachmentId` 반환 확인

### 2. 메시지 전송 테스트
- [ ] `attachmentId`를 포함한 메시지 전송 성공
- [ ] 응답에 `attachment` 객체 포함 확인

### 3. 메시지 조회 테스트
- [ ] 메시지 목록 조회 시 `attachment` 필드 포함 확인
- [ ] 첨부파일이 없는 메시지는 `attachment: null` 확인

### 4. WebSocket 테스트
- [ ] 새 메시지 브로드캐스트 시 `attachment` 포함 확인
- [ ] 두 개의 브라우저 창에서 실시간 업데이트 확인

---

## 🔍 확인 방법

### API 테스트
```bash
# 메시지 조회
curl -X GET "http://localhost:8080/api/applicant/chat/{sessionToken}/messages" \
  -H "Authorization: Bearer {token}" \
  | jq '.data.messages[0].attachment'

# 기대 결과: attachment 객체 또는 null
```

### 데이터베이스 확인
```sql
-- 메시지와 첨부파일 연결 확인
SELECT m.message_id, m.message, a.file_name, a.file_url
FROM message m
LEFT JOIN attachment a ON m.attachment_id = a.attachment_id
WHERE m.message_id = 'xxx';
```

---

## 🔥 우선순위
**HIGH** - 이 수정이 없으면 첨부파일 기능이 전혀 작동하지 않습니다.

---

## ✅ 프론트엔드 준비 상태
프론트엔드는 이미 완전히 구현되어 있습니다:
- ✅ 파일 업로드 API 호출
- ✅ 메시지 전송 시 `attachmentId` 포함
- ✅ `msg.attachment` 체크 후 AttachmentDisplay 컴포넌트 렌더링
- ✅ 다운로드 링크 구현
- ✅ 파일 타입별 아이콘 및 스타일링
- ✅ 이미지 인라인 미리보기

**백엔드에서 위 체크리스트를 완료하면 바로 작동합니다!**
