# Resume Chat - 개발 작업 목록 (Tasks)

> 이 문서는 [plan.md](./plan.md)의 세부 작업을 체크리스트 형식으로 정리한 문서입니다.

**작성일**: 2026-03-10
**현재 진행**: Phase 2 (실시간 통신 & 알림)

---

## 📊 전체 진행 현황

- [x] Phase 0: 프로젝트 초기 설정
- [x] Phase 1: MVP - 핵심 기능
- [ ] Phase 2: 실시간 통신 & 알림 (진행 예정)
- [ ] Phase 3: Apple UX/UI 강화
- [ ] Phase 4: 파일 & 검색
- [ ] Phase 5: 보안 강화
- [ ] Phase 6: 분석 & 통계
- [ ] Phase 7: 고급 기능
- [ ] Phase 8: 모바일 앱

---

## Phase 2: 실시간 통신 & 알림 🔄

**우선순위**: 높음 | **예상 기간**: 2-3주

### 2.1 WebSocket 실시간 채팅

#### Backend
- [ ] Spring WebSocket 의존성 추가 (build.gradle)
- [ ] WebSocketConfig 클래스 생성
  - [ ] MessageBroker 설정 (/topic)
  - [ ] Application destination prefix 설정 (/app)
  - [ ] STOMP endpoint 등록 (/ws)
  - [ ] SockJS fallback 설정
  - [ ] CORS 설정 추가
- [ ] ChatWebSocketController 생성
  - [ ] @MessageMapping 메시지 수신 핸들러
  - [ ] @SendTo 브로드캐스트 설정
  - [ ] 메시지 저장 로직 연동
- [ ] WebSocket 보안 설정
  - [ ] JWT 인증 통합
  - [ ] 세션별 접근 권한 검증

#### Frontend
- [ ] WebSocket 라이브러리 설치 (@stomp/stompjs, sockjs-client)
- [ ] WebSocket 클라이언트 유틸리티 생성 (/shared/api/websocket.ts)
  - [ ] SockJS 연결 설정
  - [ ] STOMP 클라이언트 초기화
  - [ ] 자동 재연결 로직 (5초 간격)
  - [ ] 구독(subscribe) 함수
  - [ ] 메시지 발행(publish) 함수
- [ ] ChatPage에 WebSocket 통합
  - [ ] useEffect로 연결 관리
  - [ ] 실시간 메시지 수신 처리
  - [ ] 메시지 전송 시 WebSocket 사용
  - [ ] 연결 상태 표시 UI
  - [ ] cleanup 함수로 연결 해제
- [ ] RecruiterChatPage에 WebSocket 통합

#### 테스트
- [ ] WebSocket 연결 테스트
- [ ] 실시간 메시지 송수신 테스트
- [ ] 여러 클라이언트 동시 접속 테스트
- [ ] 재연결 시나리오 테스트

### 2.2 입력 중 표시 (Typing Indicator)

#### Backend
- [ ] TypingEvent DTO 생성
  - [ ] sessionToken 필드
  - [ ] senderName 필드
  - [ ] typing (boolean) 필드
- [ ] ChatWebSocketController에 typing 핸들러 추가
  - [ ] @MessageMapping("/chat/{sessionToken}/typing")
  - [ ] @SendTo("/topic/session/{sessionToken}/typing")

#### Frontend
- [ ] useTypingIndicator 커스텀 훅 생성
  - [ ] 입력 감지 함수
  - [ ] 타이핑 이벤트 발행
  - [ ] 3초 타임아웃 자동 해제
- [ ] 타이핑 인디케이터 UI 컴포넌트
  - [ ] "상대방이 입력 중..." 메시지
  - [ ] 애니메이션 효과 (점 3개)
- [ ] ChatPage에 통합
- [ ] RecruiterChatPage에 통합

### 2.3 온라인 상태 표시

#### Backend
- [ ] PresenceService 생성
  - [ ] ConcurrentHashMap으로 온라인 사용자 관리
  - [ ] userConnected 메서드
  - [ ] userDisconnected 메서드
  - [ ] broadcastPresence 메서드
- [ ] WebSocket 연결/해제 이벤트 리스너
  - [ ] SessionConnectEvent 핸들러
  - [ ] SessionDisconnectEvent 핸들러
- [ ] PresenceEvent DTO 생성

#### Frontend
- [ ] 온라인 상태 표시 UI
  - [ ] 초록색/회색 원 아이콘
  - [ ] "온라인" / "오프라인" 텍스트
  - [ ] 마지막 접속 시간 표시 (formatRelative)
- [ ] WebSocket으로 presence 구독
- [ ] 상태 변경 시 실시간 업데이트

### 2.4 이메일 알림

#### Backend
- [ ] Spring Mail 의존성 추가
- [ ] application.yml 메일 설정
  - [ ] Gmail SMTP 설정
  - [ ] 환경변수 설정 (username, password)
- [ ] EmailService 생성
  - [ ] @Async 비동기 처리 설정
  - [ ] sendNewMessageNotification 메서드
  - [ ] sendNewSessionNotification 메서드
  - [ ] HTML 템플릿 렌더링
- [ ] HTML 이메일 템플릿 생성
  - [ ] email-new-message.html
  - [ ] email-new-session.html
  - [ ] 반응형 디자인
  - [ ] 채팅 링크 버튼
- [ ] ChatService에 이메일 알림 통합
  - [ ] 메시지 전송 시 알림 발송
  - [ ] 알림 설정 확인 로직

### 2.5 브라우저 푸시 알림

#### Backend
- [ ] Web Push 라이브러리 추가 (web-push)
- [ ] VAPID 키 생성
- [ ] WebPushService 생성
  - [ ] sendNotification 메서드
  - [ ] 구독 정보 암호화
- [ ] PushSubscription 엔티티 생성
- [ ] PushSubscription 리포지토리 생성
- [ ] PushController 생성
  - [ ] POST /api/push/subscribe
  - [ ] DELETE /api/push/unsubscribe
- [ ] ChatService에 푸시 알림 통합

#### Frontend
- [ ] Service Worker 파일 생성 (public/sw.js)
  - [ ] push 이벤트 핸들러
  - [ ] notificationclick 이벤트 핸들러
- [ ] Service Worker 등록 (main.tsx)
- [ ] usePushNotification 훅 생성
  - [ ] 브라우저 알림 권한 요청
  - [ ] 푸시 구독 함수
  - [ ] VAPID 공개키 설정
  - [ ] 구독 정보 서버 전송
- [ ] 알림 권한 요청 UI
  - [ ] 첫 로그인 시 팝업
  - [ ] 설정에서 재요청 가능

### 2.6 알림 설정 페이지

#### Database
- [ ] rc_notification_settings 테이블 생성
  - [ ] applicant_id (FK)
  - [ ] email_new_message (BOOLEAN)
  - [ ] email_new_session (BOOLEAN)
  - [ ] push_new_message (BOOLEAN)
  - [ ] created_at, updated_at

#### Backend
- [ ] NotificationSettings 엔티티 생성
- [ ] NotificationSettingsRepository 생성
- [ ] NotificationSettingsService 생성
  - [ ] getSettings 메서드
  - [ ] updateSettings 메서드
  - [ ] 기본 설정 생성 로직
- [ ] NotificationSettingsController 생성
  - [ ] GET /api/applicant/notification-settings
  - [ ] PUT /api/applicant/notification-settings

#### Frontend
- [ ] NotificationSettingsPage 생성
  - [ ] 이메일 알림 on/off 토글
  - [ ] 푸시 알림 on/off 토글
  - [ ] 저장 버튼
- [ ] useNotificationSettings 훅 생성
- [ ] 라우팅 설정 (/settings/notifications)
- [ ] 네비게이션 메뉴에 링크 추가

### 테스트 & 마무리
- [ ] 전체 WebSocket 통합 테스트
- [ ] 이메일 발송 테스트
- [ ] 푸시 알림 테스트 (Chrome, Safari)
- [ ] 성능 테스트 (동시 접속 100명)
- [ ] 문서 업데이트

---

## Phase 3: Apple UX/UI 강화 📋

**우선순위**: 중간 | **예상 기간**: 2주

### 3.1 애니메이션 시스템

#### Setup
- [ ] Framer Motion 라이브러리 설치
- [ ] Tailwind 커스텀 설정
  - [ ] iOS 이징 함수 추가
  - [ ] Spring 이징 함수 추가
  - [ ] 애니메이션 duration 추가
  - [ ] shimmer 키프레임 추가

#### 구현
- [ ] PageTransition 컴포넌트 생성
  - [ ] Fade + Slide 애니메이션
  - [ ] iOS 이징 적용
- [ ] 버튼 마이크로 인터랙션
  - [ ] whileHover 효과
  - [ ] whileTap 효과
- [ ] 리스트 아이템 애니메이션
  - [ ] stagger 효과
- [ ] 모달 애니메이션
  - [ ] scale + fade

### 3.2 Heroicons 통합

- [ ] @heroicons/react 설치
- [ ] 아이콘 매핑 문서 작성
- [ ] 기존 SVG를 Heroicons로 교체
  - [ ] PaperAirplaneIcon (전송)
  - [ ] LinkIcon (링크 복사)
  - [ ] TrashIcon (삭제)
  - [ ] ArrowUpTrayIcon (업로드)
  - [ ] ChatBubbleLeftIcon (채팅)
  - [ ] DocumentTextIcon (문서)
  - [ ] MagnifyingGlassIcon (검색)
  - [ ] Cog6ToothIcon (설정)
  - [ ] BellIcon (알림)
  - [ ] UserIcon (프로필)

### 3.3 다크 모드

#### Setup
- [ ] Tailwind 다크 모드 설정 (class 모드)
- [ ] CSS Variables 정의
  - [ ] 색상 변수
  - [ ] 그림자 변수

#### 구현
- [ ] useDarkMode 훅 생성
  - [ ] localStorage 저장
  - [ ] 시스템 설정 감지
  - [ ] 토글 함수
- [ ] 다크 모드 토글 버튼 (Header)
  - [ ] SunIcon / MoonIcon
  - [ ] 애니메이션 효과
- [ ] 모든 컴포넌트 다크 모드 스타일 추가
  - [ ] Button
  - [ ] Input
  - [ ] Card
  - [ ] Modal
  - [ ] Toast
  - [ ] Skeleton
  - [ ] EmptyState
- [ ] 페이지별 다크 모드 적용
  - [ ] LoginPage
  - [ ] JoinPage
  - [ ] ResumesPage
  - [ ] ChatPage
  - [ ] RecruiterChatPage
  - [ ] NotificationSettingsPage

### 3.4 Backdrop Blur

- [ ] 네비게이션 backdrop blur
  - [ ] 스크롤 감지
  - [ ] 동적 클래스 적용
- [ ] 모달 오버레이 backdrop blur
- [ ] 채팅 입력창 backdrop blur (iOS)

### 3.5 Toast 커스터마이징

- [ ] Toaster 설정 업데이트
  - [ ] position: top-center
  - [ ] 다크 모드 지원 스타일
  - [ ] 커스텀 아이콘 색상
  - [ ] 둥근 모서리
  - [ ] 그림자 효과
- [ ] CSS Variables 연동

### 3.6 Skeleton 개선

- [ ] Skeleton 컴포넌트 업데이트
  - [ ] Gradient 배경
  - [ ] shimmer 애니메이션
  - [ ] 다크 모드 지원

### 3.7 스와이프 제스처

- [ ] react-swipeable 설치
- [ ] ResumeRow 스와이프 삭제 구현
  - [ ] 왼쪽 스와이프 감지
  - [ ] 삭제 버튼 표시
  - [ ] 애니메이션 효과
- [ ] 채팅 세션 카드 스와이프 구현

### 3.8 Pull to Refresh

- [ ] usePullToRefresh 훅 생성
  - [ ] Touch 이벤트 핸들러
  - [ ] 80px 임계값
  - [ ] progress 계산
- [ ] LoadingSpinner 컴포넌트
- [ ] ResumesPage에 적용
- [ ] ChatSessionsPage에 적용

### 3.9 Haptic Feedback

- [ ] haptic.ts 유틸리티 생성
  - [ ] triggerHaptic 함수
  - [ ] light/medium/heavy 패턴
- [ ] 버튼 클릭 시 적용
- [ ] 스와이프 제스처 시 적용
- [ ] 삭제/전송 성공 시 적용

### 테스트 & 마무리
- [ ] 애니메이션 성능 테스트
- [ ] 다크 모드 전환 테스트
- [ ] 모바일 터치 제스처 테스트
- [ ] 접근성 검증 (키보드 네비게이션)

---

## Phase 4: 파일 & 검색 📋

**우선순위**: 중간 | **예상 기간**: 2주

### 4.1 채팅 내 파일 첨부

#### Database
- [ ] rc_chat_attachment 테이블 생성
  - [ ] attachment_id (UUID)
  - [ ] message_id (FK)
  - [ ] file_name, file_path, file_size
  - [ ] mime_type
  - [ ] created_at

#### Backend
- [ ] ChatAttachment 엔티티 생성
- [ ] ChatAttachmentRepository 생성
- [ ] FileStorageService 확장
  - [ ] storeAttachment 메서드
  - [ ] 파일 타입 검증 (PDF, DOCX, PPTX, JPG, PNG, GIF, ZIP)
  - [ ] 크기 제한 (문서 10MB, 이미지 5MB, 압축 20MB)
- [ ] ChatController에 첨부파일 API 추가
  - [ ] POST /api/applicant/chat/{sessionToken}/attachment
  - [ ] POST /api/chat/session/{sessionToken}/attachment
  - [ ] GET /api/chat/attachment/{attachmentId}/download
- [ ] ChatMessage와 ChatAttachment 연관관계 설정

#### Frontend
- [ ] 파일 선택 UI
  - [ ] hidden input + ref
  - [ ] PaperClipIcon 버튼
  - [ ] accept 속성 설정
- [ ] 파일 업로드 로직
  - [ ] FormData 생성
  - [ ] 업로드 진행률 표시
- [ ] 첨부파일 표시 컴포넌트
  - [ ] 파일 아이콘 (타입별)
  - [ ] 파일명 + 크기
  - [ ] 다운로드 버튼
- [ ] 이미지 파일 인라인 표시
  - [ ] <img> 태그로 렌더링
  - [ ] 라이트박스 효과
- [ ] useUploadAttachment 훅 생성

### 4.2 이력서 PDF 미리보기

#### Frontend
- [ ] react-pdf, pdfjs-dist 설치
- [ ] PdfViewer 컴포넌트 생성
  - [ ] Document, Page 렌더링
  - [ ] 페이지 네비게이션 (이전/다음)
  - [ ] 확대/축소 기능
  - [ ] 전체화면 모드
- [ ] Modal 컴포넌트에 PdfViewer 통합
- [ ] ResumesPage에 "미리보기" 버튼 추가
- [ ] PDF Worker 설정

#### Backend
- [ ] ResumeController에 파일 다운로드 API
  - [ ] GET /api/applicant/resume/{resumeSlug}/file
  - [ ] Content-Disposition: inline
  - [ ] FileSystemResource 반환

### 4.3 채팅 내 검색

#### Backend
- [ ] ChatMessageRepository에 검색 메서드 추가
  - [ ] findBySessionAndContentContainingIgnoreCase
  - [ ] 페이지네이션 지원
- [ ] ChatController에 검색 API 추가
  - [ ] GET /api/applicant/chat/{sessionToken}/search?q=keyword&page=1

#### Frontend
- [ ] 검색 UI 구현
  - [ ] MagnifyingGlassIcon 버튼
  - [ ] 검색 입력창 (토글)
  - [ ] 검색 결과 개수 표시
- [ ] 키워드 하이라이트 함수
  - [ ] highlightKeyword 유틸리티
  - [ ] dangerouslySetInnerHTML 사용
  - [ ] 보안 처리 (XSS 방지)
- [ ] useSearchMessages 훅 생성
- [ ] 검색 결과로 스크롤 이동

### 4.4 채팅 히스토리 내보내기

#### Backend
- [ ] iText7 라이브러리 추가
- [ ] ChatExportService 생성
  - [ ] exportToPdf 메서드
  - [ ] exportToText 메서드
  - [ ] exportToJson 메서드
  - [ ] 첨부파일 포함 옵션
- [ ] @Async 비동기 처리
- [ ] ChatController에 내보내기 API
  - [ ] POST /api/applicant/chat/{sessionToken}/export?format=pdf
  - [ ] 완료 시 이메일 전송

#### Frontend
- [ ] ExportModal 컴포넌트
  - [ ] 포맷 선택 (PDF/TXT/JSON)
  - [ ] 첨부파일 포함 체크박스
  - [ ] 날짜 범위 선택 (optional)
- [ ] 내보내기 버튼 (ChatPage)
- [ ] useExportChat 훅 생성
- [ ] 진행 상태 표시 (Toast)

### 테스트 & 마무리
- [ ] 파일 업로드/다운로드 테스트
- [ ] 다양한 파일 타입 테스트
- [ ] PDF 미리보기 성능 테스트
- [ ] 검색 정확도 테스트
- [ ] 내보내기 기능 테스트

---

## Phase 5: 보안 강화 📋

**우선순위**: 높음 | **예상 기간**: 1-2주

### 5.1 2단계 인증 (2FA)

#### Database
- [ ] rc_applicant 테이블 컬럼 추가
  - [ ] two_factor_enabled (BOOLEAN)
  - [ ] two_factor_secret (VARCHAR)
- [ ] rc_two_factor_backup_codes 테이블 생성
  - [ ] applicant_id (FK)
  - [ ] code (VARCHAR)
  - [ ] used (BOOLEAN)

#### Backend
- [ ] Google Authenticator 라이브러리 추가
- [ ] TwoFactorService 생성
  - [ ] generateSecret 메서드
  - [ ] verifyCode 메서드
  - [ ] generateQrCodeUrl 메서드
  - [ ] generateBackupCodes 메서드
- [ ] TwoFactorController 생성
  - [ ] POST /api/applicant/2fa/enable
  - [ ] POST /api/applicant/2fa/verify
  - [ ] POST /api/applicant/2fa/disable
  - [ ] GET /api/applicant/2fa/backup-codes
- [ ] 로그인 로직에 2FA 검증 추가

#### Frontend
- [ ] qrcode.react 설치
- [ ] TwoFactorSetupPage 생성
  - [ ] QR 코드 표시
  - [ ] Secret 키 표시 (수동 입력용)
  - [ ] 인증 코드 입력 (6자리)
  - [ ] 백업 코드 표시 및 다운로드
- [ ] TwoFactorVerifyPage 생성 (로그인 후)
- [ ] SecuritySettingsPage에 2FA 토글 추가

### 5.2 채용담당자 이메일 인증

#### Database
- [ ] rc_chat_session 테이블 컬럼 추가
  - [ ] email_verified (BOOLEAN)
  - [ ] verification_token (VARCHAR)
  - [ ] verification_expires_at (TIMESTAMP)

#### Backend
- [ ] ChatService 메시지 전송 로직 수정
  - [ ] 이메일 인증 확인
  - [ ] 미인증 시 인증 이메일 발송
  - [ ] EMAIL_VERIFICATION_REQUIRED 예외
- [ ] 인증 이메일 템플릿 생성
- [ ] ChatController에 인증 API 추가
  - [ ] GET /api/chat/verify?token={token}
  - [ ] 토큰 만료 시간 검증 (24시간)

#### Frontend
- [ ] 이메일 인증 안내 UI
  - [ ] 노란색 알림 박스
  - [ ] 재전송 버튼
- [ ] EmailVerificationPage 생성
  - [ ] 성공/실패 메시지
  - [ ] 채팅으로 이동 버튼

### 5.3 Rate Limiting

#### Backend
- [ ] Bucket4j 라이브러리 추가
- [ ] RateLimitingFilter 생성
  - [ ] ConcurrentHashMap 캐시
  - [ ] IP 기반 제한
  - [ ] Bucket 생성 (분당 요청 수)
- [ ] Rate Limit 정책 설정
  - [ ] 일반 API: 분당 60회
  - [ ] 로그인: 분당 5회
  - [ ] 메시지 전송: 분당 20회
- [ ] 429 에러 응답 처리

#### Frontend
- [ ] Rate limit 에러 처리
  - [ ] 429 에러 감지
  - [ ] "너무 많은 요청" 메시지
  - [ ] Retry-After 헤더 파싱

### 5.4 reCAPTCHA

#### Backend
- [ ] WebFlux 의존성 추가
- [ ] RecaptchaService 생성
  - [ ] verify 메서드
  - [ ] Google API 호출
  - [ ] Score 검증 (> 0.5)
- [ ] application.yml에 reCAPTCHA 설정
- [ ] 인증 API에 reCAPTCHA 검증 추가
  - [ ] 회원가입
  - [ ] 로그인 (3회 실패 후)
  - [ ] 채용담당자 첫 메시지

#### Frontend
- [ ] react-google-recaptcha 설치
- [ ] ReCAPTCHA 컴포넌트 추가
  - [ ] JoinPage
  - [ ] LoginPage (조건부)
  - [ ] RecruiterChatPage
- [ ] 환경변수 설정 (SITE_KEY)

### 5.5 악성 사용자 차단

#### Database
- [ ] rc_blocked_ips 테이블 생성
  - [ ] ip_address (VARCHAR)
  - [ ] reason (VARCHAR)
  - [ ] blocked_at, expires_at
- [ ] rc_blocked_emails 테이블 생성
  - [ ] email (VARCHAR)
  - [ ] reason (VARCHAR)
  - [ ] blocked_at

#### Backend
- [ ] BlockedIp 엔티티 생성
- [ ] BlockedEmail 엔티티 생성
- [ ] BlockListService 생성
  - [ ] isIpBlocked 메서드
  - [ ] isEmailBlocked 메서드
  - [ ] blockIp 메서드
  - [ ] blockEmail 메서드
  - [ ] unblock 메서드
- [ ] BlockListFilter 생성 (요청 필터링)
- [ ] AdminController 생성 (관리자 전용)
  - [ ] POST /api/admin/block/ip
  - [ ] POST /api/admin/block/email
  - [ ] DELETE /api/admin/block/ip/{ip}
  - [ ] DELETE /api/admin/block/email/{email}
  - [ ] GET /api/admin/block/list

#### Frontend (관리자)
- [ ] AdminBlockListPage 생성
  - [ ] 차단 목록 조회
  - [ ] 차단 추가 폼
  - [ ] 차단 해제 버튼

### 테스트 & 마무리
- [ ] 2FA 전체 플로우 테스트
- [ ] 이메일 인증 테스트
- [ ] Rate limiting 테스트
- [ ] reCAPTCHA 동작 확인
- [ ] 차단 기능 테스트
- [ ] 보안 감사

---

## Phase 6: 분석 & 통계 📋

**우선순위**: 낮음 | **예상 기간**: 1주

### 6.1 지원자 대시보드

#### Database
- [ ] 통계 쿼리 최적화
  - [ ] 인덱스 추가 확인
  - [ ] 집계 쿼리 성능 측정

#### Backend
- [ ] DashboardDto 생성
  - [ ] totalResumes
  - [ ] totalViews
  - [ ] totalSessions
  - [ ] totalMessages
  - [ ] recentSessions
  - [ ] viewsChart
- [ ] DashboardService 생성
  - [ ] getDashboard 메서드
  - [ ] getViewsChartData 메서드 (일별 조회수)
  - [ ] getRecentSessions 메서드
- [ ] DashboardController 생성
  - [ ] GET /api/applicant/dashboard

#### Frontend
- [ ] Recharts 설치
- [ ] StatCard 컴포넌트 생성
  - [ ] 제목, 값, 아이콘
  - [ ] 증감률 표시 (optional)
- [ ] DashboardPage 생성
  - [ ] 4개 통계 카드 (Grid)
  - [ ] 조회수 LineChart
  - [ ] 최근 세션 목록
- [ ] useDashboard 훅 생성
- [ ] 라우팅 설정 (/)
- [ ] 네비게이션 메뉴에 "대시보드" 추가

### 6.2 이력서별 통계

#### Backend
- [ ] ResumeStatsDto 생성
  - [ ] totalViews
  - [ ] totalSessions
  - [ ] averageResponseTime
  - [ ] sessionsByCompany
  - [ ] viewsByDate
- [ ] ResumeService에 통계 메서드 추가
  - [ ] getResumeStats 메서드
  - [ ] calculateAverageResponseTime 메서드
  - [ ] groupSessionsByCompany 메서드
- [ ] ResumeController에 API 추가
  - [ ] GET /api/applicant/resume/{resumeSlug}/stats

#### Frontend
- [ ] ResumeStatsPage 생성
  - [ ] 상단 통계 카드
  - [ ] 회사별 세션 BarChart
  - [ ] 날짜별 조회수 AreaChart
  - [ ] 응답 시간 PieChart
- [ ] useResumeStats 훅 생성
- [ ] ResumesPage에서 "통계 보기" 버튼 추가
- [ ] 라우팅 설정 (/resumes/:resumeSlug/stats)

### 테스트 & 마무리
- [ ] 대시보드 데이터 정확성 검증
- [ ] 차트 렌더링 테스트
- [ ] 대용량 데이터 성능 테스트
- [ ] 반응형 디자인 확인

---

## Phase 7: 고급 기능 📋

**우선순위**: 낮음 | **예상 기간**: 3-4주

### 7.1 AI 이력서 분석

#### Backend
- [ ] OpenAI GPT Java 라이브러리 추가
- [ ] application.yml에 OpenAI API 키 설정
- [ ] OpenAIService 생성
  - [ ] analyzeResume 메서드
  - [ ] extractKeywords 메서드
  - [ ] generateSuggestions 메서드
- [ ] ResumeAnalysisDto 생성
  - [ ] strengths (강점 리스트)
  - [ ] improvements (개선점 리스트)
  - [ ] keywords (키워드 리스트)
  - [ ] score (점수)
- [ ] PDF 텍스트 추출 로직
  - [ ] Apache PDFBox 사용
- [ ] ResumeController에 API 추가
  - [ ] POST /api/applicant/resume/{resumeSlug}/analyze
  - [ ] @Async 비동기 처리

#### Frontend
- [ ] ResumeAnalysisPage 생성
  - [ ] 분석 요청 버튼
  - [ ] 로딩 상태 표시
  - [ ] 분석 결과 표시
    - [ ] 강점 섹션 (초록색)
    - [ ] 개선점 섹션 (노란색)
    - [ ] 키워드 태그
    - [ ] 점수 게이지
- [ ] useResumeAnalysis 훅 생성
- [ ] ResumesPage에서 "AI 분석" 버튼 추가

### 7.2 캘린더 연동

#### Backend
- [ ] Google Calendar API 라이브러리 추가
- [ ] OAuth2 인증 설정
  - [ ] Google Cloud Console 설정
  - [ ] Redirect URI 등록
- [ ] CalendarService 생성
  - [ ] createMeetingEvent 메서드
  - [ ] getCalendarEvents 메서드
- [ ] GoogleAuthController 생성
  - [ ] GET /api/auth/google/authorize
  - [ ] GET /api/auth/google/callback
- [ ] ChatSession에 meetingEventId 필드 추가
- [ ] ChatController에 미팅 생성 API
  - [ ] POST /api/applicant/chat/{sessionToken}/meeting

#### Frontend
- [ ] GoogleCalendarConnect 버튼
- [ ] MeetingScheduleModal 컴포넌트
  - [ ] 날짜/시간 선택
  - [ ] 제목 입력
  - [ ] 참석자 확인
- [ ] react-datepicker 설치
- [ ] useMeeting 훅 생성
- [ ] 생성된 캘린더 이벤트 링크 표시

### 7.3 화상 면접 연동 (Zoom)

#### Backend
- [ ] Zoom API 라이브러리 추가
- [ ] Zoom OAuth 설정
- [ ] ZoomService 생성
  - [ ] createMeeting 메서드
  - [ ] getMeetingDetails 메서드
  - [ ] deleteMeeting 메서드
- [ ] ChatSession에 zoomMeetingUrl 필드 추가
- [ ] ChatController에 Zoom 미팅 API
  - [ ] POST /api/applicant/chat/{sessionToken}/zoom-meeting

#### Frontend
- [ ] ZoomMeetingButton 컴포넌트
- [ ] ZoomMeetingModal 컴포넌트
  - [ ] 일시 선택
  - [ ] 미팅 생성
  - [ ] 링크 복사
- [ ] 채팅에 Zoom 링크 메시지 표시
- [ ] useZoomMeeting 훅 생성

### 테스트 & 마무리
- [ ] AI 분석 정확도 검증
- [ ] Google Calendar 연동 테스트
- [ ] Zoom 미팅 생성 테스트
- [ ] OAuth 플로우 테스트

---

## Phase 8: 모바일 앱 📋

**우선순위**: 낮음 | **예상 기간**: 4-6주

### 8.1 프로젝트 초기 설정

- [ ] React Native 프로젝트 생성
  - [ ] TypeScript 템플릿
- [ ] 폴더 구조 설정
- [ ] React Navigation 설치
- [ ] React Native Paper 설치 (UI 라이브러리)
- [ ] AsyncStorage 설치
- [ ] Axios 설정

### 8.2 인증 화면

- [ ] LoginScreen 생성
- [ ] SignUpScreen 생성
- [ ] 토큰 저장 (AsyncStorage)
- [ ] 자동 로그인 처리

### 8.3 이력서 관리

- [ ] ResumeListScreen 생성
- [ ] 이미지 선택 (react-native-image-picker)
- [ ] 카메라 촬영 기능
- [ ] PDF 파일 선택 (react-native-document-picker)
- [ ] 업로드 진행률 표시

### 8.4 채팅

- [ ] ChatListScreen 생성
- [ ] ChatScreen 생성
- [ ] 키보드 처리 (KeyboardAvoidingView)
- [ ] WebSocket 연동
- [ ] 타이핑 인디케이터
- [ ] 이미지 메시지

### 8.5 푸시 알림

- [ ] Firebase Cloud Messaging 설정
- [ ] iOS APNs 설정
- [ ] Notification 권한 요청
- [ ] 백그라운드 알림 처리
- [ ] 알림 탭 시 채팅 이동

### 8.6 생체 인증

- [ ] react-native-biometrics 설치
- [ ] Face ID / Touch ID 설정
- [ ] 로그인 시 생체 인증 옵션

### 8.7 배포

- [ ] iOS
  - [ ] Xcode 프로젝트 설정
  - [ ] App Store Connect 등록
  - [ ] TestFlight 베타 테스트
  - [ ] App Store 출시
- [ ] Android
  - [ ] Keystore 생성
  - [ ] Google Play Console 등록
  - [ ] 내부 테스트
  - [ ] Play Store 출시

---

## 기타 작업 📝

### 문서화
- [ ] API 문서 Swagger 최신화
- [ ] README.md 업데이트
- [ ] 사용자 가이드 작성
- [ ] 개발자 가이드 작성
- [ ] 배포 가이드 작성

### 테스트
- [ ] 단위 테스트 작성 (JUnit)
- [ ] 통합 테스트 작성
- [ ] E2E 테스트 (Playwright)
- [ ] 부하 테스트 (JMeter)

### DevOps
- [ ] CI/CD 파이프라인 구축 (GitHub Actions)
- [ ] Docker 컨테이너화
- [ ] Nginx 설정
- [ ] SSL 인증서 설정 (Let's Encrypt)
- [ ] 모니터링 설정 (Prometheus, Grafana)
- [ ] 로그 수집 (ELK Stack)

### 성능 최적화
- [ ] 데이터베이스 쿼리 최적화
- [ ] 인덱스 추가
- [ ] 캐싱 전략 (Redis)
- [ ] 이미지 최적화
- [ ] 번들 크기 최적화
- [ ] Lazy Loading 적용

---

**문서 버전**: 1.0
**최종 업데이트**: 2026-03-10
**관련 문서**: [plan.md](./plan.md), [spec.md](./spec.md)
