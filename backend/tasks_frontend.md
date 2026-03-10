# Resume Chat - Frontend 개발 작업 목록

> 이 문서는 [tasks.md](./tasks.md)에서 Frontend 개발 작업만 분리한 체크리스트입니다.

**작성일**: 2026-03-10
**현재 진행**: Phase 2 (실시간 통신 & 알림)

---

## 📊 전체 진행 현황

- [ ] Phase 2: 실시간 통신 & 알림 (Frontend)
- [ ] Phase 3: Apple UX/UI 강화
- [ ] Phase 4: 파일 & 검색 (Frontend)
- [ ] Phase 5: 보안 강화 (Frontend)
- [ ] Phase 6: 분석 & 통계 (Frontend)
- [ ] Phase 7: 고급 기능 (Frontend)
- [ ] Phase 8: 모바일 앱

---

## Phase 2: 실시간 통신 & 알림 (Frontend) 🔄

**우선순위**: 높음

### 2.1 WebSocket 실시간 채팅

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

### 2.2 입력 중 표시 (Typing Indicator)

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

- [ ] 온라인 상태 표시 UI
  - [ ] 초록색/회색 원 아이콘
  - [ ] "온라인" / "오프라인" 텍스트
  - [ ] 마지막 접속 시간 표시 (formatRelative)
- [ ] WebSocket으로 presence 구독
- [ ] 상태 변경 시 실시간 업데이트

### 2.5 브라우저 푸시 알림

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

- [ ] NotificationSettingsPage 생성
  - [ ] 이메일 알림 on/off 토글
  - [ ] 푸시 알림 on/off 토글
  - [ ] 저장 버튼
- [ ] useNotificationSettings 훅 생성
- [ ] 라우팅 설정 (/settings/notifications)
- [ ] 네비게이션 메뉴에 링크 추가

---

## Phase 3: Apple UX/UI 강화 📋

**우선순위**: 중간

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

## Phase 4: 파일 & 검색 (Frontend) 📋

**우선순위**: 중간

### 4.1 채팅 내 파일 첨부

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

- [ ] react-pdf, pdfjs-dist 설치
- [ ] PdfViewer 컴포넌트 생성
  - [ ] Document, Page 렌더링
  - [ ] 페이지 네비게이션 (이전/다음)
  - [ ] 확대/축소 기능
  - [ ] 전체화면 모드
- [ ] Modal 컴포넌트에 PdfViewer 통합
- [ ] ResumesPage에 "미리보기" 버튼 추가
- [ ] PDF Worker 설정

### 4.3 채팅 내 검색

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

- [ ] ExportModal 컴포넌트
  - [ ] 포맷 선택 (PDF/TXT/JSON)
  - [ ] 첨부파일 포함 체크박스
  - [ ] 날짜 범위 선택 (optional)
- [ ] 내보내기 버튼 (ChatPage)
- [ ] useExportChat 훅 생성
- [ ] 진행 상태 표시 (Toast)

---

## Phase 5: 보안 강화 (Frontend) 📋

**우선순위**: 높음

### 5.1 2단계 인증 (2FA)

- [ ] qrcode.react 설치
- [ ] TwoFactorSetupPage 생성
  - [ ] QR 코드 표시
  - [ ] Secret 키 표시 (수동 입력용)
  - [ ] 인증 코드 입력 (6자리)
  - [ ] 백업 코드 표시 및 다운로드
- [ ] TwoFactorVerifyPage 생성 (로그인 후)
- [ ] SecuritySettingsPage에 2FA 토글 추가

### 5.2 채용담당자 이메일 인증

- [ ] 이메일 인증 안내 UI
  - [ ] 노란색 알림 박스
  - [ ] 재전송 버튼
- [ ] EmailVerificationPage 생성
  - [ ] 성공/실패 메시지
  - [ ] 채팅으로 이동 버튼

### 5.3 Rate Limiting

- [ ] Rate limit 에러 처리
  - [ ] 429 에러 감지
  - [ ] "너무 많은 요청" 메시지
  - [ ] Retry-After 헤더 파싱

### 5.4 reCAPTCHA

- [ ] react-google-recaptcha 설치
- [ ] ReCAPTCHA 컴포넌트 추가
  - [ ] JoinPage
  - [ ] LoginPage (조건부)
  - [ ] RecruiterChatPage
- [ ] 환경변수 설정 (SITE_KEY)

### 5.5 악성 사용자 차단 (관리자)

- [ ] AdminBlockListPage 생성
  - [ ] 차단 목록 조회
  - [ ] 차단 추가 폼
  - [ ] 차단 해제 버튼

---

## Phase 6: 분석 & 통계 (Frontend) 📋

**우선순위**: 낮음

### 6.1 지원자 대시보드

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

- [ ] ResumeStatsPage 생성
  - [ ] 상단 통계 카드
  - [ ] 회사별 세션 BarChart
  - [ ] 날짜별 조회수 AreaChart
  - [ ] 응답 시간 PieChart
- [ ] useResumeStats 훅 생성
- [ ] ResumesPage에서 "통계 보기" 버튼 추가
- [ ] 라우팅 설정 (/resumes/:resumeSlug/stats)

---

## Phase 7: 고급 기능 (Frontend) 📋

**우선순위**: 낮음

### 7.1 AI 이력서 분석

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

- [ ] GoogleCalendarConnect 버튼
- [ ] MeetingScheduleModal 컴포넌트
  - [ ] 날짜/시간 선택
  - [ ] 제목 입력
  - [ ] 참석자 확인
- [ ] react-datepicker 설치
- [ ] useMeeting 훅 생성
- [ ] 생성된 캘린더 이벤트 링크 표시

### 7.3 화상 면접 연동 (Zoom)

- [ ] ZoomMeetingButton 컴포넌트
- [ ] ZoomMeetingModal 컴포넌트
  - [ ] 일시 선택
  - [ ] 미팅 생성
  - [ ] 링크 복사
- [ ] 채팅에 Zoom 링크 메시지 표시
- [ ] useZoomMeeting 훅 생성

---

## Phase 8: 모바일 앱 📋

**우선순위**: 낮음

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

**문서 버전**: 1.0
**최종 업데이트**: 2026-03-10
**관련 문서**: [tasks.md](./tasks.md), [plan.md](./plan.md), [spec.md](./spec.md)
