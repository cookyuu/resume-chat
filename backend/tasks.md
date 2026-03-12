# Resume Chat - Backend 개발 작업 목록

> 이 문서는 [plan.md](./plan.md)의 Backend 개발 작업만 체크리스트 형식으로 정리한 문서입니다.
> Frontend 작업은 [tasks_frontend.md](./tasks_frontend.md)를 참고하세요.

**작성일**: 2026-03-10
**현재 진행**: Phase 2 (실시간 통신 & 알림)

---

## 📊 전체 진행 현황

- [x] Phase 0: 프로젝트 초기 설정
- [x] Phase 1: MVP - 핵심 기능
- [ ] Phase 2: 실시간 통신 & 알림 (진행 중)
- [ ] Phase 4: 파일 & 검색
- [ ] Phase 5: 보안 강화
- [ ] Phase 6: 분석 & 통계
- [ ] Phase 7: 고급 기능

---

## Phase 2: 실시간 통신 & 알림 🔄

**우선순위**: 높음 | **예상 기간**: 2-3주

### 2.1 WebSocket 실시간 채팅

#### Backend
- [x] Spring WebSocket 의존성 추가 (build.gradle)
- [x] WebSocketConfig 클래스 생성
  - [x] MessageBroker 설정 (/topic)
  - [x] Application destination prefix 설정 (/app)
  - [x] STOMP endpoint 등록 (/ws)
  - [x] SockJS fallback 설정
  - [x] CORS 설정 추가
- [x] ChatWebSocketController 생성
  - [x] @MessageMapping 메시지 수신 핸들러
  - [x] @SendTo 브로드캐스트 설정
  - [x] 메시지 저장 로직 연동
- [x] WebSocket 보안 설정
  - [x] JWT 인증 통합
  - [x] 세션별 접근 권한 검증

#### 테스트
- [x] WebSocket 연결 테스트
- [x] 실시간 메시지 송수신 테스트
- [x] 여러 클라이언트 동시 접속 테스트
- [x] 재연결 시나리오 테스트

### 2.2 입력 중 표시 (Typing Indicator)

#### Backend
- [x] TypingEvent DTO 생성 ✅
  - [x] sessionToken 필드
  - [x] senderName 필드
  - [x] senderType 필드
  - [x] typing (boolean) 필드
  - [x] timestamp 필드
- [x] ChatWebSocketController에 typing 핸들러 추가 ✅
  - [x] @MessageMapping("/chat/{sessionToken}/typing")
  - [x] @SendTo("/topic/session/{sessionToken}/typing")
  - [x] 서버 타임스탬프 자동 설정
  - [x] Javadoc 문서화 완료

### 2.3 온라인 상태 표시

#### Backend
- [x] PresenceService 생성 ✅
  - [x] ConcurrentHashMap으로 온라인 사용자 관리
  - [x] userConnected 메서드
  - [x] userDisconnected 메서드
  - [x] broadcastPresence 메서드 (broadcastPresenceUpdate)
  - [x] getConnectedUsers 메서드
  - [x] isUserConnected 메서드
- [x] WebSocket 연결/해제 이벤트 리스너 ✅
  - [x] WebSocketEventListener 클래스 생성
  - [x] SessionConnectEvent 핸들러
  - [x] SessionDisconnectEvent 핸들러
  - [x] 지원자/채용담당자 구분 처리
- [x] PresenceEvent DTO 생성 ✅
  - [x] PresenceUpdate 클래스 (eventType, userIdentifier, senderType, displayName, timestamp, totalConnected)
  - [x] UserPresence 클래스 (접속 정보 관리)
  - [x] PresenceEventType enum (CONNECTED, DISCONNECTED)

#### 테스트
- [x] PresenceServiceTest 작성 완료 (8/8 통과) ✅

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

### 테스트 & 마무리
- [ ] 전체 WebSocket 통합 테스트
- [ ] 이메일 발송 테스트
- [ ] 푸시 알림 테스트 (Chrome, Safari)
- [ ] 성능 테스트 (동시 접속 100명)
- [ ] 문서 업데이트

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

### 4.2 이력서 PDF 미리보기

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

### 테스트 & 마무리
- [ ] AI 분석 정확도 검증
- [ ] Google Calendar 연동 테스트
- [ ] Zoom 미팅 생성 테스트
- [ ] OAuth 플로우 테스트

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
**관련 문서**: [tasks_frontend.md](./tasks_frontend.md), [plan.md](./plan.md), [spec.md](./spec.md)
