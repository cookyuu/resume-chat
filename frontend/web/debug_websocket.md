# WebSocket 디버깅 가이드

## 1단계: 콘솔 로그 확인

### 채팅방 입장 시 확인할 로그

**브라우저 콘솔 (F12 → Console)**을 열고 다음 로그를 찾으세요:

#### ✅ 정상적인 경우:
```
[WebSocket] ✅ Connected successfully
[WebSocket] Connection details: {command: "CONNECTED", headers: {...}}
[useChatWebSocket] Starting subscriptions for session: abc123
[WebSocket] ✅ Subscribed to /topic/chat/abc123
[WebSocket] Total active subscriptions: 1 또는 2
[WebSocket] ✅ Subscribed to /topic/chat/abc123/presence
[WebSocket] Total active subscriptions: 2
```

**질문**: 위 로그가 모두 출력되나요?
- [ ] 예 (다음 단계로)
- [ ] 아니오 (어떤 로그가 없는지 확인)

---

## 2단계: 메시지 전송 후 로그 확인

한쪽에서 메시지를 전송하고, **상대방 브라우저 콘솔**을 확인하세요.

### 시나리오 1: 채용담당자 → 지원자

1. **채용담당자**: 메시지 "안녕하세요" 전송
2. **지원자 브라우저 콘솔**: 다음 로그 확인

#### ✅ 정상적인 경우:
```
[WebSocket] 📨 Message received on /topic/chat/abc123
[useChatWebSocket] 🔔 Raw message received: {"messageId":"...","message":"안녕하세요",...}
[useChatWebSocket] ✅ Parsed message: {messageId: "...", message: "안녕하세요", ...}
[useChatWebSocket] 📝 Updating cache with new message
[useChatWebSocket] ✅ Cache updated. Total messages: X
```

#### ❌ 비정상적인 경우 A: 아무 로그도 없음
```
(아무것도 출력되지 않음)
```
→ **원인**: 백엔드에서 WebSocket 메시지를 보내지 않음

#### ❌ 비정상적인 경우 B: 에러 로그
```
[useChatWebSocket] ⚠️ No existing data in cache
```
→ **원인**: React Query 캐시가 초기화되지 않았거나 queryKey가 다름

**질문**: 어떤 경우인가요?
- [ ] 정상 (로그가 모두 출력됨)
- [ ] 비정상 A (아무 로그도 없음)
- [ ] 비정상 B (에러 로그 출력)
- [ ] 기타 (콘솔에 출력된 로그를 복사해주세요)

---

## 3단계: Network 탭에서 WebSocket 확인

### Chrome DevTools Network 탭

1. **F12** → **Network** 탭
2. **WS** 필터 클릭
3. WebSocket 연결 클릭 (이름: `ws` 또는 `/api/ws/...`)
4. **Messages** 탭 선택

### 확인사항

#### A. 구독 확인 (SUBSCRIBE 프레임)

다음과 같은 SUBSCRIBE 프레임이 있어야 합니다:

```
↑ SUBSCRIBE
destination:/topic/chat/abc123
id:sub-0
```

**질문**: SUBSCRIBE 프레임이 보이나요?
- [ ] 예 (destination이 무엇인가요? _______________)
- [ ] 아니오

#### B. 메시지 수신 확인 (MESSAGE 프레임)

메시지 전송 후 다음과 같은 MESSAGE 프레임이 **상대방에게** 와야 합니다:

```
↓ MESSAGE
destination:/topic/chat/abc123
content-type:application/json

{"messageId":"xxx","senderType":"RECRUITER","message":"안녕하세요","sentAt":"2026-03-12T..."}
```

**질문**: 메시지 전송 후 MESSAGE 프레임이 보이나요?
- [ ] 예 (destination이 무엇인가요? _______________)
- [ ] 아니오 (전혀 안 옴)

---

## 4단계: sessionToken 일치 여부 확인

콘솔에 다음을 입력해서 sessionToken을 확인하세요:

```javascript
// 현재 페이지 URL
console.log('Current URL:', window.location.href);

// URL에서 sessionToken 추출
const path = window.location.pathname;
console.log('Path:', path);

// 구독 중인 토픽 확인 (WebSocket 클라이언트 내부 확인)
```

**질문**:
- 채용담당자 URL: _____________________
- 지원자 URL: _____________________
- 두 URL의 sessionToken이 같나요?
  - [ ] 예
  - [ ] 아니오 (다르면 sessionToken 값을 알려주세요)

---

## 5단계: 백엔드 로그 확인

백엔드 콘솔에서 다음을 확인하세요:

### 메시지 전송 시 로그

```
메시지 브로드캐스트 완료: sessionToken=abc123, messageId=xxx
```

또는

```
Sending message to /topic/chat/abc123
```

**질문**: 백엔드에서 브로드캐스트 로그가 출력되나요?
- [ ] 예 (로그 내용: _______________)
- [ ] 아니오
- [ ] 확인 불가

---

## 6단계: 토픽 불일치 의심

### 현재 프론트엔드 구독 토픽

- 메시지: `/topic/chat/{sessionToken}`
- Presence: `/topic/chat/{sessionToken}/presence`

### 백엔드가 보내는 토픽 확인

백엔드 코드에서 다음을 확인하세요:

```java
messagingTemplate.convertAndSend(
    "/topic/chat/" + sessionToken,  // ← 이 부분이 일치해야 함
    message
);
```

**질문**: 백엔드 코드를 확인할 수 있나요?
- [ ] 예 (어디로 보내는지 확인: _______________)
- [ ] 아니오

---

## 일반적인 문제 및 해결

### 문제 1: 백엔드 브로드캐스트 누락
**증상**: Network 탭에 MESSAGE 프레임이 전혀 없음
**해결**: 백엔드에 `messagingTemplate.convertAndSend()` 추가

### 문제 2: 토픽 불일치
**증상**: MESSAGE 프레임은 오지만 다른 destination
**해결**: 백엔드와 프론트엔드의 토픽 경로 일치시키기

### 문제 3: sessionToken 불일치
**증상**: 메시지는 오지만 다른 채팅방으로 감
**해결**: 두 사용자가 같은 sessionToken 사용하는지 확인

### 문제 4: React Query 캐시 문제
**증상**: 메시지는 수신되지만 화면에 안 뜸
**해결**: queryKey 확인, setQueryData 로직 확인

---

## 테스트 시나리오

### 시나리오 A: 두 브라우저 테스트

1. **브라우저 1** (채용담당자): 시크릿 모드로 `/chat/{resumeSlug}` 접속
2. **브라우저 2** (지원자): 일반 모드로 로그인 후 `/chat/session/{sessionToken}` 접속
3. **브라우저 1**: 메시지 전송
4. **브라우저 2 콘솔**: 로그 확인
5. **역순**: 브라우저 2 → 1로 메시지 전송 후 브라우저 1 콘솔 확인

### 시나리오 B: 같은 브라우저, 다른 탭

1. **탭 1**: 채용담당자 채팅방
2. **탭 2**: 지원자 채팅방 (같은 sessionToken)
3. **탭 1**: 메시지 전송
4. **탭 2**: F12 콘솔 확인

---

## 다음 단계

위 체크리스트를 확인하고 다음 정보를 알려주세요:

1. **콘솔 로그**: 메시지 전송 후 어떤 로그가 출력되나요?
2. **Network 탭**: MESSAGE 프레임이 보이나요?
3. **sessionToken**: 양쪽이 같은 sessionToken을 사용하나요?
4. **백엔드 로그**: 브로드캐스트 로그가 출력되나요?

정보를 알려주시면 정확한 원인을 파악하고 해결하겠습니다!
