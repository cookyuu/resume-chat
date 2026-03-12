# 이슈 목록

## 🔴 긴급 - 타이핑 시 WebSocket 재구독 문제 (해결됨 ✅)

### 문제 상황
- 입력 필드에 타이핑할 때마다 WebSocket cleanup → 재구독이 반복됨
- 로그: `Cleanup → UNSUBSCRIBE → Starting subscriptions → SUBSCRIBE` 반복
- 성능 저하 및 리소스 낭비

### 원인 분석
- **타이핑 → message state 변경 → 컴포넌트 재렌더링**
- `queryKey: chatQueryKeys.messages(sessionToken!)` 매번 새로운 배열 생성
- useEffect 의존성 배열에 참조가 변경되는 값이 포함됨
- 결과: 타이핑할 때마다 useEffect 재실행 → cleanup → 재구독

### 해결 방법 (적용 완료 ✅)

**ChatPage.tsx**, **RecruiterChatPage.tsx** 수정:

```typescript
// Before ❌
useChatWebSocket({
  sessionToken: sessionToken!,
  queryKey: chatQueryKeys.messages(sessionToken!),  // 매번 새로 생성
  onStatusChange: setConnectionStatus,
  onPresenceChange: setRecruiterOnline,
  counterpartType: 'RECRUITER',
});

// After ✅
const queryKey = useMemo(() => chatQueryKeys.messages(sessionToken!), [sessionToken]);

const handleStatusChange = useCallback((status: ConnectionStatus) => {
  setConnectionStatus(status);
}, []);

const handlePresenceChange = useCallback((online: boolean) => {
  setRecruiterOnline(online);
}, []);

useChatWebSocket({
  sessionToken: sessionToken!,
  queryKey,  // 메모이제이션된 값
  onStatusChange: handleStatusChange,  // 안정적인 참조
  onPresenceChange: handlePresenceChange,  // 안정적인 참조
  counterpartType: 'RECRUITER',
});
```

### 체크리스트
- [x] ChatPage.tsx queryKey 메모이제이션 ✅
- [x] ChatPage.tsx 콜백 함수 useCallback ✅
- [x] RecruiterChatPage.tsx queryKey 메모이제이션 ✅
- [x] RecruiterChatPage.tsx 콜백 함수 useCallback ✅

---

## 🔴 긴급 - WebSocket 중복 연결 문제 (해결됨 ✅)

### 문제 상황
- 콘솔에서 WebSocket session이 **두 개씩** 맺어지는 현상 발생
- 하나의 채팅방에 여러 개의 중복 구독이 생성됨
- **비정상적인 상황**으로, 즉시 수정 필요

### 원인 분석

#### 1. React Strict Mode로 인한 이중 마운트 (주요 원인)
- 파일: `src/main.tsx:7`
- React 18의 StrictMode는 개발 환경에서 컴포넌트를 **두 번 마운트**합니다
- 마운트 → 언마운트 → 재마운트 순서로 실행되어 side effect 감지
- 결과: `useEffect`가 두 번 실행되어 WebSocket 연결/구독이 두 번 생성됨

#### 2. useChatWebSocket의 cleanup 버그 (치명적)
- 파일: `src/shared/hooks/useChatWebSocket.ts`

**문제점**:
```typescript
// Line 54-109: waitForConnection interval 설정
const waitForConnection = setInterval(() => {
  if (wsClient.isConnected() && !isSubscribedRef.current) {
    // ... 구독 설정 ...

    // ❌ 잘못된 cleanup 위치
    return () => {  // Line 103
      unsubscribeMessages();
      unsubscribePresence();
      isSubscribedRef.current = false;
    };  // Line 107
  }
}, 100);

// Line 111-116: 실제 useEffect cleanup
return () => {
  clearInterval(waitForConnection);
  removeStatusListener();
  isSubscribedRef.current = false;
  // ❌ 구독 해제가 없음! (unsubscribe 호출 안됨)
};
```

**문제 설명**:
- Line 103-107의 `return`은 **interval 콜백의 반환값**일 뿐, useEffect의 cleanup이 아닙니다
- 진짜 cleanup은 Line 111-116인데, 여기서는 **구독 해제를 하지 않습니다**
- 결과: 컴포넌트가 언마운트되어도 구독이 해제되지 않아 **메모리 누수** 발생
- Strict Mode에서 재마운트 시 새로운 구독이 추가되어 **중복 구독** 발생

#### 3. 싱글톤 WebSocket + 컴포넌트별 구독 관리
- `getWebSocketClient()`는 전역 싱글톤 인스턴스 (하나의 연결만 유지)
- 하지만 각 컴포넌트가 독립적으로 `useChatWebSocket` 훅을 사용하여 구독 관리
- Strict Mode에서 재마운트 시 같은 토픽에 여러 번 구독 가능

### 영향 범위
- **리소스 낭비**: 불필요한 중복 연결/구독
- **메시지 중복 수신**: 같은 메시지를 여러 번 받을 가능성
- **메모리 누수**: cleanup 시 구독이 해제되지 않음
- **React Query 캐시 오염**: 같은 메시지가 여러 번 추가될 수 있음

### 수정 방법

#### ✅ 1. useChatWebSocket cleanup 로직 수정 (필수)

**Before** (useChatWebSocket.ts:38-116):
```typescript
useEffect(() => {
  // ... 연결 설정 ...

  const waitForConnection = setInterval(() => {
    if (wsClient.isConnected() && !isSubscribedRef.current) {
      // ... 구독 ...

      // ❌ 이 return은 useEffect cleanup이 아님!
      return () => {
        unsubscribeMessages();
        unsubscribePresence();
      };
    }
  }, 100);

  return () => {
    clearInterval(waitForConnection);
    // ❌ 구독 해제가 없음
  };
}, [deps]);
```

**After** (수정 필요):
```typescript
useEffect(() => {
  if (!sessionToken) return;

  const wsClient = wsClientRef.current;

  // 구독 해제 함수를 저장할 ref
  const unsubscribersRef = { current: [] };

  // 연결 상태 리스너
  const removeStatusListener = onStatusChange
    ? wsClient.onStatusChange(onStatusChange)
    : () => {};

  // WebSocket 연결
  if (!wsClient.isConnected()) {
    wsClient.connect(sessionToken);
  }

  // 연결 대기 및 구독
  const waitForConnection = setInterval(() => {
    if (wsClient.isConnected() && !isSubscribedRef.current) {
      clearInterval(waitForConnection);
      isSubscribedRef.current = true;

      // 메시지 구독
      const unsubscribeMessages = wsClient.subscribe(
        `/topic/chat/${sessionToken}`,
        (message) => {
          // ... 메시지 처리 ...
        }
      );

      // 접속 상태 구독
      const unsubscribePresence = wsClient.subscribe(
        `/topic/chat/${sessionToken}/presence`,
        (message) => {
          // ... presence 처리 ...
        }
      );

      // ✅ 구독 해제 함수를 ref에 저장
      unsubscribersRef.current = [unsubscribeMessages, unsubscribePresence];
    }
  }, 100);

  // ✅ cleanup: 모든 리소스 정리
  return () => {
    clearInterval(waitForConnection);
    removeStatusListener();

    // ✅ 구독 해제
    unsubscribersRef.current.forEach(unsub => unsub());
    isSubscribedRef.current = false;
  };
}, [sessionToken, queryKey, onStatusChange, onPresenceChange, counterpartType, queryClient]);
```

#### ✅ 2. React Strict Mode 대응 강화 (권장)

**옵션 A**: Strict Mode 유지 + 중복 방지 로직 강화 (권장)
```typescript
// isSubscribedRef를 더 엄격하게 관리
const waitForConnection = setInterval(() => {
  if (wsClient.isConnected() && !isSubscribedRef.current) {
    // ✅ 중복 구독 방지: 먼저 플래그 설정
    isSubscribedRef.current = true;
    clearInterval(waitForConnection);

    // 구독 시작
    // ...
  }
}, 100);
```

**옵션 B**: 개발 환경에서만 Strict Mode 비활성화 (임시 방편)
```typescript
// src/main.tsx
createRoot(document.getElementById('root')!).render(
  import.meta.env.DEV ? (
    <App />  // 개발 환경: Strict Mode 비활성화
  ) : (
    <StrictMode>
      <App />  // 프로덕션: Strict Mode 유지
    </StrictMode>
  )
);
```

#### ✅ 3. WebSocketClient 연결 중복 방지 강화 (권장)

**파일**: `src/shared/api/websocket.ts:34-91`

```typescript
connect(customToken?: string): void {
  // ✅ 이미 연결 중이면 중단
  if (this.client?.connected || this.status === 'CONNECTING') {
    console.warn('[WebSocket] Already connected or connecting');
    return;
  }

  // ... 연결 로직 ...
}
```

### 체크리스트

**긴급 수정 필요** (개발 중단 수준):
- [x] `useChatWebSocket.ts` cleanup 로직 수정 (Line 37, 107, 111-120) ✅
  - [x] 구독 해제 함수를 ref에 저장 (`unsubscribersRef`) ✅
  - [x] useEffect cleanup에서 모든 구독 해제 ✅
  - [x] interval도 cleanup에서 제거 ✅
- [x] `isSubscribedRef` 중복 방지 로직 강화 ✅
  - [x] 구독 시작 전에 먼저 플래그 설정 (Line 58) ✅
- [x] WebSocketClient 연결 중복 방지 ✅
  - [x] `CONNECTING` 상태 체크 추가 (websocket.ts:36) ✅

**테스트 필수**:
- [ ] React DevTools Profiler로 컴포넌트 마운트 횟수 확인
- [ ] 콘솔에서 WebSocket 연결 로그 확인 (1개만 생성되어야 함)
- [ ] 구독 로그 확인 (`[WebSocket] Subscribed to` 로그가 1번만 출력되어야 함)
- [ ] 컴포넌트 언마운트 시 구독 해제 로그 확인
- [ ] 메모리 누수 테스트 (Chrome DevTools Memory Profiler)
- [ ] 메시지 중복 수신 여부 확인

**권장 사항**:
- [ ] React Strict Mode 대응 전략 결정 (옵션 A 권장)
- [ ] WebSocket 연결 상태 관리 개선
- [ ] 단위 테스트 작성 (useChatWebSocket 훅)

---

## 🔴 긴급 - WebSocket 403 Forbidden 에러

### 문제 상황
- 에러: `GET http://localhost:31000/api/ws/info?t=1773191265508 403 (Forbidden)`
- 발생 위치: websocket.ts:41
- 원인: SockJS 초기 HTTP 핸드셰이크(`/api/ws/info`) 요청 시 403 에러 발생

### 원인 분석
SockJS는 WebSocket 연결 전에 HTTP 핸드셰이크를 수행합니다:
1. `/api/ws/info` - SockJS 서버 정보 조회 (현재 403 발생)
2. `/api/ws/{server-id}/{session-id}/websocket` - WebSocket 연결

현재 `connectHeaders`에 Authorization 토큰을 설정하고 있지만, 이는 **STOMP 연결 시에만 적용**되고 SockJS의 HTTP 핸드셰이크에는 적용되지 않습니다.

---


## 프론트엔드 수정 완료

### ✅ sessionToken 기반 인증 처리 완료 (방법 A 적용)

**수정 내용:**

1. **WebSocketClient 수정** (websocket.ts)
   - [x] `connect(customToken?: string)` - 토큰을 파라미터로 받도록 수정
   - [x] `lastToken` 필드 추가 - 재연결 시 사용
   - [x] 재연결 시 마지막 토큰 재사용

2. **ChatPage 수정** (ChatPage.tsx:38)
   - [x] `wsClient.connect(sessionToken)` - sessionToken 전달

3. **RecruiterChatPage 수정** (RecruiterChatPage.tsx:141)
   - [x] `wsClient.connect(sessionToken)` - sessionToken 전달


# 프론트엔드 메시지 로딩 구조 개선

> 이 문서는 채팅 메시지 로딩 방식을 폴링에서 REST API + WebSocket 하이브리드 방식으로 개선하는 프론트엔드 플랜입니다.

---

## 현재 구조 분석

### 추정되는 현재 구현

**문제점**:
1. **5초 폴링 방식 사용**
   - `setInterval(() => fetchMessages(), 5000)` 형태로 추정
   - 메시지가 없어도 5초마다 API 호출
   - 불필요한 네트워크 트래픽 발생
   - 서버 부하 증가

2. **실시간성 저하**
   - 최대 5초 지연 발생
   - 사용자 경험 저하

3. **WebSocket 미활용**
   - WebSocket은 연결만 되어 있고 메시지 수신에 활용하지 않음
   - 또는 WebSocket으로 전송만 하고 수신은 폴링으로 처리

4. **중복 메시지 가능성**
   - WebSocket으로 받은 메시지를 폴링으로 다시 조회
   - 중복 렌더링 발생 가능

---

## 이상적인 메시지 로딩 흐름

```
┌─────────────────────────────────────────────────────────────┐
│                    채팅방 진입                                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
         ┌───────────────────────────┐
         │ REST API로 최근 메시지 조회 │
         │ GET /api/chat/.../messages │
         │ (초기 로드, 1회만 호출)      │
         └───────────┬───────────────┘
                     │
                     ▼
         ┌───────────────────────────┐
         │  WebSocket 구독 시작       │
         │  /topic/session/{token}   │
         └───────────┬───────────────┘
                     │
                     ▼
         ┌───────────────────────────┐
         │   실시간 메시지 수신        │
         │   (WebSocket push)        │
         │   - 새 메시지 즉시 렌더링  │
         │   - 폴링 제거 ✅           │
         └───────────────────────────┘
```

---

## 개선 플랜

### Phase 1: 초기 메시지 로드 (필수)

#### 1.1 채팅방 진입 시 REST API로 메시지 조회 (1회)

**구현 위치**: `ChatRoom.jsx` 또는 `ChatContainer.jsx`

**Before (폴링 방식 - 추정)**:
```javascript
useEffect(() => {
  // 5초마다 메시지 조회
  const intervalId = setInterval(() => {
    fetchMessages(sessionToken);
  }, 5000);

  return () => clearInterval(intervalId);
}, [sessionToken]);
```

**After (초기 로드 + WebSocket)**:
```javascript
const [messages, setMessages] = useState([]);
const [isLoading, setIsLoading] = useState(true);
const [hasError, setHasError] = useState(false);

useEffect(() => {
  // 1. 초기 메시지 로드 (1회만)
  const loadInitialMessages = async () => {
    setIsLoading(true);
    setHasError(false);

    try {
      const response = await fetch(`/api/chat/session/${sessionToken}/messages`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          // 지원자인 경우 JWT 토큰 추가
          ...(userType === 'APPLICANT' && { 'Authorization': `Bearer ${jwtToken}` })
        }
      });

      if (!response.ok) {
        throw new Error('Failed to load messages');
      }

      const data = await response.json();
      setMessages(data.data.messages || []);
    } catch (error) {
      console.error('메시지 로드 실패:', error);
      setHasError(true);
    } finally {
      setIsLoading(false);
    }
  };

  loadInitialMessages();
}, [sessionToken]); // sessionToken 변경 시에만 재로드
```

**체크리스트**:
- [x] 초기 메시지 로드 함수 구현 (`loadInitialMessages`) - useQuery 훅으로 구현됨
- [x] 로딩 상태 관리 (`isLoading`, `hasError`) - useQuery가 제공
- [x] 지원자/채용담당자 구분하여 헤더 설정 - api.ts에서 구분됨
- [x] 에러 처리 및 사용자에게 피드백 - 페이지에서 처리 중

#### 1.2 페이지네이션 구현 (무한 스크롤)

**구현 위치**: `ChatMessageList.jsx`

**구현 방식**: Intersection Observer API 활용

```javascript
const ChatMessageList = ({ sessionToken }) => {
  const [messages, setMessages] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const observerTarget = useRef(null);

  // 초기 로드 (최신 메시지 20개)
  useEffect(() => {
    loadMessages(0);
  }, [sessionToken]);

  // 무한 스크롤 (위로 스크롤 시 과거 메시지 로드)
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !isLoadingMore) {
          loadMessages(page + 1);
        }
      },
      { threshold: 1.0 }
    );

    if (observerTarget.current) {
      observer.observe(observerTarget.current);
    }

    return () => {
      if (observerTarget.current) {
        observer.unobserve(observerTarget.current);
      }
    };
  }, [page, hasMore, isLoadingMore]);

  const loadMessages = async (pageNumber) => {
    if (isLoadingMore) return;

    setIsLoadingMore(true);

    try {
      // 백엔드에서 페이지네이션 API 구현 후 사용
      const response = await fetch(
        `/api/chat/session/${sessionToken}/messages/paged?page=${pageNumber}&size=20&sort=createdAt,desc`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            ...(userType === 'APPLICANT' && { 'Authorization': `Bearer ${jwtToken}` })
          }
        }
      );

      const data = await response.json();
      const newMessages = data.data.content || [];

      setMessages((prev) => [...newMessages, ...prev]); // 과거 메시지를 앞에 추가
      setPage(pageNumber);
      setHasMore(!data.data.last); // Spring Page의 last 속성
    } catch (error) {
      console.error('메시지 로드 실패:', error);
    } finally {
      setIsLoadingMore(false);
    }
  };

  return (
    <div className="message-list">
      {/* 무한 스크롤 트리거 (맨 위) */}
      <div ref={observerTarget} style={{ height: '1px' }} />

      {isLoadingMore && <LoadingSpinner />}

      {messages.map((msg) => (
        <MessageItem key={msg.messageId} message={msg} />
      ))}
    </div>
  );
};
```

**체크리스트**:
- [ ] Intersection Observer 기반 무한 스크롤 구현 (⚠️ 백엔드 API 필요)
- [ ] 페이지네이션 상태 관리 (page, hasMore, isLoadingMore) (⚠️ 백엔드 API 필요)
- [ ] 과거 메시지 로드 시 스크롤 위치 유지 (⚠️ 백엔드 API 필요)
- [ ] 로딩 인디케이터 UI 추가 (⚠️ 백엔드 API 필요)

#### 1.3 로딩 상태 UI 표시

**구현 위치**: `ChatRoom.jsx`

```javascript
const ChatRoom = () => {
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  if (isLoading) {
    return (
      <div className="chat-loading">
        <Spinner />
        <p>메시지를 불러오는 중...</p>
      </div>
    );
  }

  if (hasError) {
    return (
      <div className="chat-error">
        <ErrorIcon />
        <p>메시지를 불러오는데 실패했습니다.</p>
        <button onClick={() => window.location.reload()}>다시 시도</button>
      </div>
    );
  }

  return (
    <div className="chat-room">
      {/* 채팅 UI */}
    </div>
  );
};
```

**체크리스트**:
- [x] 로딩 스피너 컴포넌트 구현 - Skeleton 사용 중
- [x] 에러 화면 컴포넌트 구현 - 페이지에 구현됨
- [x] 재시도 버튼 구현 - ChatPage.tsx:130, RecruiterChatPage.tsx:261

---

### Phase 2: WebSocket 구독 설정 (필수)

#### 2.1 채팅방 진입 시 즉시 구독

**구현 위치**: `useChatWebSocket.js` (커스텀 훅)

```javascript
import { useEffect, useState, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const useChatWebSocket = (sessionToken, userType, authToken) => {
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState(null);
  const stompClientRef = useRef(null);
  const subscriptionRef = useRef(null);
  const reconnectAttemptsRef = useRef(0);
  const maxReconnectAttempts = 5;

  useEffect(() => {
    if (!sessionToken) return;

    connectWebSocket();

    return () => {
      disconnectWebSocket();
    };
  }, [sessionToken]);

  const connectWebSocket = () => {
    try {
      const socket = new SockJS('/api/ws');
      const stompClient = Stomp.over(socket);

      // 디버그 로그 비활성화 (프로덕션)
      stompClient.debug = (msg) => {
        if (process.env.NODE_ENV === 'development') {
          console.log(msg);
        }
      };

      // STOMP 연결 헤더 설정
      const connectHeaders = {
        'X-Chat-Session-Token': sessionToken, // 채팅방 식별
      };

      // 사용자 타입에 따라 인증 헤더 추가
      if (userType === 'APPLICANT') {
        connectHeaders['Authorization'] = `Bearer ${authToken}`;
      } else if (userType === 'RECRUITER') {
        connectHeaders['X-Session-Token'] = authToken; // sessionToken으로 인증
      }

      stompClient.connect(
        connectHeaders,
        (frame) => {
          console.log('WebSocket 연결 성공:', frame);
          setIsConnected(true);
          setConnectionError(null);
          reconnectAttemptsRef.current = 0;

          // 메시지 구독 시작
          subscribeToMessages(stompClient);
        },
        (error) => {
          console.error('WebSocket 연결 실패:', error);
          setIsConnected(false);
          setConnectionError(error);

          // 재연결 로직
          handleReconnect();
        }
      );

      stompClientRef.current = stompClient;
    } catch (error) {
      console.error('WebSocket 초기화 실패:', error);
      setConnectionError(error);
    }
  };

  const subscribeToMessages = (stompClient) => {
    const subscription = stompClient.subscribe(
      `/topic/session/${sessionToken}`,
      (message) => {
        const newMessage = JSON.parse(message.body);
        console.log('새 메시지 수신:', newMessage);

        // 부모 컴포넌트로 메시지 전달
        if (onMessageReceived) {
          onMessageReceived(newMessage);
        }
      }
    );

    subscriptionRef.current = subscription;
  };

  const handleReconnect = () => {
    if (reconnectAttemptsRef.current >= maxReconnectAttempts) {
      console.error('최대 재연결 시도 횟수 초과');
      return;
    }

    const delay = Math.min(1000 * Math.pow(2, reconnectAttemptsRef.current), 30000);
    console.log(`${delay}ms 후 재연결 시도 (${reconnectAttemptsRef.current + 1}/${maxReconnectAttempts})`);

    setTimeout(() => {
      reconnectAttemptsRef.current += 1;
      connectWebSocket();
    }, delay);
  };

  const disconnectWebSocket = () => {
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    if (stompClientRef.current) {
      stompClientRef.current.disconnect(() => {
        console.log('WebSocket 연결 해제');
      });
      stompClientRef.current = null;
    }

    setIsConnected(false);
  };

  const sendMessage = (content, senderType) => {
    if (!stompClientRef.current || !stompClientRef.current.connected) {
      console.error('WebSocket이 연결되지 않았습니다');
      return false;
    }

    try {
      stompClientRef.current.send(
        `/app/chat/${sessionToken}`,
        {},
        JSON.stringify({
          sessionToken,
          senderType,
          content
        })
      );
      return true;
    } catch (error) {
      console.error('메시지 전송 실패:', error);
      return false;
    }
  };

  return {
    isConnected,
    connectionError,
    sendMessage
  };
};

export default useChatWebSocket;
```

**체크리스트**:
- [x] SockJS + STOMP 연결 구현 - WebSocketClient 클래스 (websocket.ts)
- [x] 지원자/채용담당자 구분 인증 헤더 설정 - connectHeaders에 Authorization 토큰 포함
- [x] `/topic/chat/{sessionToken}` 구독 - useChatWebSocket 훅에서 처리
- [x] 연결 상태 관리 (isConnected, connectionError) - ConnectionStatus 타입으로 관리

#### 2.2 연결 실패 시 재시도 로직

**위 코드의 `handleReconnect()` 참조**

**재연결 전략**:
- Exponential Backoff 방식 (1초 → 2초 → 4초 → 8초 → 16초 → 30초 max)
- 최대 5회 시도
- 5회 실패 시 사용자에게 수동 재연결 안내

**체크리스트**:
- [x] Exponential Backoff 재연결 로직 구현 - websocket.ts:185 (1s→2s→4s→8s→16s→30s)
- [x] 최대 재연결 시도 횟수 제한 - 최대 5회 (websocket.ts:19)
- [x] 재연결 상태 UI 표시 - 페이지에서 connectionStatus로 표시 중

---

### Phase 3: 실시간 메시지 수신 (필수)

#### 3.1 WebSocket으로 받은 메시지 즉시 UI에 추가

**구현 위치**: `ChatRoom.jsx`

```javascript
const ChatRoom = ({ sessionToken, userType, authToken }) => {
  const [messages, setMessages] = useState([]);
  const messageIdsRef = useRef(new Set()); // 중복 방지용

  // WebSocket 훅 사용
  const { isConnected, connectionError, sendMessage } = useChatWebSocket(
    sessionToken,
    userType,
    authToken
  );

  // 초기 메시지 로드
  useEffect(() => {
    loadInitialMessages();
  }, [sessionToken]);

  // WebSocket 메시지 수신 핸들러
  const handleMessageReceived = (newMessage) => {
    // 중복 메시지 필터링
    if (messageIdsRef.current.has(newMessage.messageId)) {
      console.log('중복 메시지 무시:', newMessage.messageId);
      return;
    }

    // 메시지 추가
    setMessages((prev) => [...prev, newMessage]);
    messageIdsRef.current.add(newMessage.messageId);

    // 자동 스크롤 (맨 아래로)
    scrollToBottom();
  };

  const loadInitialMessages = async () => {
    // ... (Phase 1.1 참조)
    const data = await fetchMessages();

    // 초기 메시지 ID를 Set에 추가 (중복 방지)
    data.messages.forEach((msg) => {
      messageIdsRef.current.add(msg.messageId);
    });

    setMessages(data.messages);
  };

  const scrollToBottom = () => {
    // 부드러운 스크롤
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div className="chat-room">
      <ChatHeader />

      <ChatMessageList
        messages={messages}
        onMessageReceived={handleMessageReceived}
      />

      <ChatInput
        onSendMessage={sendMessage}
        disabled={!isConnected}
      />

      {/* 연결 상태 표시 */}
      {!isConnected && (
        <div className="connection-status warning">
          연결 끊김. 재연결 중...
        </div>
      )}
    </div>
  );
};
```

**체크리스트**:
- [ ] WebSocket 메시지 수신 핸들러 구현
- [ ] 새 메시지 즉시 렌더링
- [ ] 자동 스크롤 (맨 아래로)
- [ ] 메시지 애니메이션 효과 (선택)

#### 3.2 중복 메시지 필터링 (messageId 기반)

**구현 방식**: Set 자료구조 활용 (O(1) 조회)

```javascript
const messageIdsRef = useRef(new Set());

const addMessage = (newMessage) => {
  // 중복 체크
  if (messageIdsRef.current.has(newMessage.messageId)) {
    return; // 중복 메시지 무시
  }

  // 메시지 추가
  setMessages((prev) => [...prev, newMessage]);
  messageIdsRef.current.add(newMessage.messageId);
};

// 초기 로드 시에도 ID 등록
const loadInitialMessages = async () => {
  const data = await fetchMessages();

  data.messages.forEach((msg) => {
    messageIdsRef.current.add(msg.messageId);
  });

  setMessages(data.messages);
};
```

**체크리스트**:
- [ ] Set 기반 중복 체크 구현
- [ ] 초기 메시지 로드 시 ID 등록
- [ ] WebSocket 메시지 수신 시 중복 검증

#### 3.3 메시지 정렬 유지

**정렬 기준**: `sentAt` (ISO-8601 타임스탬프)

```javascript
const addMessage = (newMessage) => {
  if (messageIdsRef.current.has(newMessage.messageId)) {
    return;
  }

  setMessages((prev) => {
    const updated = [...prev, newMessage];

    // sentAt 기준 오름차순 정렬 (오래된 메시지가 위)
    updated.sort((a, b) => new Date(a.sentAt) - new Date(b.sentAt));

    return updated;
  });

  messageIdsRef.current.add(newMessage.messageId);
};
```

**체크리스트**:
- [ ] sentAt 기준 정렬 구현
- [ ] 시간대 처리 (UTC → 로컬 타임)
- [ ] 정렬 성능 최적화 (필요 시 이진 탐색 삽입)

---

### Phase 4: 폴링 제거 (필수)

#### 4.1 5초 폴링 로직 제거

**Before**:
```javascript
useEffect(() => {
  const intervalId = setInterval(() => {
    fetchMessages(sessionToken);
  }, 5000);

  return () => clearInterval(intervalId);
}, [sessionToken]);
```

**After**:
```javascript
// 폴링 완전 제거
// 초기 로드만 수행
useEffect(() => {
  loadInitialMessages();
}, [sessionToken]);

// WebSocket으로 실시간 수신
const { isConnected } = useChatWebSocket(sessionToken, userType, authToken);
```

**체크리스트**:
- [x] `setInterval` 기반 폴링 코드 제거 - useRecruiterMessages의 refetchInterval 제거됨 (hooks.ts:17)
- [x] WebSocket 기반 실시간 수신으로 대체 - 이미 구현되어 있음
- [x] 불필요한 API 호출 제거 확인 - 초기 로드만 수행, 이후 WebSocket으로 실시간 수신

#### 4.2 WebSocket 연결 끊김 시에만 REST API fallback

**구현 위치**: `useChatWebSocket.js`

```javascript
const useChatWebSocket = (sessionToken, userType, authToken, onConnectionLost) => {
  const [isConnected, setIsConnected] = useState(false);
  const [fallbackMode, setFallbackMode] = useState(false);

  const handleDisconnect = (error) => {
    console.error('WebSocket 연결 끊김:', error);
    setIsConnected(false);

    // 재연결 시도
    handleReconnect();

    // 재연결 실패 시 fallback 모드
    if (reconnectAttemptsRef.current >= maxReconnectAttempts) {
      setFallbackMode(true);

      // 부모 컴포넌트에 알림
      if (onConnectionLost) {
        onConnectionLost();
      }
    }
  };

  return {
    isConnected,
    fallbackMode
  };
};
```

**ChatRoom에서 fallback 처리**:
```javascript
const ChatRoom = () => {
  const [fallbackPolling, setFallbackPolling] = useState(false);

  const { isConnected, fallbackMode } = useChatWebSocket(
    sessionToken,
    userType,
    authToken,
    () => setFallbackPolling(true)
  );

  // Fallback 폴링 (연결 끊김 시에만)
  useEffect(() => {
    if (!fallbackPolling) return;

    const intervalId = setInterval(() => {
      fetchMessages(sessionToken);
    }, 10000); // 10초 간격으로 폴링

    return () => clearInterval(intervalId);
  }, [fallbackPolling]);

  return (
    <div>
      {fallbackPolling && (
        <div className="fallback-warning">
          실시간 연결이 끊겼습니다. 폴링 모드로 전환되었습니다.
        </div>
      )}
      {/* ... */}
    </div>
  );
};
```

**체크리스트**:
- [ ] 연결 끊김 감지 로직 구현
- [ ] fallback 폴링 모드 구현 (10초 간격)
- [ ] 사용자에게 연결 상태 알림

---

### Phase 5: 에러 처리 및 사용자 경험 개선 (권장)

#### 5.1 WebSocket 연결 실패 시 폴링 fallback

**위 Phase 4.2 참조**

#### 5.2 재연결 로직 구현

**위 Phase 2.2 참조**

**추가 개선사항**:
- 재연결 시도 중 UI 표시
- 재연결 성공 시 알림
- 수동 재연결 버튼 제공

```javascript
const ReconnectButton = ({ onReconnect }) => {
  const [isReconnecting, setIsReconnecting] = useState(false);

  const handleReconnect = async () => {
    setIsReconnecting(true);
    await onReconnect();
    setIsReconnecting(false);
  };

  return (
    <button
      onClick={handleReconnect}
      disabled={isReconnecting}
      className="reconnect-button"
    >
      {isReconnecting ? '재연결 중...' : '다시 연결'}
    </button>
  );
};
```

#### 5.3 사용자에게 연결 상태 표시

**구현 위치**: `ConnectionStatus.jsx`

```javascript
const ConnectionStatus = ({ isConnected, connectionError, fallbackMode }) => {
  if (isConnected) {
    return (
      <div className="connection-status success">
        <span className="status-icon">●</span>
        <span>연결됨</span>
      </div>
    );
  }

  if (fallbackMode) {
    return (
      <div className="connection-status warning">
        <span className="status-icon">⚠</span>
        <span>일시적 연결 끊김 (폴링 모드)</span>
      </div>
    );
  }

  return (
    <div className="connection-status error">
      <span className="status-icon">●</span>
      <span>연결 끊김 - 재연결 중...</span>
    </div>
  );
};
```

**CSS 스타일**:
```css
.connection-status {
  position: fixed;
  top: 10px;
  right: 10px;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 1000;
}

.connection-status.success {
  background-color: #d4edda;
  color: #155724;
}

.connection-status.warning {
  background-color: #fff3cd;
  color: #856404;
}

.connection-status.error {
  background-color: #f8d7da;
  color: #721c24;
}

.status-icon {
  font-size: 8px;
}
```

**체크리스트**:
- [ ] 연결 상태 UI 컴포넌트 구현
- [ ] 연결/끊김/재연결 중 상태 표시
- [ ] 애니메이션 효과 추가 (선택)

---

## 구현 우선순위 및 순서

### Phase 1: 기본 구조 (필수) - 예상 소요: 4-6시간
1. [1.1] 초기 메시지 로드 구현 (2시간)
   - REST API 호출 로직
   - 로딩 상태 관리
   - 에러 처리
2. [1.2] 페이지네이션 (무한 스크롤) (2시간)
   - Intersection Observer 구현
   - 과거 메시지 로드
3. [1.3] 로딩 UI 구현 (1시간)

### Phase 2: WebSocket 연결 (필수) - ✅ 완료
1. [2.1] WebSocket 구독 설정 ✅
   - useChatWebSocket 커스텀 훅 구현 (src/shared/hooks/useChatWebSocket.ts)
   - 인증 헤더 설정 (websocket.ts:45)
   - 구독 로직 구현 (useChatWebSocket에서 자동 처리)
2. [2.2] 재연결 로직 ✅
   - Exponential Backoff (websocket.ts:185)
   - 최대 5회 재시도
   - 수동 재연결 메서드 추가 (manualReconnect)

### Phase 3: 실시간 수신 (필수) - 예상 소요: 2-3시간
1. [3.1] 메시지 수신 핸들러 구현 (1시간)
2. [3.2] 중복 필터링 로직 (1시간)
3. [3.3] 메시지 정렬 유지 (30분)

### Phase 4: 폴링 제거 (필수) - 예상 소요: 1-2시간
1. [4.1] 폴링 코드 제거 (30분)
2. [4.2] Fallback 폴링 구현 (1시간)

### Phase 5: UX 개선 (권장) - 예상 소요: 2-3시간
1. [5.1] Fallback 모드 구현 (1시간)
2. [5.2] 재연결 UI 구현 (1시간)
3. [5.3] 연결 상태 표시 UI (1시간)

---

## 기대 효과

### 성능 개선
- **네트워크 트래픽 감소**: 폴링 제거로 불필요한 API 호출 99% 감소
- **서버 부하 감소**: 초당 요청 수 대폭 감소
- **클라이언트 리소스 절약**: 5초마다 렌더링 → 메시지 수신 시에만 렌더링

### 사용자 경험 개선
- **실시간성 향상**: 5초 지연 → 즉시 수신 (< 100ms)
- **부드러운 UI**: 폴링으로 인한 깜빡임 제거
- **안정성 향상**: 재연결 로직으로 연결 안정성 확보

---

## 테스트 체크리스트

### 기능 테스트
- [ ] 초기 메시지 로드 정상 동작
- [ ] WebSocket 연결 성공
- [ ] 새 메시지 실시간 수신
- [ ] 중복 메시지 필터링 동작
- [ ] 메시지 정렬 유지

### 에러 시나리오 테스트
- [ ] WebSocket 연결 실패 시 재연결
- [ ] 최대 재연결 시도 후 fallback 폴링
- [ ] 네트워크 일시 중단 후 복구
- [ ] 서버 재시작 후 재연결

### 성능 테스트
- [ ] 1000개 메시지 로드 시 성능
- [ ] 무한 스크롤 부드러움 확인
- [ ] 메모리 누수 확인 (DevTools Memory Profiler)

### 크로스 브라우저 테스트
- [ ] Chrome (최신)
- [ ] Firefox (최신)
- [ ] Safari (최신)
- [ ] Edge (최신)

---

## 참고 사항

### 백엔드 의존성
이 개선은 백엔드 fix.md의 다음 기능을 활용합니다:
- WebSocket 브로드캐스트: `/topic/session/{sessionToken}` ✅
- REST API 메시지 조회: `GET /api/chat/session/{sessionToken}/messages` ✅
- 페이지네이션 API (백엔드 구현 필요): `GET /api/chat/session/{sessionToken}/messages/paged`

### 라이브러리 요구사항
```json
{
  "dependencies": {
    "sockjs-client": "^1.6.1",
    "@stomp/stompjs": "^7.0.0"
  }
}
```

### 환경 변수
```env
REACT_APP_WS_ENDPOINT=/api/ws
REACT_APP_API_BASE_URL=http://localhost:7777
```

---

## 프로덕션 체크리스트

배포 전 확인사항:
- [ ] 폴링 코드 완전 제거 확인
- [ ] WebSocket 연결 안정성 검증
- [ ] 에러 처리 로직 검증
- [ ] 재연결 로직 검증
- [ ] 중복 메시지 필터링 검증
- [ ] 메모리 누수 테스트
- [ ] 크로스 브라우저 테스트
- [ ] 모바일 환경 테스트
- [ ] 백엔드 API 엔드포인트 확인

---

**작성일**: 2026-03-11
**작성자**: Claude (Backend Architect)
**관련 문서**: [fix.md](./fix.md) (백엔드 개선 플랜)
