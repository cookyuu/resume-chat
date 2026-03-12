# Resume Chat - 개발 계획 (Development Plan)

## 문서 개요

이 문서는 Resume Chat 프로젝트의 단계별 개발 계획을 정의합니다. 각 Phase는 이전 Phase에서 구현된 기능을 기반으로 점진적으로 기능을 추가하는 방식으로 구성되어 있습니다.

**관련 문서**: [spec.md](./spec.md) - 전체 기능 명세 및 기술 스택

---

## 개발 진행 상황

| Phase | 설명 | 상태 | 예상 기간 | 완료일        |
|-------|------|------|-----------|------------|
| Phase 0 | 프로젝트 초기 설정 | ✅ 완료 | 1주 | 2026-02-01 |
| Phase 1 | MVP - 핵심 기능 | ✅ 완료 | 3주 | 2026-02-14 |
| Phase 2 | 실시간 통신 & 알림 | 🔄 진행 예정 | 2-3주 | -          |
| Phase 3 | Apple UX/UI 강화 | 📋 계획 | 2주 | -          |
| Phase 4 | 파일 & 검색 | 📋 계획 | 2주 | -          |
| Phase 5 | 보안 강화 | 📋 계획 | 1-2주 | -          |
| Phase 6 | 분석 & 통계 | 📋 계획 | 1주 | -          |
| Phase 7 | 고급 기능 | 📋 계획 | 3-4주 | -          |
| Phase 8 | 모바일 앱 | 📋 계획 | 4-6주 | -          |

---

## Phase 0: 프로젝트 초기 설정 ✅

**목표**: 개발 환경 구축 및 기본 인프라 구성

### 구현 내용

#### Backend
- [x] Spring Boot 3.2.5 프로젝트 생성
- [x] Gradle 빌드 설정
- [x] MySQL 데이터베이스 설정
- [x] Spring Data JPA 설정
- [x] Spring Security 기본 설정
- [x] Swagger/OpenAPI 설정
- [x] CORS 설정
- [x] 에러 핸들링 구조 (ApiResponse, ErrorCode)

#### Frontend
- [x] React 19 + TypeScript + Vite 프로젝트 생성
- [x] Tailwind CSS 설정
- [x] React Router DOM 설정
- [x] Zustand 상태 관리 설정
- [x] Tanstack Query 설정
- [x] Axios 클라이언트 설정
- [x] Feature-Sliced Design 디렉토리 구조

#### Database
```sql
-- 기본 테이블 생성
CREATE TABLE rc_applicant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    login_fail_cnt INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE rc_resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_slug VARCHAR(36) UNIQUE NOT NULL,
    applicant_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    file_path VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255),
    view_cnt INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (applicant_id) REFERENCES rc_applicant(id)
);

CREATE TABLE rc_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_token VARCHAR(36) UNIQUE NOT NULL,
    resume_id BIGINT NOT NULL,
    recruiter_name VARCHAR(100) NOT NULL,
    recruiter_email VARCHAR(255) NOT NULL,
    recruiter_company VARCHAR(200) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    total_messages BIGINT DEFAULT 0,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (resume_id) REFERENCES rc_resume(id),
    UNIQUE KEY uk_resume_email (resume_id, recruiter_email),
    INDEX idx_session_token (session_token)
);

CREATE TABLE rc_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(36) UNIQUE NOT NULL,
    session_id BIGINT NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    read_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES rc_chat_session(id)
);
```

### 기술 스택
- **Backend**: Spring Boot 3.2.5, Java 17, MySQL, JPA
- **Frontend**: React 19, TypeScript 5.7, Vite 6, Tailwind CSS 3.4
- **Build**: Gradle (Backend), npm (Frontend)

### 완료 기준
- [x] 백엔드 서버 실행 가능 (http://localhost:8080)
- [x] 프론트엔드 개발 서버 실행 가능 (http://localhost:31000)
- [x] 데이터베이스 연결 정상
- [x] Swagger UI 접근 가능

---

## Phase 1: MVP - 핵심 기능 ✅

**목표**: 최소 기능 제품 (MVP) 구현 - 기본적인 이력서 공유 및 채팅 기능

### 구현 내용

#### Backend API

**인증 API** (`/api/applicant`):
- [x] `POST /join` - 회원가입
- [x] `POST /login` - 로그인 (JWT 발급)
- [x] `GET /profile` - 프로필 조회

**이력서 API** (`/api/applicant/resume`):
- [x] `POST /` - 이력서 업로드 (multipart/form-data)
- [x] `GET /` - 내 이력서 목록 조회
- [x] `DELETE /{resumeSlug}` - 이력서 삭제

**채팅 API** (`/api/chat`, `/api/applicant`):
- [x] `POST /chat/{resumeSlug}/enter` - 채용담당자 세션 진입
- [x] `POST /chat/{resumeSlug}/send` - 채용담당자 메시지 전송 (세션 생성)
- [x] `POST /chat/session/{sessionToken}/send` - 채용담당자 메시지 전송
- [x] `GET /chat/session/{sessionToken}/messages` - 채용담당자 메시지 조회
- [x] `GET /applicant/resume/{resumeSlug}/chats` - 지원자 세션 목록 조회
- [x] `GET /applicant/chat/{sessionToken}/messages` - 지원자 메시지 조회
- [x] `POST /applicant/chat/{sessionToken}/send` - 지원자 메시지 전송

#### Backend 서비스 로직
```java
// 핵심 구현 사항
- JWT 인증 (Access Token 1시간, Refresh Token 7일)
- BCrypt 비밀번호 암호화
- 로그인 실패 5회 시 계정 잠금
- 파일 업로드 (PDF, 최대 10MB)
- 세션 고유성 보장 (resume + email)
- Cascade 삭제 (이력서 → 세션 → 메시지)
```

#### Frontend 페이지

**인증 페이지**:
- [x] `/join` - 회원가입 페이지
  - 이메일, 이름, 비밀번호, 비밀번호 확인
  - 실시간 유효성 검사
- [x] `/login` - 로그인 페이지
  - 이메일, 비밀번호
  - JWT 토큰 저장 (Zustand)

**이력서 페이지**:
- [x] `/resumes` - 이력서 목록 페이지
  - 테이블 형식 (제목, 파일명, 조회수, 날짜)
  - 업로드 폼 (토글)
  - 채팅 링크 복사
  - 삭제 버튼

**채팅 페이지**:
- [x] `/resumes/{resumeSlug}/chats` - 채팅 세션 목록
  - 채용담당자별 세션 카드
  - 읽지 않은 메시지 수 뱃지
- [x] `/chat/{sessionToken}` - 채팅방 (지원자)
  - iMessage 스타일 UI
  - 본인 메시지: 오른쪽, Blue 600
  - 상대방 메시지: 왼쪽, White
  - 자동 스크롤
- [x] `/chat/{resumeSlug}/recruiter` - 채팅방 (채용담당자)
  - Public 접근 (인증 불필요)
  - 첫 방문 시 정보 입력 폼

#### Frontend UI 컴포넌트
```tsx
// /shared/ui/
- Button: variant (primary, secondary, danger), loading
- Input: label, error, forwardRef
- Skeleton: Pulse 애니메이션
- EmptyState: 빈 상태 표시
```

#### Frontend 상태 관리
```tsx
// Zustand
- authStore: user, accessToken, setAuth, logout

// Tanstack Query
- useJoin, useLogin (auth)
- useMyResumes, useUploadResume, useDeleteResume (resume)
- useResumeChats, useSessionMessages (chat)
- useSendApplicantMessage, useSendRecruiterMessage (chat)
```

### 기술 구현

**JWT 인증**:
```java
// JwtTokenProvider.java
- generateAccessToken(uuid): 1시간
- generateRefreshToken(uuid): 7일
- validateToken(token): boolean
- getUuidFromToken(token): UUID

// JwtAuthenticationFilter.java
- Authorization 헤더에서 토큰 추출
- 토큰 검증 후 SecurityContext에 인증 정보 설정
```

**파일 업로드**:
```java
// FileStorageService.java
- storeFile(MultipartFile): String (저장 경로)
- validateFile(MultipartFile): void
- 허용 확장자: .pdf
- 최대 크기: 10MB
- 저장 위치: ./uploads/resumes/
```

**메시지 읽음 처리**:
```java
// ChatService.java
public ChatDetailResponse getSessionMessages(UUID applicantUuid, String sessionToken) {
    // 1. 권한 확인
    // 2. 메시지 조회
    // 3. 읽지 않은 메시지 자동 읽음 처리
    messages.forEach(msg -> {
        if (!msg.isReadStatus()) {
            msg.markAsRead();
        }
    });
}
```

### 완료 기준
- [x] 회원가입 → 로그인 → 이력서 업로드 플로우 동작
- [x] 이력서 링크 복사 가능
- [x] 채용담당자가 링크로 접근하여 메시지 전송 가능
- [x] 지원자가 메시지 수신 및 답장 가능
- [x] 채팅 UI가 iMessage 스타일로 표시
- [x] 모든 API 테스트 통과

### 예상 기간
**3주** (완료)

---

## Phase 2: 실시간 통신 & 알림 🔄

**목표**: WebSocket 기반 실시간 채팅 및 알림 시스템 구축

**우선순위**: 높음
**예상 기간**: 2-3주

### 구현 내용

#### 2.1 WebSocket 실시간 채팅

**Backend - WebSocket 설정**:
```java
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
}

// WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();
    }
}

// ChatWebSocketController.java
@Controller
public class ChatWebSocketController {
    @MessageMapping("/chat/{sessionToken}")
    @SendTo("/topic/session/{sessionToken}")
    public ChatMessageDto sendMessage(
            @DestinationVariable String sessionToken,
            ChatMessageDto message
    ) {
        // 메시지 저장
        // 브로드캐스트
        return message;
    }
}
```

**Frontend - WebSocket 클라이언트**:
```bash
npm install @stomp/stompjs sockjs-client
```

```tsx
// /shared/api/websocket.ts
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function createWebSocketClient(sessionToken: string) {
  const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/session/${sessionToken}`, (message) => {
        const data = JSON.parse(message.body);
        // 메시지 수신 처리
      });
    },
  });

  client.activate();
  return client;
}

// ChatPage.tsx에서 사용
useEffect(() => {
  const client = createWebSocketClient(sessionToken);
  return () => client.deactivate();
}, [sessionToken]);
```

**API 변경**:
- 기존 HTTP 폴링 방식 유지 (호환성)
- WebSocket 연결 시 실시간 수신
- 연결 끊김 시 자동 재연결

#### 2.2 입력 중 표시 (Typing Indicator)

**Backend**:
```java
// TypingEvent.java
public class TypingEvent {
    private String sessionToken;
    private String senderName;
    private boolean typing;
}

// ChatWebSocketController.java
@MessageMapping("/chat/{sessionToken}/typing")
@SendTo("/topic/session/{sessionToken}/typing")
public TypingEvent handleTyping(@DestinationVariable String sessionToken, TypingEvent event) {
    return event;
}
```

**Frontend**:
```tsx
// useTypingIndicator.ts
export function useTypingIndicator(client: Client, sessionToken: string) {
  const [isTyping, setIsTyping] = useState(false);
  const timeoutRef = useRef<NodeJS.Timeout>();

  const sendTyping = (typing: boolean) => {
    client.publish({
      destination: `/app/chat/${sessionToken}/typing`,
      body: JSON.stringify({ typing, senderName: user.name }),
    });
  };

  const handleInputChange = () => {
    sendTyping(true);
    clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => sendTyping(false), 3000);
  };

  return { isTyping, handleInputChange };
}

// UI: "상대방이 입력 중..." (3초 후 자동 사라짐)
```

#### 2.3 온라인 상태 표시

**Backend**:
```java
// PresenceService.java
@Service
public class PresenceService {
    private final Map<String, Set<String>> onlineUsers = new ConcurrentHashMap<>();
    // sessionToken -> Set<userId>

    public void userConnected(String sessionToken, String userId) {
        onlineUsers.computeIfAbsent(sessionToken, k -> new HashSet<>()).add(userId);
        broadcastPresence(sessionToken);
    }

    public void userDisconnected(String sessionToken, String userId) {
        Set<String> users = onlineUsers.get(sessionToken);
        if (users != null) {
            users.remove(userId);
            broadcastPresence(sessionToken);
        }
    }
}
```

**Frontend**:
```tsx
// 온라인 상태 표시
<div className="flex items-center gap-2">
  <div className={`w-2 h-2 rounded-full ${isOnline ? 'bg-green-500' : 'bg-gray-300'}`} />
  <span className="text-xs text-gray-500">
    {isOnline ? '온라인' : lastSeen ? `마지막 접속: ${formatRelative(lastSeen)}` : '오프라인'}
  </span>
</div>
```

#### 2.4 이메일 알림

**Backend - Spring Mail 설정**:
```java
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-mail'
}

// application.yml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

// EmailService.java
@Service
public class EmailService {
    @Async
    public void sendNewMessageNotification(Applicant applicant, ChatSession session, ChatMessage message) {
        String subject = "[Resume Chat] 새 메시지가 도착했습니다";
        String body = String.format("""
            안녕하세요, %s님

            %s (%s)님으로부터 새 메시지가 도착했습니다.

            메시지 미리보기: "%s"

            채팅 확인하기: %s
            """,
            applicant.getName(),
            session.getRecruiterName(),
            session.getRecruiterCompany(),
            truncate(message.getContent(), 100),
            generateChatLink(session.getSessionToken())
        );

        sendEmail(applicant.getEmail(), subject, body);
    }
}

// ChatService.java - 메시지 전송 시 알림
@Transactional
public void sendMessage(...) {
    // 메시지 저장
    chatMessageRepository.save(message);

    // 이메일 알림 전송 (비동기)
    emailService.sendNewMessageNotification(applicant, session, message);
}
```

**이메일 템플릿** (HTML):
```html
<!-- email-template.html -->
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .button { background: #2563EB; color: white; padding: 12px 24px;
                  text-decoration: none; border-radius: 8px; }
    </style>
</head>
<body>
    <div class="container">
        <h2>새 메시지가 도착했습니다</h2>
        <p>채용담당자: <strong>{{recruiterName}}</strong> ({{recruiterCompany}})</p>
        <p>메시지: "{{messagePreview}}"</p>
        <a href="{{chatLink}}" class="button">채팅 확인하기</a>
    </div>
</body>
</html>
```

#### 2.5 브라우저 푸시 알림

**Backend - Web Push 설정**:
```java
// build.gradle
dependencies {
    implementation 'nl.martijndwars:web-push:5.1.1'
}

// WebPushService.java
@Service
public class WebPushService {
    private final PushService pushService;

    public void sendNotification(String endpoint, String publicKey, String auth, String payload) {
        Notification notification = new Notification(endpoint, publicKey, auth, payload);
        pushService.send(notification);
    }
}

// PushSubscription Entity
@Entity
public class PushSubscription {
    private Long id;
    private UUID applicantUuid;
    private String endpoint;
    private String publicKey;
    private String auth;
}
```

**Frontend - Service Worker**:
```tsx
// public/sw.js
self.addEventListener('push', (event) => {
  const data = event.data.json();
  const options = {
    body: data.body,
    icon: '/logo.png',
    badge: '/badge.png',
    vibrate: [200, 100, 200],
    data: { url: data.url },
  };

  event.waitUntil(
    self.registration.showNotification(data.title, options)
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(clients.openWindow(event.notification.data.url));
});

// /shared/hooks/usePushNotification.ts
export function usePushNotification() {
  const subscribe = async () => {
    const registration = await navigator.serviceWorker.ready;
    const subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY),
    });

    // 구독 정보를 서버에 전송
    await api.post('/api/push/subscribe', subscription);
  };

  return { subscribe };
}
```

#### 2.6 알림 설정 페이지

**Database 스키마 추가**:
```sql
CREATE TABLE rc_notification_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    applicant_id BIGINT NOT NULL,
    email_new_message BOOLEAN DEFAULT TRUE,
    email_new_session BOOLEAN DEFAULT TRUE,
    push_new_message BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (applicant_id) REFERENCES rc_applicant(id),
    UNIQUE KEY uk_applicant (applicant_id)
);
```

**API**:
- `GET /api/applicant/notification-settings` - 알림 설정 조회
- `PUT /api/applicant/notification-settings` - 알림 설정 변경

**Frontend 페이지**:
```tsx
// /pages/settings/NotificationSettingsPage.tsx
- 이메일 알림 on/off (새 메시지, 새 세션)
- 푸시 알림 on/off
- 알림음 on/off
- 방해금지 시간대 설정
```

### 기술 스택
- **WebSocket**: Spring WebSocket, STOMP, SockJS
- **Frontend**: @stomp/stompjs, sockjs-client
- **Email**: Spring Mail, Gmail SMTP
- **Push**: Web Push API, Service Worker, VAPID

### 완료 기준
- [ ] 메시지 전송 시 실시간으로 상대방 화면에 표시
- [ ] 입력 중일 때 "입력 중..." 표시
- [ ] 온라인/오프라인 상태 실시간 업데이트
- [ ] 새 메시지 수신 시 이메일 알림 발송
- [ ] 브라우저 푸시 알림 동작
- [ ] 알림 설정 페이지에서 on/off 가능
- [ ] WebSocket 연결 끊김 시 자동 재연결

### 의존성
- Phase 1 완료 필수

---

## Phase 3: Apple UX/UI 강화 📋

**목표**: Apple 디자인 가이드라인에 따른 사용자 경험 극대화

**우선순위**: 중간
**예상 기간**: 2주

### 구현 내용

#### 3.1 애니메이션 시스템

**Framer Motion 도입**:
```bash
npm install framer-motion
```

**Tailwind 커스텀 설정**:
```js
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      transitionTimingFunction: {
        'ios': 'cubic-bezier(0.42, 0, 0.58, 1)',
        'spring': 'cubic-bezier(0.175, 0.885, 0.32, 1.275)',
      },
      transitionDuration: {
        '150': '150ms',
        '250': '250ms',
        '350': '350ms',
      },
      animation: {
        'shimmer': 'shimmer 2s infinite',
      },
      keyframes: {
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      },
    },
  },
};
```

**페이지 전환 애니메이션**:
```tsx
// /app/layouts/PageTransition.tsx
import { motion } from 'framer-motion';

export function PageTransition({ children }: { children: React.ReactNode }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      transition={{ duration: 0.3, ease: [0.42, 0, 0.58, 1] }}
    >
      {children}
    </motion.div>
  );
}
```

**버튼 마이크로 인터랙션**:
```tsx
<motion.button
  whileHover={{ scale: 1.02 }}
  whileTap={{ scale: 0.98 }}
  transition={{ duration: 0.15 }}
  className="transition-all duration-150 ease-ios"
>
```

#### 3.2 Heroicons 통합

```bash
npm install @heroicons/react
```

**아이콘 교체**:
```tsx
// Before: SVG 직접 작성
<svg>...</svg>

// After: Heroicons 사용
import { PaperAirplaneIcon } from '@heroicons/react/24/outline';
<PaperAirplaneIcon className="w-5 h-5" />

// 아이콘 목록
- PaperAirplaneIcon: 전송
- LinkIcon: 링크 복사
- TrashIcon: 삭제
- ArrowUpTrayIcon: 업로드
- ChatBubbleLeftIcon: 채팅
- DocumentTextIcon: 문서
- MagnifyingGlassIcon: 검색
- Cog6ToothIcon: 설정
- BellIcon: 알림
- UserIcon: 프로필
```

#### 3.3 다크 모드

**Tailwind 다크 모드 설정**:
```js
// tailwind.config.js
module.exports = {
  darkMode: 'class',
  // ...
};
```

**다크 모드 훅**:
```tsx
// /shared/hooks/useDarkMode.ts
export function useDarkMode() {
  const [isDark, setIsDark] = useState(() => {
    const saved = localStorage.getItem('theme');
    if (saved) return saved === 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  });

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  }, [isDark]);

  return { isDark, toggle: () => setIsDark(!isDark) };
}
```

**컴포넌트 다크 모드 적용**:
```tsx
// 배경
<div className="bg-white dark:bg-gray-900">

// 텍스트
<p className="text-gray-900 dark:text-white">

// 카드
<div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">

// 입력 필드
<input className="bg-white dark:bg-gray-800 text-gray-900 dark:text-white" />
```

**다크 모드 토글 버튼**:
```tsx
// Header에 추가
import { MoonIcon, SunIcon } from '@heroicons/react/24/outline';

<button onClick={toggle} className="p-2">
  {isDark ? <SunIcon className="w-5 h-5" /> : <MoonIcon className="w-5 h-5" />}
</button>
```

#### 3.4 Backdrop Blur

```tsx
// 네비게이션 (스크롤 시 적용)
<header className="sticky top-0 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm">

// 모달 오버레이
<div className="fixed inset-0 bg-black/50 backdrop-blur-sm">

// 입력창 (iOS 키보드 위)
<div className="bg-white/95 dark:bg-gray-900/95 backdrop-blur-md">
```

#### 3.5 Toast 커스터마이징

```tsx
// /app/providers.tsx
import { Toaster } from 'react-hot-toast';

<Toaster
  position="top-center"
  toastOptions={{
    duration: 3000,
    style: {
      background: 'var(--toast-bg)',
      color: 'var(--toast-text)',
      borderRadius: '12px',
      padding: '12px 16px',
      fontSize: '14px',
      boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
    },
    success: {
      iconTheme: { primary: '#10B981', secondary: '#fff' },
    },
    error: {
      iconTheme: { primary: '#EF4444', secondary: '#fff' },
    },
  }}
/>

// CSS Variables (index.css)
:root {
  --toast-bg: #ffffff;
  --toast-text: #374151;
}

.dark {
  --toast-bg: #1F2937;
  --toast-text: #F9FAFB;
}
```

#### 3.6 Skeleton 개선

```tsx
// /shared/ui/Skeleton.tsx
export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      className={`
        bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200
        dark:from-gray-700 dark:via-gray-600 dark:to-gray-700
        bg-[length:200%_100%]
        animate-shimmer
        rounded
        ${className}
      `}
    />
  );
}
```

#### 3.7 스와이프 제스처

```bash
npm install react-swipeable
```

```tsx
// 이력서 목록에서 스와이프로 삭제
import { useSwipeable } from 'react-swipeable';

function ResumeRow({ resume, onDelete }) {
  const [swiped, setSwiped] = useState(false);

  const handlers = useSwipeable({
    onSwipedLeft: () => setSwiped(true),
    onSwipedRight: () => setSwiped(false),
    trackMouse: true,
  });

  return (
    <div {...handlers} className="relative overflow-hidden">
      <div className={`transform transition-transform ${swiped ? '-translate-x-20' : ''}`}>
        {/* Row Content */}
      </div>
      {swiped && (
        <button
          onClick={onDelete}
          className="absolute right-0 top-0 h-full w-20 bg-red-500 text-white"
        >
          삭제
        </button>
      )}
    </div>
  );
}
```

#### 3.8 Pull to Refresh (모바일)

```tsx
// /shared/hooks/usePullToRefresh.ts
export function usePullToRefresh(onRefresh: () => Promise<void>) {
  const [pulling, setPulling] = useState(false);
  const [progress, setProgress] = useState(0);

  // Touch 이벤트 핸들러
  // 80px 이상 당기면 새로고침
}

// ResumesPage.tsx
const { pulling, progress, ...handlers } = usePullToRefresh(async () => {
  await refetch();
});

<div {...handlers}>
  {pulling && <LoadingSpinner progress={progress} />}
  {/* Content */}
</div>
```

#### 3.9 Haptic Feedback

```tsx
// /shared/lib/haptic.ts
export function triggerHaptic(type: 'light' | 'medium' | 'heavy' = 'light') {
  if ('vibrate' in navigator) {
    const patterns = { light: [10], medium: [20], heavy: [50] };
    navigator.vibrate(patterns[type]);
  }
}

// 버튼 클릭 시 적용
<button onClick={() => {
  triggerHaptic('light');
  handleSubmit();
}}>
```

### 기술 스택
- **Animation**: Framer Motion
- **Icons**: Heroicons
- **Gestures**: react-swipeable
- **Haptic**: Navigator Vibration API

### 완료 기준
- [ ] 페이지 전환 시 Fade + Slide 애니메이션
- [ ] 모든 아이콘이 Heroicons로 교체
- [ ] 다크 모드 토글 동작
- [ ] 모든 페이지에 다크 모드 적용
- [ ] Backdrop blur 효과 적용 (네비게이션, 모달)
- [ ] Toast 알림이 다크 모드 지원
- [ ] Skeleton에 Shimmer 효과
- [ ] 스와이프로 이력서 삭제 가능
- [ ] Pull to refresh 동작 (모바일)
- [ ] 버튼 클릭 시 Haptic feedback

### 의존성
- Phase 1 완료 필수

---

## Phase 4: 파일 & 검색 📋

**목표**: 파일 첨부, PDF 미리보기, 검색 기능 추가

**우선순위**: 중간
**예상 기간**: 2주

### 구현 내용

#### 4.1 채팅 내 파일 첨부

**Database 스키마 추가**:
```sql
CREATE TABLE rc_chat_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attachment_id VARCHAR(36) UNIQUE NOT NULL,
    message_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES rc_chat_message(id)
);
```

**Backend API**:
```java
// POST /api/applicant/chat/{sessionToken}/attachment
// POST /api/chat/session/{sessionToken}/attachment

@PostMapping("/{sessionToken}/attachment")
public ResponseEntity<ApiResponse<AttachmentDto>> uploadAttachment(
        @PathVariable String sessionToken,
        @RequestParam("file") MultipartFile file
) {
    // 파일 검증 (확장자, 크기)
    // 파일 저장
    // 첨부파일 정보 DB 저장
    // 메시지 생성 (파일 첨부 타입)
}

// GET /api/chat/attachment/{attachmentId}/download
```

**허용 파일 타입**:
- 문서: PDF, DOCX, PPTX (최대 10MB)
- 이미지: JPG, PNG, GIF (최대 5MB)
- 압축: ZIP (최대 20MB)

**Frontend**:
```tsx
// 파일 선택 UI
<input
  type="file"
  accept=".pdf,.docx,.pptx,.jpg,.png,.gif,.zip"
  onChange={handleFileSelect}
  className="hidden"
  ref={fileInputRef}
/>

<button onClick={() => fileInputRef.current?.click()}>
  <PaperClipIcon className="w-5 h-5" />
</button>

// 첨부파일 표시
<div className="flex items-center gap-2 p-2 bg-gray-100 rounded">
  <DocumentTextIcon className="w-5 h-5" />
  <span>{file.name}</span>
  <span className="text-xs text-gray-500">{formatFileSize(file.size)}</span>
  <button onClick={handleDownload}>
    <ArrowDownTrayIcon className="w-4 h-4" />
  </button>
</div>
```

#### 4.2 이력서 PDF 미리보기

```bash
npm install react-pdf pdfjs-dist
```

**Frontend 컴포넌트**:
```tsx
// /features/resume/components/PdfViewer.tsx
import { Document, Page, pdfjs } from 'react-pdf';

pdfjs.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.min.js`;

export function PdfViewer({ url }: { url: string }) {
  const [numPages, setNumPages] = useState<number>(0);
  const [pageNumber, setPageNumber] = useState(1);

  return (
    <div>
      <Document file={url} onLoadSuccess={({ numPages }) => setNumPages(numPages)}>
        <Page pageNumber={pageNumber} width={600} />
      </Document>
      <div>
        <button onClick={() => setPageNumber(p => Math.max(1, p - 1))}>이전</button>
        <span>{pageNumber} / {numPages}</span>
        <button onClick={() => setPageNumber(p => Math.min(numPages, p + 1))}>다음</button>
      </div>
    </div>
  );
}
```

**모달로 미리보기**:
```tsx
// ResumesPage.tsx
const [previewUrl, setPreviewUrl] = useState<string | null>(null);

<button onClick={() => setPreviewUrl(resume.fileUrl)}>미리보기</button>

{previewUrl && (
  <Modal onClose={() => setPreviewUrl(null)}>
    <PdfViewer url={previewUrl} />
  </Modal>
)}
```

**Backend API**:
```java
// GET /api/applicant/resume/{resumeSlug}/file
@GetMapping("/{resumeSlug}/file")
public ResponseEntity<Resource> getResumeFile(@PathVariable UUID resumeSlug) {
    Resume resume = resumeRepository.findByResumeSlug(resumeSlug);
    File file = new File(resume.getFilePath());
    Resource resource = new FileSystemResource(file);

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resume.getOriginalFileName() + "\"")
            .body(resource);
}
```

#### 4.3 채팅 내 검색

**Backend API**:
```java
// GET /api/applicant/chat/{sessionToken}/search?q=keyword&page=1

@GetMapping("/{sessionToken}/search")
public ResponseEntity<ApiResponse<List<ChatMessageDto>>> searchMessages(
        @PathVariable String sessionToken,
        @RequestParam String q,
        @RequestParam(defaultValue = "1") int page
) {
    List<ChatMessage> messages = chatMessageRepository
            .findBySessionAndContentContainingIgnoreCase(session, q, PageRequest.of(page - 1, 20));
    return ResponseEntity.ok(ApiResponse.success(messages.stream().map(ChatMessageDto::from).toList()));
}
```

**Frontend**:
```tsx
// ChatPage.tsx
const [searchQuery, setSearchQuery] = useState('');
const [searchMode, setSearchMode] = useState(false);

<button onClick={() => setSearchMode(!searchMode)}>
  <MagnifyingGlassIcon className="w-5 h-5" />
</button>

{searchMode && (
  <input
    type="text"
    placeholder="메시지 검색..."
    value={searchQuery}
    onChange={(e) => setSearchQuery(e.target.value)}
    className="..."
  />
)}

// 검색 결과 하이라이트
<p dangerouslySetInnerHTML={{ __html: highlightKeyword(message.content, searchQuery) }} />
```

#### 4.4 채팅 히스토리 내보내기

**Backend API**:
```java
// POST /api/applicant/chat/{sessionToken}/export?format=pdf

@PostMapping("/{sessionToken}/export")
@Async
public ResponseEntity<ApiResponse<Void>> exportChatHistory(
        @PathVariable String sessionToken,
        @RequestParam(defaultValue = "pdf") String format
) {
    // 비동기 작업 시작
    chatExportService.exportToFormat(sessionToken, format);
    return ResponseEntity.accepted().body(ApiResponse.success());
}

// 완료 시 이메일로 다운로드 링크 전송
```

**PDF 생성**:
```java
// build.gradle
dependencies {
    implementation 'com.itextpdf:itext7-core:7.2.5'
}

// ChatExportService.java
public void exportToPdf(String sessionToken) {
    PdfWriter writer = new PdfWriter(outputStream);
    PdfDocument pdf = new PdfDocument(writer);
    Document document = new Document(pdf);

    // 메시지 추가
    for (ChatMessage message : messages) {
        document.add(new Paragraph(message.getContent()));
    }

    document.close();
}
```

**Frontend**:
```tsx
<button onClick={handleExport}>
  <ArrowDownTrayIcon className="w-5 h-5" />
  내보내기
</button>

// 내보내기 옵션 선택 모달
<Modal>
  <select value={format} onChange={(e) => setFormat(e.target.value)}>
    <option value="pdf">PDF</option>
    <option value="txt">텍스트</option>
    <option value="json">JSON</option>
  </select>
  <label>
    <input type="checkbox" checked={includeAttachments} onChange={...} />
    첨부파일 포함
  </label>
</Modal>
```

### 기술 스택
- **PDF**: iText7 (Backend), react-pdf (Frontend)
- **File Upload**: MultipartFile, FileSystemResource
- **Export**: iText7, Jackson (JSON)

### 완료 기준
- [ ] 채팅에서 파일 첨부 가능
- [ ] 첨부파일 다운로드 가능
- [ ] 이미지 파일은 인라인 표시
- [ ] 이력서 PDF 미리보기 모달
- [ ] 채팅 내 메시지 검색
- [ ] 키워드 하이라이트
- [ ] 채팅 히스토리 PDF 내보내기
- [ ] 내보내기 완료 시 이메일 전송

### 의존성
- Phase 1 완료 필수

---

## Phase 5: 보안 강화 📋

**목표**: 인증 강화 및 스팸 방지

**우선순위**: 높음
**예상 기간**: 1-2주

### 구현 내용

#### 5.1 2단계 인증 (2FA)

**Database 스키마**:
```sql
ALTER TABLE rc_applicant ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE rc_applicant ADD COLUMN two_factor_secret VARCHAR(32);

CREATE TABLE rc_two_factor_backup_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    applicant_id BIGINT NOT NULL,
    code VARCHAR(10) NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (applicant_id) REFERENCES rc_applicant(id)
);
```

**Backend - TOTP 구현**:
```java
// build.gradle
dependencies {
    implementation 'com.warrenstrange:googleauth:1.5.0'
}

// TwoFactorService.java
@Service
public class TwoFactorService {
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }

    public String generateQrCodeUrl(String email, String secret) {
        return "otpauth://totp/ResumeChat:" + email + "?secret=" + secret + "&issuer=ResumeChat";
    }
}
```

**API**:
- `POST /api/applicant/2fa/enable` - 2FA 활성화 (QR 코드 반환)
- `POST /api/applicant/2fa/verify` - 코드 검증
- `POST /api/applicant/2fa/disable` - 2FA 비활성화
- `GET /api/applicant/2fa/backup-codes` - 백업 코드 생성

**Frontend**:
```tsx
// QR 코드 표시
import QRCode from 'qrcode.react';

<QRCode value={qrCodeUrl} size={200} />

// 인증 코드 입력
<input
  type="text"
  maxLength={6}
  placeholder="6자리 코드"
  pattern="[0-9]{6}"
/>
```

#### 5.2 채용담당자 이메일 인증

**Database 스키마**:
```sql
ALTER TABLE rc_chat_session ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE rc_chat_session ADD COLUMN verification_token VARCHAR(36);
ALTER TABLE rc_chat_session ADD COLUMN verification_expires_at TIMESTAMP;
```

**Backend**:
```java
// 첫 메시지 전송 시 인증 이메일 발송
public void sendMessage(UUID resumeSlug, SendMessageRequest request) {
    ChatSession session = getOrCreateSession(resume, request);

    if (!session.isEmailVerified()) {
        String token = UUID.randomUUID().toString();
        session.setVerificationToken(token);
        session.setVerificationExpiresAt(LocalDateTime.now().plusHours(24));

        emailService.sendVerificationEmail(session.getRecruiterEmail(), token);

        throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
    }

    // 메시지 저장
}

// GET /api/chat/verify?token={token}
@GetMapping("/verify")
public ResponseEntity<String> verifyEmail(@RequestParam String token) {
    ChatSession session = chatSessionRepository.findByVerificationToken(token);
    // 토큰 확인 및 만료 시간 검증
    session.setEmailVerified(true);
    return ResponseEntity.ok("이메일 인증 완료");
}
```

**Frontend**:
```tsx
// 인증 필요 안내
{error?.code === 'EMAIL_VERIFICATION_REQUIRED' && (
  <div className="p-4 bg-yellow-50 rounded">
    <p>이메일 인증이 필요합니다. {recruiterEmail}로 전송된 인증 링크를 확인해주세요.</p>
  </div>
)}
```

#### 5.3 Rate Limiting

**Backend - Bucket4j 사용**:
```java
// build.gradle
dependencies {
    implementation 'com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0'
}

// RateLimitingFilter.java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String key = getClientIP(request);
        Bucket bucket = resolveBucket(key);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.getWriter().write("{\"error\": \"Too many requests\"}");
        }
    }

    private Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
            return Bucket.builder().addLimit(limit).build();
        });
    }
}
```

**Rate Limit 정책**:
- 일반 API: 분당 60회
- 로그인: 분당 5회
- 메시지 전송: 분당 20회

#### 5.4 reCAPTCHA

**Backend**:
```java
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
}

// RecaptchaService.java
@Service
public class RecaptchaService {
    @Value("${recaptcha.secret}")
    private String secret;

    public boolean verify(String token) {
        String url = "https://www.google.com/recaptcha/api/siteverify";
        // POST 요청으로 검증
        return response.getSuccess() && response.getScore() > 0.5;
    }
}
```

**Frontend**:
```bash
npm install react-google-recaptcha
```

```tsx
import ReCAPTCHA from 'react-google-recaptcha';

<ReCAPTCHA
  sitekey={RECAPTCHA_SITE_KEY}
  onChange={setRecaptchaToken}
/>
```

**적용 위치**:
- 회원가입
- 로그인 (3회 실패 후)
- 채용담당자 첫 메시지

#### 5.5 악성 사용자 차단

**Database**:
```sql
CREATE TABLE rc_blocked_ips (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ip_address VARCHAR(45) NOT NULL,
    reason VARCHAR(500),
    blocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    UNIQUE KEY uk_ip (ip_address)
);

CREATE TABLE rc_blocked_emails (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    reason VARCHAR(500),
    blocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email (email)
);
```

**API** (관리자용):
- `POST /api/admin/block/ip` - IP 차단
- `POST /api/admin/block/email` - 이메일 차단
- `DELETE /api/admin/block/ip/{ip}` - 차단 해제

### 기술 스택
- **2FA**: Google Authenticator (TOTP)
- **Rate Limiting**: Bucket4j
- **CAPTCHA**: Google reCAPTCHA v3

### 완료 기준
- [ ] 2FA 활성화 및 로그인 시 검증
- [ ] 백업 코드 생성 및 사용
- [ ] 채용담당자 이메일 인증 필수
- [ ] Rate limiting 동작 (429 에러)
- [ ] reCAPTCHA 검증
- [ ] IP/이메일 차단 기능

### 의존성
- Phase 1 완료 필수

---

## Phase 6: 분석 & 통계 📋

**목표**: 대시보드 및 데이터 분석 기능

**우선순위**: 낮음
**예상 기간**: 1주

### 구현 내용

#### 6.1 지원자 대시보드

**API**:
```java
// GET /api/applicant/dashboard
public DashboardDto getDashboard(UUID applicantUuid) {
    return DashboardDto.builder()
            .totalResumes(resumeRepository.countByApplicant(applicant))
            .totalViews(resumeRepository.sumViewsByApplicant(applicant))
            .totalSessions(chatSessionRepository.countByApplicant(applicant))
            .totalMessages(chatMessageRepository.countByApplicant(applicant))
            .recentSessions(getRecentSessions(applicant))
            .viewsChart(getViewsChartData(applicant))
            .build();
}
```

**Frontend**:
```bash
npm install recharts
```

```tsx
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts';

// /pages/dashboard/DashboardPage.tsx
<div className="grid grid-cols-4 gap-4">
  <StatCard title="총 이력서" value={data.totalResumes} />
  <StatCard title="총 조회수" value={data.totalViews} />
  <StatCard title="채팅 세션" value={data.totalSessions} />
  <StatCard title="받은 메시지" value={data.totalMessages} />
</div>

<LineChart width={600} height={300} data={data.viewsChart}>
  <CartesianGrid strokeDasharray="3 3" />
  <XAxis dataKey="date" />
  <YAxis />
  <Tooltip />
  <Line type="monotone" dataKey="views" stroke="#2563EB" />
</LineChart>
```

#### 6.2 이력서별 통계

**API**:
```java
// GET /api/applicant/resume/{resumeSlug}/stats
public ResumeStatsDto getResumeStats(UUID resumeSlug) {
    return ResumeStatsDto.builder()
            .totalViews(resume.getViewCnt())
            .totalSessions(chatSessionRepository.countByResume(resume))
            .averageResponseTime(calculateAverageResponseTime(resume))
            .sessionsByCompany(groupSessionsByCompany(resume))
            .viewsByDate(getViewsByDate(resume))
            .build();
}
```

**Frontend**:
```tsx
// 이력서 상세 통계 페이지
<BarChart data={sessionsByCompany}>
  <Bar dataKey="count" fill="#2563EB" />
</BarChart>

<PieChart>
  <Pie data={responseTimes} dataKey="value" nameKey="name" />
</PieChart>
```

### 기술 스택
- **Charts**: Recharts
- **Analytics**: 자체 구현 (향후 Google Analytics 연동 가능)

### 완료 기준
- [ ] 대시보드 페이지
- [ ] 조회수 그래프
- [ ] 이력서별 통계
- [ ] 응답률 분석

### 의존성
- Phase 1 완료 필수

---

## Phase 7: 고급 기능 📋

**목표**: AI, 캘린더 연동, 화상 면접 등 고급 기능

**우선순위**: 낮음
**예상 기간**: 3-4주

### 구현 내용

#### 7.1 AI 이력서 분석

```java
// build.gradle
dependencies {
    implementation 'com.theokanning.openai-gpt3-java:service:0.14.0'
}

// OpenAIService.java
public ResumeAnalysisDto analyzeResume(String resumeText) {
    String prompt = """
        다음 이력서를 분석하여 강점, 개선점, 키워드를 추출해주세요.

        이력서 내용:
        %s
        """.formatted(resumeText);

    ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(List.of(new ChatMessage("user", prompt)))
            .build();

    // OpenAI API 호출
}
```

#### 7.2 캘린더 연동

```java
// build.gradle
dependencies {
    implementation 'com.google.api-client:google-api-client:1.32.1'
    implementation 'com.google.apis:google-api-services-calendar:v3-rev20210629-1.32.1'
}

// CalendarService.java
public String createMeetingEvent(String title, LocalDateTime start, LocalDateTime end) {
    Event event = new Event()
            .setSummary(title)
            .setStart(new EventDateTime().setDateTime(new DateTime(start)))
            .setEnd(new EventDateTime().setDateTime(new DateTime(end)));

    Event createdEvent = calendar.events().insert("primary", event).execute();
    return createdEvent.getHtmlLink();
}
```

#### 7.3 화상 면접 연동

**Zoom API**:
```java
// ZoomService.java
public String createMeeting(String topic, LocalDateTime start) {
    // Zoom API 호출
    // 미팅 링크 반환
}
```

### 완료 기준
- [ ] AI 이력서 분석 결과 표시
- [ ] 캘린더 이벤트 생성
- [ ] Zoom 미팅 링크 생성

### 의존성
- Phase 1 완료 필수

---

## Phase 8: 모바일 앱 📋

**목표**: React Native 기반 iOS/Android 앱

**우선순위**: 낮음
**예상 기간**: 4-6주

### 구현 내용

```bash
npx react-native init ResumeChatApp --template react-native-template-typescript
```

### 기능
- [ ] 로그인/회원가입
- [ ] 이력서 업로드 (카메라, 갤러리)
- [ ] 채팅
- [ ] 푸시 알림 (FCM)
- [ ] 생체 인증

### 완료 기준
- [ ] iOS App Store 출시
- [ ] Google Play Store 출시

---

## 기술 의존성 그래프

```
Phase 0 (기본 인프라)
    ↓
Phase 1 (MVP) ← 모든 Phase의 기반
    ↓
    ├─→ Phase 2 (실시간 통신) → Phase 3 (UX 강화)
    ├─→ Phase 4 (파일 & 검색)
    ├─→ Phase 5 (보안)
    ├─→ Phase 6 (분석)
    └─→ Phase 7 (고급 기능) → Phase 8 (모바일)
```

---

## 총 예상 기간

- **Phase 0-1**: 4주 (완료)
- **Phase 2**: 2-3주
- **Phase 3**: 2주
- **Phase 4**: 2주
- **Phase 5**: 1-2주
- **Phase 6**: 1주
- **Phase 7**: 3-4주
- **Phase 8**: 4-6주

**총 예상 기간**: 약 3-4개월 (Phase 2-8)

---

**문서 버전**: 1.0
**최종 업데이트**: 2024-03-10
**작성자**: Resume Chat Team
