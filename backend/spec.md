# Resume Chat - Product Requirements Document (PRD)

## 1. 제품 개요

### 1.1 제품명
**Resume Chat** - 이력서 기반 실시간 채팅 플랫폼

### 1.2 제품 비전
지원자가 이력서를 업로드하고 고유한 채팅 링크를 생성하여, 채용담당자가 별도의 회원가입 없이 해당 링크를 통해 지원자와 직접 소통할 수 있는 간편한 채용 커뮤니케이션 플랫폼

### 1.3 핵심 가치 제안
- **지원자**: 이력서를 업로드하고 링크만 공유하면 여러 채용담당자와 동시에 소통 가능
- **채용담당자**: 회원가입 없이 링크만으로 지원자와 즉시 대화 시작
- **간편성**: 복잡한 채용 프로세스 없이 빠르고 직접적인 커뮤니케이션

### 1.4 타겟 사용자
- **주 사용자 (지원자)**: 구직 중인 개발자, 디자이너 등 전문 인력
- **부 사용자 (채용담당자)**: 스타트업, 중소기업 채용 담당자 및 리크루터

---

## 2. 기술 스택

### 2.1 Backend
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: MySQL
- **ORM**: Spring Data JPA
- **Authentication**: Spring Security + JWT
- **API Documentation**: Swagger/OpenAPI 3.0 (SpringDoc)
- **Build Tool**: Gradle

### 2.2 Frontend
- **Framework**: React 19
- **Language**: TypeScript 5.7
- **Build Tool**: Vite 6
- **State Management**:
  - Zustand 5.0 (전역 상태 관리)
  - Tanstack Query 5.0 (서버 상태 관리)
- **Routing**: React Router DOM 6.28
- **Styling**: Tailwind CSS 3.4
- **UI/UX Philosophy**: Apple Design Guidelines 기반
- **HTTP Client**: Axios 1.7
- **Toast Notifications**: React Hot Toast
- **Date Handling**: Day.js 1.11
- **Architecture**: Feature-Sliced Design (FSD)
- **포트**: 3000 (개발 환경)

**프로젝트 구조 (Feature-Sliced Design)**:
```
src/
├── app/              # 앱 초기화, 라우팅, 레이아웃
│   ├── layouts/      # 공통 레이아웃 컴포넌트
│   ├── providers.tsx # 전역 Provider 설정
│   └── router.tsx    # 라우팅 설정
├── pages/            # 페이지 컴포넌트
│   ├── join/         # 회원가입 페이지
│   ├── login/        # 로그인 페이지
│   ├── resume/       # 이력서 관리 페이지
│   └── chat/         # 채팅 페이지
├── features/         # 비즈니스 로직 (도메인별)
│   ├── auth/         # 인증 관련 (API, hooks)
│   ├── resume/       # 이력서 관련
│   └── chat/         # 채팅 관련
├── shared/           # 공통 모듈
│   ├── ui/           # 재사용 가능한 UI 컴포넌트
│   ├── api/          # API 클라이언트 설정
│   ├── lib/          # 유틸리티 함수
│   ├── store/        # 전역 상태 관리
│   └── types/        # 공통 타입 정의
└── index.css         # 글로벌 스타일
```

### 2.3 Infrastructure
- **File Storage**: Local File System (./uploads/resumes)
- **지원 파일 형식**: PDF
- **최대 파일 크기**: 10MB

---

## 2.4 Apple UX/UI 디자인 시스템

### 2.4.1 디자인 철학
Resume Chat은 Apple의 Human Interface Guidelines를 기반으로 한 사용자 경험을 제공합니다.

**핵심 원칙**:
- **명확성 (Clarity)**: 텍스트는 읽기 쉽고, 아이콘은 명확하며, 기능은 명백해야 함
- **존중 (Deference)**: UI가 콘텐츠를 방해하지 않고, 콘텐츠가 주인공이 되도록 함
- **깊이 (Depth)**: 시각적 레이어와 애니메이션으로 계층 구조와 활력 제공

### 2.4.2 타이포그래피

**San Francisco 폰트 시스템**:
```css
font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
             'Helvetica Neue', Arial, sans-serif;
-webkit-font-smoothing: antialiased;
-moz-osx-font-smoothing: grayscale;
```

**텍스트 크기 시스템** (Tailwind 기반):
- **Large Title**: `text-2xl` (24px) - 페이지 제목
- **Title 1**: `text-xl` (20px) - 섹션 제목
- **Title 2**: `text-lg` (18px) - 서브 섹션
- **Headline**: `text-base` (16px) - 카드 제목
- **Body**: `text-sm` (14px) - 본문, 입력 필드
- **Caption**: `text-xs` (12px) - 부가 정보, 메타데이터

**폰트 굵기**:
- Regular: `font-normal` (400) - 일반 텍스트
- Medium: `font-medium` (500) - 강조
- Semibold: `font-semibold` (600) - 제목, 버튼
- Bold: `font-bold` (700) - 주요 제목

### 2.4.3 컬러 시스템

**Primary Colors (Blue)**:
- Blue 50: `#EFF6FF` - 배경, Hover 상태
- Blue 100: `#DBEAFE` - 라이트 버튼 배경
- Blue 600: `#2563EB` - 주 액션 버튼, 링크
- Blue 700: `#1D4ED8` - Hover 상태

**Neutral Colors (Gray)**:
- Gray 50: `#F9FAFB` - 페이지 배경
- Gray 100: `#F3F4F6` - 카드 배경, Secondary 버튼
- Gray 200: `#E5E7EB` - 테두리, Divider
- Gray 300: `#D1D5DB` - Input 테두리
- Gray 400: `#9CA3AF` - Placeholder, 비활성 텍스트
- Gray 500: `#6B7280` - Secondary 텍스트
- Gray 600: `#4B5563` - 기본 텍스트
- Gray 700: `#374151` - 강조 텍스트
- Gray 900: `#111827` - 헤딩

**Semantic Colors**:
- Success (Green): `#10B981` - 성공 메시지
- Error (Red): `#EF4444` - 에러, 삭제 버튼
- Warning (Yellow): `#F59E0B` - 경고

### 2.4.4 간격 시스템 (Spacing)

Apple은 8pt 그리드 시스템을 사용합니다 (Tailwind 기본값과 동일):
- **4px** (`space-1`) - 최소 간격
- **8px** (`space-2`) - 아이콘과 텍스트 사이
- **12px** (`space-3`) - 관련 요소 사이
- **16px** (`space-4`) - 섹션 내 요소 사이
- **24px** (`space-6`) - 섹션 사이
- **32px** (`space-8`) - 페이지 여백
- **48px** (`space-12`) - 큰 여백

### 2.4.5 모서리 반경 (Border Radius)

**iOS/macOS 스타일 라운딩**:
- **Small**: `rounded-lg` (8px) - 버튼, Input, 카드
- **Medium**: `rounded-xl` (12px) - 모달, 큰 카드
- **Large**: `rounded-2xl` (16px) - 채팅 버블
- **Full**: `rounded-full` (9999px) - 원형 버튼, 입력창

### 2.4.6 그림자 (Elevation)

**iOS 스타일 섬세한 그림자**:
```css
/* Card */
box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);

/* Modal */
box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);

/* Floating */
box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1);
```

Tailwind 클래스:
- `shadow-sm` - 카드, 버튼
- `shadow` - 드롭다운, 팝오버
- `shadow-lg` - 모달
- `shadow-xl` - 최상위 레이어

### 2.4.7 애니메이션 & 트랜지션

**iOS 스타일 부드러운 애니메이션**:
```css
/* 기본 트랜지션 */
transition: all 150ms cubic-bezier(0.4, 0, 0.2, 1);

/* Ease In Out (iOS 기본) */
transition-timing-function: cubic-bezier(0.42, 0, 0.58, 1);

/* Spring 애니메이션 */
transition-timing-function: cubic-bezier(0.175, 0.885, 0.32, 1.275);
```

**적용 예시**:
- 버튼 Hover: 150ms
- 페이지 전환: 300ms
- 모달 등장: 200ms ease-out
- 채팅 메시지 추가: Slide up + Fade in

### 2.4.8 UI 컴포넌트 가이드라인

#### Button
**Apple 스타일 버튼**:
```tsx
// Primary Button
- 배경: Blue 600
- 텍스트: White
- 높이: 40px (py-2.5)
- 패딩: 16px 좌우 (px-4)
- 모서리: rounded-lg (8px)
- Hover: Blue 700 + shadow-sm
- Active: Scale 0.98
- Disabled: 투명도 50%

// Secondary Button
- 배경: Gray 100
- 텍스트: Gray 700
- 나머지 동일

// Text Button
- 배경: Transparent
- 텍스트: Blue 600
- 밑줄 없음, Hover 시 투명도 80%
```

#### Input Field
**iOS 스타일 입력 필드**:
```tsx
- 높이: 44px (터치 영역 확보)
- 패딩: 12px 좌우
- 테두리: 1px solid Gray 300
- 모서리: rounded-lg (8px)
- Focus: Blue 600 테두리 2px + ring
- Label: Gray 700, 14px, Medium
- Placeholder: Gray 400
- 에러: Red 500 테두리 + 하단 에러 메시지
```

#### Card
**macOS 스타일 카드**:
```tsx
- 배경: White
- 테두리: 1px solid Gray 200
- 모서리: rounded-lg (8px)
- 그림자: shadow-sm
- 패딩: 16px ~ 24px
- Hover: 배경 Gray 50 (선택 가능한 경우)
```

#### 채팅 버블 (Message Bubble)
**iMessage 스타일**:
```tsx
// 지원자 메시지 (본인)
- 배경: Blue 600
- 텍스트: White
- 정렬: 오른쪽
- 모서리: rounded-2xl
- 오른쪽 하단만: rounded-br-md (꼬리 효과)
- 최대 너비: 70%

// 채용담당자 메시지
- 배경: White
- 테두리: 1px solid Gray 200
- 텍스트: Gray 900
- 정렬: 왼쪽
- 모서리: rounded-2xl
- 왼쪽 하단만: rounded-bl-md
- 최대 너비: 70%

// 타임스탬프
- 크기: 12px
- 색상: Blue 200 (본인) / Gray 400 (상대방)
- 위치: 버블 하단
```

#### Modal
**iOS 스타일 모달**:
```tsx
- 배경: White
- 모서리: rounded-xl (12px)
- 그림자: shadow-xl
- 등장 애니메이션: Scale 0.95 → 1.0 + Fade in (200ms)
- 배경 오버레이: Black 50% 투명도
- 모바일: 하단에서 슬라이드 업
```

#### Navigation Bar (Header)
**iOS 스타일 네비게이션**:
```tsx
- 높이: 64px (h-16)
- 배경: White
- 하단 테두리: 1px solid Gray 200
- 패딩: 24px 좌우
- 타이틀: 18px, Semibold
- 버튼: Text 버튼 스타일
- Blur 효과: backdrop-blur-sm (스크롤 시)
```

#### Table
**macOS 스타일 테이블**:
```tsx
- 배경: White
- 테두리: rounded-lg + border
- 헤더: Gray 50 배경, Gray 500 텍스트, Medium
- Row: Hover 시 Gray 50 배경
- Divider: Gray 200
- 패딩: 셀당 12px ~ 16px
- 텍스트: 14px
```

### 2.4.9 아이콘 시스템

**SF Symbols 스타일**:
- 크기: 16px, 20px, 24px
- 굵기: Regular (400) 또는 Medium (500)
- 색상: 주변 텍스트와 동일
- 추천 아이콘 라이브러리: **Heroicons** (Tailwind와 호환)

**주요 아이콘**:
- 전송: Paper Airplane
- 링크 복사: Link
- 삭제: Trash
- 업로드: Arrow Up Tray
- 검색: Magnifying Glass
- 설정: Gear
- 알림: Bell
- 메뉴: Bars 3

### 2.4.10 다크 모드 지원

**자동 다크 모드** (Phase 2 구현 예정):
```css
@media (prefers-color-scheme: dark) {
  /* 배경 */
  --bg-primary: #000000;
  --bg-secondary: #1C1C1E;
  --bg-tertiary: #2C2C2E;

  /* 텍스트 */
  --text-primary: #FFFFFF;
  --text-secondary: #EBEBF5;
  --text-tertiary: #8E8E93;

  /* Blue (다크 모드에서 더 밝게) */
  --blue-600: #0A84FF;
}
```

### 2.4.11 반응형 디자인

**Breakpoints** (Tailwind 기본값):
```css
sm: 640px   /* 모바일 가로, 작은 태블릿 */
md: 768px   /* 태블릿 */
lg: 1024px  /* 노트북 */
xl: 1280px  /* 데스크톱 */
2xl: 1536px /* 큰 모니터 */
```

**모바일 최우선 (Mobile-First)**:
- 기본 스타일은 모바일 기준
- `md:` prefix로 태블릿 이상 스타일 추가
- 터치 영역: 최소 44x44px (iOS 권장)
- 폰트 크기: 최소 14px (가독성)

### 2.4.12 접근성 (Accessibility)

**WCAG 2.1 AA 준수**:
- 색상 대비: 최소 4.5:1 (텍스트)
- 포커스 표시: Blue 600 ring (2px)
- 키보드 네비게이션: Tab 순서 논리적
- 스크린 리더: aria-label, role 속성
- 터치 영역: 최소 44x44px

### 2.4.13 로딩 상태

**Skeleton UI** (Apple 스타일):
```tsx
- 배경: Gray 200
- 애니메이션: Pulse (서서히 밝아짐)
- 모서리: 실제 UI와 동일한 radius
- 높이: 실제 콘텐츠와 유사
```

**Loading Spinner**:
```tsx
- 색상: Blue 600
- 크기: 20px (버튼 내부) / 40px (페이지)
- 애니메이션: Smooth rotation
```

### 2.4.14 에러 & 빈 상태

**Empty State**:
```tsx
- 아이콘: Gray 300, 48px
- 제목: Gray 900, 18px, Medium
- 설명: Gray 500, 14px
- CTA 버튼: Primary Button
```

**Toast 알림** (react-hot-toast):
```tsx
// Success
- 배경: White
- 아이콘: Green 500 체크마크
- 위치: 상단 중앙
- 지속: 3초
- 애니메이션: Slide down + Fade in

// Error
- 아이콘: Red 500 X
- 나머지 동일
```

---

## 3. 핵심 기능 명세

### 3.1 사용자 역할 정의

#### 3.1.1 지원자 (Applicant)
- **인증 방식**: JWT 기반 회원가입/로그인 필요
- **권한**: 자신의 이력서 및 채팅 세션에 대한 전체 접근 권한
- **계정 상태**:
  - `ACTIVE`: 활성 계정
  - `INACTIVE`: 비활성 계정 (로그인 5회 실패 시 자동 잠금)
  - `WITHDRAWN`: 탈퇴한 계정

#### 3.1.2 채용담당자 (Recruiter)
- **인증 방식**: 인증 불필요 (Public Access)
- **식별 방식**: 이메일 기반 세션 관리
- **권한**: 이력서 링크를 통한 채팅 세션 생성 및 메시지 전송

---

## 4. 주요 기능 상세

### 4.1 지원자 기능

#### 4.1.1 회원 관리
**회원가입 (POST /api/applicant/join)**
- **입력값**:
  - 이메일 (고유값, 이메일 형식 검증)
  - 이름
  - 비밀번호 (BCrypt 암호화)
- **비즈니스 로직**:
  - 이메일 중복 검사
  - 비밀번호 암호화 저장
  - 기본 상태: `ACTIVE`
- **응답**: 201 Created
- **에러 처리**:
  - 400: 유효성 검증 실패
  - 409: 이메일 중복

**로그인 (POST /api/applicant/login)**
- **입력값**:
  - 이메일
  - 비밀번호
- **클라이언트 타입별 응답** (X-Client-Type 헤더):
  - **WEB**: Refresh Token을 HttpOnly 쿠키로 전달, Access Token만 응답 Body에 포함
  - **APP**: Access Token과 Refresh Token 모두 응답 Body에 포함
- **JWT 토큰**:
  - Access Token: 1시간 유효
  - Refresh Token: 7일 유효
- **보안 정책**:
  - 로그인 실패 시 실패 횟수 증가
  - 5회 연속 실패 시 계정 자동 잠금 (INACTIVE)
  - 로그인 성공 시 실패 횟수 초기화
- **응답**: 200 OK (UUID, 이메일, 이름, 토큰)
- **에러 처리**:
  - 400: 유효성 검증 실패
  - 401: 이메일 또는 비밀번호 불일치
  - 403: 계정 잠김

**프로필 조회 (GET /api/applicant/profile)**
- **인증**: JWT 필수
- **응답**: 사용자 UUID, 이메일, 이름, 가입일
- **에러 처리**:
  - 401: 인증 실패
  - 404: 지원자를 찾을 수 없음

#### 4.1.2 이력서 관리

**이력서 업로드 (POST /api/applicant/resume)**
- **인증**: JWT 필수
- **Content-Type**: multipart/form-data
- **입력값**:
  - file: PDF 파일 (최대 10MB)
  - title: 이력서 제목 (2~100자)
  - description: 이력서 설명 (선택, 최대 500자)
- **비즈니스 로직**:
  - 고유한 resumeSlug(UUID) 자동 생성
  - 파일을 로컬 파일 시스템에 저장
  - 채팅 링크 생성: `{frontend-url}/chat/{resumeSlug}`
- **응답**: 201 Created
  - resumeSlug
  - title, description
  - originalFileName
  - chatLink
  - createdAt
- **에러 처리**:
  - 400: 파일 형식 또는 크기 오류
  - 401: 인증 실패

**이력서 목록 조회 (GET /api/applicant/resume)**
- **인증**: JWT 필수
- **응답**: 본인이 업로드한 모든 이력서 목록
  - resumeSlug, title, description
  - originalFileName
  - chatLink
  - viewCnt (조회수)
  - createdAt
- **에러 처리**:
  - 401: 인증 실패

**이력서 삭제 (DELETE /api/applicant/resume/{resumeSlug})**
- **인증**: JWT 필수
- **비즈니스 로직**:
  - 본인의 이력서만 삭제 가능
  - Cascade 삭제: 관련 채팅 세션 및 메시지 모두 삭제
- **응답**: 200 OK
- **에러 처리**:
  - 401: 인증 실패
  - 403: 권한 없음 (다른 사용자의 이력서)
  - 404: 이력서를 찾을 수 없음

#### 4.1.3 채팅 관리 (지원자)

**이력서별 채팅 세션 목록 조회 (GET /api/applicant/resume/{resumeSlug}/chats)**
- **인증**: JWT 필수
- **응답**:
  - 이력서 정보 (resumeSlug, title)
  - 세션 목록:
    - sessionToken
    - 채용담당자 정보 (email, name, company)
    - totalMessages: 총 메시지 수
    - unreadMessages: 읽지 않은 메시지 수
    - lastMessageAt: 마지막 메시지 시간
    - createdAt: 세션 생성일
- **에러 처리**:
  - 401: 인증 실패
  - 403: 권한 없음
  - 404: 이력서를 찾을 수 없음

**채팅 세션 메시지 조회 (GET /api/applicant/chat/{sessionToken}/messages)**
- **인증**: JWT 필수
- **비즈니스 로직**:
  - 본인의 채팅 세션만 조회 가능
  - 조회 시 읽지 않은 메시지 자동으로 읽음 처리
- **응답**:
  - 세션 정보
  - 메시지 목록:
    - messageId
    - message (내용)
    - senderType: `APPLICANT` | `RECRUITER`
    - readStatus: 읽음 여부
    - sentAt: 전송 시간
- **에러 처리**:
  - 401: 인증 실패
  - 403: 권한 없음
  - 404: 채팅 세션을 찾을 수 없음

**지원자 메시지 전송 (POST /api/applicant/chat/{sessionToken}/send)**
- **인증**: JWT 필수
- **입력값**:
  - message: 메시지 내용 (필수)
- **비즈니스 로직**:
  - 본인의 채팅 세션에만 전송 가능
  - SenderType: `APPLICANT`
  - 초기 readStatus: false
- **응답**: 201 Created
  - sessionToken
  - messageId
  - message
  - sentAt
- **에러 처리**:
  - 400: 유효성 검증 실패
  - 401: 인증 실패
  - 403: 권한 없음
  - 404: 채팅 세션을 찾을 수 없음

---

### 4.2 채용담당자 기능

#### 4.2.1 채팅 세션 진입 (POST /api/chat/{resumeSlug}/enter)
- **인증**: 불필요 (Public)
- **입력값**:
  - recruiterName: 채용담당자 이름
  - recruiterEmail: 채용담당자 이메일
  - recruiterCompany: 소속 회사
- **비즈니스 로직**:
  - 기존 세션이 있으면 해당 세션 정보 반환
  - 기존 세션이 없으면 새로 생성
  - 고유한 sessionToken(UUID) 자동 생성
  - 동일한 이력서 + 이메일 조합은 하나의 세션만 존재
- **응답**: 200 OK
  - sessionToken
  - resumeSlug, resumeTitle
  - recruiterEmail, recruiterName, recruiterCompany
  - totalMessages, lastMessageAt
  - createdAt
- **에러 처리**:
  - 400: 유효성 검증 실패
  - 404: 이력서를 찾을 수 없음

#### 4.2.2 메시지 조회 (GET /api/chat/session/{sessionToken}/messages)
- **인증**: 불필요 (Public)
- **응답**:
  - 세션 정보
  - 메시지 목록 (시간순 정렬)
- **에러 처리**:
  - 404: 채팅 세션을 찾을 수 없음

#### 4.2.3 메시지 전송

**방법 1: sessionToken 기반 (POST /api/chat/session/{sessionToken}/send)**
- **인증**: 불필요 (Public)
- **입력값**:
  - message: 메시지 내용
- **사용 시나리오**: 이미 sessionToken을 알고 있는 경우 (재방문)
- **응답**: 201 Created

**방법 2: resumeSlug 기반 (POST /api/chat/{resumeSlug}/send)**
- **인증**: 불필요 (Public)
- **입력값**:
  - recruiterName, recruiterEmail, recruiterCompany
  - message: 메시지 내용
- **비즈니스 로직**:
  - 첫 요청 시 세션 생성 + 메시지 전송
  - 기존 세션 있으면 해당 세션에 메시지 추가
- **사용 시나리오**: 최초 방문 시 또는 sessionToken 없이 링크로 접근한 경우
- **응답**: 201 Created
  - sessionToken (세션 식별용)
  - messageId, message, sentAt

**공통 로직**:
- SenderType: `RECRUITER`
- 초기 readStatus: false
- 세션의 totalMessages 자동 증가
- lastMessageAt 자동 갱신

---

## 5. 데이터 모델

### 5.1 Entity 관계도

```
Applicant (지원자)
    └─── Resume (이력서) [1:N]
            └─── ChatSession (채팅 세션) [1:N]
                    └─── ChatMessage (채팅 메시지) [1:N]
```

### 5.2 Entity 상세

#### Applicant (rc_applicant)
```
- id: BIGINT (PK, Auto Increment)
- uuid: UUID (Unique, Not Null) - API 식별자
- email: VARCHAR (Unique, Not Null)
- name: VARCHAR (Not Null)
- password: VARCHAR (Not Null) - BCrypt 암호화
- status: ENUM (ApplicantStatus) - ACTIVE, INACTIVE, WITHDRAWN
- loginFailCnt: INT (Default: 0)
- createdAt: TIMESTAMP
- updatedAt: TIMESTAMP
```

#### Resume (rc_resume)
```
- id: BIGINT (PK, Auto Increment)
- resumeSlug: UUID (Unique, Not Null) - 채팅 링크 식별자
- applicantId: BIGINT (FK, Not Null)
- title: VARCHAR (Not Null)
- description: TEXT
- filePath: VARCHAR (Not Null) - 파일 저장 경로
- originalFileName: VARCHAR - 원본 파일명
- viewCnt: INT (Default: 0) - 조회수
- createdAt: TIMESTAMP
- updatedAt: TIMESTAMP

Indexes:
- UNIQUE (resumeSlug)
- FK (applicantId) REFERENCES Applicant(id)

Relations:
- ManyToOne with Applicant
- OneToMany with ChatSession (Cascade ALL, Orphan Removal)
```

#### ChatSession (rc_chat_session)
```
- id: BIGINT (PK, Auto Increment)
- sessionToken: VARCHAR (Unique, Not Null) - UUID String
- resumeId: BIGINT (FK, Not Null)
- recruiterName: VARCHAR (Not Null)
- recruiterEmail: VARCHAR (Not Null)
- recruiterCompany: VARCHAR (Not Null)
- active: BOOLEAN (Default: true)
- totalMessages: BIGINT (Default: 0)
- lastMessageAt: TIMESTAMP
- createdAt: TIMESTAMP
- updatedAt: TIMESTAMP

Constraints:
- UNIQUE (resumeId, recruiterEmail) - 동일 이력서에 동일 이메일은 하나의 세션만

Indexes:
- UNIQUE (sessionToken)
- INDEX (sessionToken) - 빠른 조회

Relations:
- ManyToOne with Resume
- OneToMany with ChatMessage (Cascade ALL, Orphan Removal)
```

#### ChatMessage (rc_chat_message)
```
- id: BIGINT (PK, Auto Increment)
- messageId: UUID (Unique, Not Null)
- sessionId: BIGINT (FK, Not Null)
- senderType: ENUM (SenderType) - APPLICANT, RECRUITER
- content: TEXT (Not Null)
- readStatus: BOOLEAN (Default: false)
- createdAt: TIMESTAMP
- updatedAt: TIMESTAMP

Indexes:
- FK (sessionId) REFERENCES ChatSession(id)

Relations:
- ManyToOne with ChatSession
```

---

## 6. API 엔드포인트 요약

### 6.1 지원자 API (인증 필요)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/applicant/join | 회원가입 |
| POST | /api/applicant/login | 로그인 |
| GET | /api/applicant/profile | 프로필 조회 |
| POST | /api/applicant/resume | 이력서 업로드 |
| GET | /api/applicant/resume | 이력서 목록 조회 |
| DELETE | /api/applicant/resume/{resumeSlug} | 이력서 삭제 |
| GET | /api/applicant/resume/{resumeSlug}/chats | 이력서별 채팅 세션 목록 |
| GET | /api/applicant/chat/{sessionToken}/messages | 채팅 메시지 조회 |
| POST | /api/applicant/chat/{sessionToken}/send | 메시지 전송 |

### 6.2 채용담당자 API (Public)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/chat/{resumeSlug}/enter | 세션 진입 (조회/생성) |
| GET | /api/chat/session/{sessionToken}/messages | 메시지 조회 |
| POST | /api/chat/{resumeSlug}/send | 첫 메시지 전송 (세션 생성) |
| POST | /api/chat/session/{sessionToken}/send | 메시지 전송 |

---

## 7. 보안 및 인증

### 7.1 JWT 인증
- **알고리즘**: HS256
- **Secret Key**: Base64 인코딩된 키 (application.yml 설정)
- **Access Token**: 1시간 유효 (3600000ms)
- **Refresh Token**: 7일 유효 (604800000ms)
- **전달 방식**:
  - Authorization 헤더: `Bearer {token}`
  - Refresh Token (WEB): HttpOnly 쿠키

### 7.2 보안 정책
- 비밀번호: BCrypt 암호화
- 로그인 실패: 5회 연속 실패 시 계정 잠금
- CORS: 프론트엔드 도메인(http://localhost:3000) 허용
- 파일 업로드: 확장자 및 크기 검증

### 7.3 권한 제어
- **지원자**: 본인의 이력서 및 채팅 세션만 접근 가능
- **채용담당자**: 이력서 링크를 통한 Public 접근, sessionToken으로 세션 관리

---

## 8. 사용자 플로우

### 8.1 지원자 플로우

```
1. 회원가입 및 로그인
   └─> JWT 토큰 발급

2. 이력서 업로드
   └─> resumeSlug 생성
   └─> 채팅 링크 발급: /chat/{resumeSlug}

3. 채팅 링크 공유
   └─> 이메일, 소셜 미디어, 구인구직 사이트 등

4. 채팅 세션 목록 확인
   └─> 어떤 채용담당자가 연락했는지 확인
   └─> 읽지 않은 메시지 수 확인

5. 채팅 메시지 조회 및 응답
   └─> 메시지 자동 읽음 처리
   └─> 답장 전송
```

### 8.2 채용담당자 플로우

```
1. 채팅 링크 접근
   └─> /chat/{resumeSlug}

2. 정보 입력 (첫 방문 시)
   └─> 이름, 이메일, 회사명 입력
   └─> 세션 생성 (sessionToken 발급)

3. 메시지 전송
   └─> 지원자에게 첫 메시지 전송

4. 대화 이어가기
   └─> 재방문 시 sessionToken으로 기존 세션 유지
   └─> 메시지 조회 및 전송
```

---

## 9. 에러 코드 체계

### 9.1 공통 에러 (C)
- C001: 유효성 검증 실패
- C002: 인증이 필요합니다

### 9.2 지원자 에러 (A)
- A001: 지원자를 찾을 수 없습니다
- A002: 이미 존재하는 이메일입니다
- A004: 계정이 잠겨있습니다
- A005: 이메일 또는 비밀번호가 일치하지 않습니다

### 9.3 이력서 에러 (R)
- R001: 이력서를 찾을 수 없습니다
- R002: 이력서 접근 권한이 없습니다

### 9.4 파일 에러 (F)
- F003: 허용되지 않은 파일 확장자입니다

### 9.5 세션 에러 (S)
- S001: 채팅 세션을 찾을 수 없습니다

---

## 10. 주요 비즈니스 로직

### 10.1 세션 생성 및 관리
- **고유성 보장**: 동일한 `(resumeId, recruiterEmail)` 조합은 하나의 세션만 생성
- **재진입**: 동일한 채용담당자가 재방문 시 기존 세션 재사용
- **세션 토큰**: UUID 기반 sessionToken으로 세션 식별

### 10.2 메시지 읽음 처리
- **지원자가 메시지 조회 시**: 읽지 않은 메시지를 자동으로 읽음 처리
- **채용담당자 조회 시**: 읽음 처리 없이 조회만 수행

### 10.3 Cascade 삭제
- **이력서 삭제 시**: 관련된 모든 채팅 세션 및 메시지 자동 삭제
- **채팅 세션 삭제 시**: 관련된 모든 메시지 자동 삭제

### 10.4 계정 잠금
- 로그인 실패 5회 누적 시 계정 자동 잠금 (INACTIVE)
- 로그인 성공 시 실패 횟수 초기화

---

## 10.5 Frontend 구현 현황

### 10.5.1 구현된 페이지

#### 인증 페이지
**회원가입 (JoinPage)**:
- 위치: `/join`
- 기능:
  - 이메일, 이름, 비밀번호 입력
  - 비밀번호 확인 검증
  - 실시간 유효성 검사 (onBlur)
  - 로딩 상태 표시
- 스타일: 중앙 배치 카드 레이아웃, rounded-xl

**로그인 (LoginPage)**:
- 위치: `/login`
- 기능:
  - 이메일, 비밀번호 입력
  - JWT 토큰 저장 (Zustand)
  - 로그인 성공 시 `/resumes`로 리다이렉트
- 스타일: 회원가입과 동일한 레이아웃

#### 이력서 관리 페이지
**이력서 목록 (ResumesPage)**:
- 위치: `/resumes`
- 기능:
  - 업로드한 이력서 목록 테이블 표시
  - 이력서 업로드 폼 (토글)
  - 채팅 링크 복사 (클립보드)
  - 이력서 삭제 (확인 다이얼로그)
  - 각 이력서별 채팅 이동 버튼
- 스타일:
  - macOS 스타일 테이블
  - Hover 효과
  - 색상별 액션 버튼 (Blue: 채팅, Gray: 링크, Red: 삭제)

#### 채팅 페이지
**이력서별 채팅 세션 목록 (ResumeChatsPage)**:
- 위치: `/resumes/:resumeSlug/chats`
- 기능:
  - 특정 이력서에 대한 모든 채팅 세션 목록
  - 채용담당자 정보 표시 (이름, 회사, 이메일)
  - 읽지 않은 메시지 수 뱃지
  - 마지막 메시지 시간
- 스타일: 카드 리스트, 클릭 시 채팅으로 이동

**채팅방 (ChatPage - 지원자용)**:
- 위치: `/chat/:sessionToken`
- 기능:
  - iMessage 스타일 채팅 UI
  - 메시지 전송 (rounded-full 입력창)
  - 자동 스크롤 (새 메시지 도착 시)
  - 실시간 로딩 상태
  - 타임스탬프 표시
- 스타일:
  - 본인 메시지: Blue 600 배경, 오른쪽 정렬, rounded-br-md
  - 상대방 메시지: White 배경, 왼쪽 정렬, rounded-bl-md
  - 전체 레이아웃: 헤더 + 메시지 영역 + 입력 영역 (고정)

**채용담당자 채팅 (RecruiterChatPage)**:
- 위치: `/chat/:resumeSlug/recruiter`
- 기능:
  - 인증 없이 접근 가능
  - 첫 방문 시 정보 입력 (이름, 이메일, 회사)
  - 이후 sessionToken으로 세션 유지
  - 메시지 송수신
- 스타일: ChatPage와 동일한 iMessage 스타일

#### 프로필 페이지 (구현 예정)
- 위치: `/profile`
- 기능: 사용자 정보 조회 및 수정

### 10.5.2 구현된 UI 컴포넌트

**Button (`/shared/ui/Button.tsx`)**:
```tsx
Props: variant (primary | secondary | danger), loading
스타일:
- Primary: Blue 600, White 텍스트
- Secondary: Gray 100, Gray 700 텍스트
- Danger: Red 600, White 텍스트
- Loading: Spinner 아이콘 + "처리 중..."
- 트랜지션: transition-colors
```

**Input (`/shared/ui/Input.tsx`)**:
```tsx
Props: label, error, ...HTMLInputProps
스타일:
- 높이: py-2 (32px)
- 테두리: border, rounded-lg
- Focus: ring-2 ring-blue-500
- Error: border-red-500 + 하단 에러 메시지
- Label: text-sm font-medium text-gray-700
```

**Skeleton (`/shared/ui/Skeleton.tsx`)**:
```tsx
용도: 로딩 상태 표시
스타일:
- 배경: bg-gray-200
- 애니메이션: animate-pulse
- 모서리: rounded
```

**EmptyState (`/shared/ui/EmptyState.tsx`)**:
```tsx
용도: 빈 목록 표시
Props: message
스타일:
- 중앙 정렬
- Gray 색상
- 아이콘 + 텍스트
```

### 10.5.3 상태 관리

**Auth Store (Zustand)**:
```tsx
// /shared/store/auth.ts
상태:
- user: { uuid, email, name } | null
- accessToken: string | null

액션:
- setAuth(user, token): 로그인
- logout(): 로그아웃 + 토큰 제거
- LocalStorage 연동 (persist)
```

**서버 상태 (Tanstack Query)**:
```tsx
// /features/auth/hooks.ts
- useJoin(): 회원가입 mutation
- useLogin(): 로그인 mutation

// /features/resume/hooks.ts
- useMyResumes(): 이력서 목록 query
- useUploadResume(): 업로드 mutation
- useDeleteResume(): 삭제 mutation

// /features/chat/hooks.ts
- useResumeChats(resumeSlug): 세션 목록 query
- useSessionMessages(sessionToken): 메시지 목록 query
- useSendApplicantMessage(): 지원자 메시지 전송
- useSendRecruiterMessage(): 채용담당자 메시지 전송
```

### 10.5.4 API 클라이언트

**Axios 인스턴스** (`/shared/api/client.ts`):
```tsx
설정:
- baseURL: http://localhost:8080
- Content-Type: application/json
- Interceptor: 자동으로 Authorization 헤더 추가
- 에러 핸들링: Toast 알림 표시
```

### 10.5.5 라우팅 구조

```tsx
// /app/router.tsx
/ (root)
├── /join         - 회원가입
├── /login        - 로그인
└── (인증 필요)
    ├── /resumes                           - 이력서 목록
    ├── /resumes/:resumeSlug/chats         - 채팅 세션 목록
    ├── /chat/:sessionToken                - 채팅방 (지원자)
    └── /profile                           - 프로필

(Public)
└── /chat/:resumeSlug/recruiter            - 채용담당자 채팅
```

### 10.5.6 현재 적용된 Apple 스타일 요소

✅ **구현됨**:
- San Francisco 폰트 시스템 (-apple-system)
- antialiased 폰트 렌더링
- rounded-lg, rounded-xl, rounded-2xl 모서리
- iMessage 스타일 채팅 버블
- Subtle 그림자 (shadow-sm)
- Blue 600 Primary 색상
- Gray 스케일 색상 시스템
- Hover 트랜지션 효과
- 44px 최소 터치 영역 (입력 필드)

⏳ **개선 필요**:
- 더 부드러운 애니메이션 (cubic-bezier)
- 다크 모드 지원
- Backdrop blur 효과
- SF Symbols 스타일 아이콘
- 모달 애니메이션
- Haptic Feedback (모바일)

---

## 11. 추가 구현 권장 기능

### 11.0 Apple 스타일 UI/UX 강화 (우선순위: 최상)

#### 11.0.1 애니메이션 개선
**iOS 스타일 부드러운 트랜지션**:
```tsx
// 현재: transition-colors
// 개선: 커스텀 timing function

// tailwind.config.js 확장
theme: {
  extend: {
    transitionTimingFunction: {
      'ios': 'cubic-bezier(0.42, 0, 0.58, 1)',        // ease-in-out
      'spring': 'cubic-bezier(0.175, 0.885, 0.32, 1.275)', // spring
    },
    transitionDuration: {
      '150': '150ms',
      '250': '250ms',
      '350': '350ms',
    }
  }
}

// 적용 예시
<button className="transition-all duration-150 ease-ios hover:scale-[0.98]">
```

**페이지 전환 애니메이션**:
- Framer Motion 도입
- Fade + Slide 조합
- 300ms duration

#### 11.0.2 Heroicons 통합
**SF Symbols 스타일 아이콘 시스템**:
```bash
npm install @heroicons/react
```

**주요 아이콘 매핑**:
```tsx
import {
  PaperAirplaneIcon,    // 전송
  LinkIcon,             // 링크 복사
  TrashIcon,            // 삭제
  ArrowUpTrayIcon,      // 업로드
  MagnifyingGlassIcon,  // 검색
  Cog6ToothIcon,        // 설정
  BellIcon,             // 알림
  Bars3Icon,            // 메뉴
  ChatBubbleLeftIcon,   // 채팅
  DocumentTextIcon,     // 문서
} from '@heroicons/react/24/outline';

// Solid 버전은 24/solid
```

#### 11.0.3 Backdrop Blur 효과
**iOS 스타일 반투명 배경**:
```tsx
// 네비게이션 바 (스크롤 시)
<header className="sticky top-0 bg-white/80 backdrop-blur-sm border-b">

// 모달 오버레이
<div className="fixed inset-0 bg-black/50 backdrop-blur-sm">

// 하단 입력창 (iOS 키보드 위)
<div className="bg-white/95 backdrop-blur-md border-t">
```

#### 11.0.4 다크 모드 구현
**시스템 테마 연동**:
```tsx
// 1. Tailwind 다크 모드 활성화
// tailwind.config.js
module.exports = {
  darkMode: 'class', // 또는 'media'
  // ...
}

// 2. 다크 모드 토글 훅
// /shared/hooks/useDarkMode.ts
export function useDarkMode() {
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    setIsDark(prefersDark);
  }, []);

  const toggle = () => {
    document.documentElement.classList.toggle('dark');
    setIsDark(!isDark);
  };

  return { isDark, toggle };
}

// 3. 컬러 클래스 변경
<div className="bg-white dark:bg-gray-900 text-gray-900 dark:text-white">
```

**다크 모드 컬러 팔레트**:
```
배경:
- primary: white → #000000
- secondary: gray-50 → #1C1C1E
- tertiary: gray-100 → #2C2C2E

텍스트:
- primary: gray-900 → white
- secondary: gray-600 → #EBEBF5
- tertiary: gray-400 → #8E8E93

Blue (더 밝게):
- blue-600 → #0A84FF
```

#### 11.0.5 마이크로 인터랙션
**버튼 피드백**:
```tsx
<button className="
  active:scale-95
  transition-transform duration-100
  hover:shadow-md
">
```

**입력 필드 포커스**:
```tsx
<input className="
  focus:ring-2 focus:ring-blue-500
  focus:border-transparent
  transition-all duration-200
">
```

**토스트 애니메이션**:
```tsx
// react-hot-toast 커스터마이징
import { Toaster } from 'react-hot-toast';

<Toaster
  position="top-center"
  toastOptions={{
    duration: 3000,
    style: {
      background: '#fff',
      color: '#374151',
      borderRadius: '12px',
      padding: '12px 16px',
      fontSize: '14px',
      boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
    },
    success: {
      iconTheme: {
        primary: '#10B981',
        secondary: '#fff',
      },
    },
    error: {
      iconTheme: {
        primary: '#EF4444',
        secondary: '#fff',
      },
    },
  }}
/>
```

#### 11.0.6 Pull to Refresh (모바일)
**iOS 네이티브 스타일 새로고침**:
```tsx
import { useState, useRef } from 'react';

function usePullToRefresh(onRefresh: () => Promise<void>) {
  const [pulling, setPulling] = useState(false);
  const startY = useRef(0);

  const handleTouchStart = (e: TouchEvent) => {
    if (window.scrollY === 0) {
      startY.current = e.touches[0].clientY;
    }
  };

  const handleTouchMove = (e: TouchEvent) => {
    const currentY = e.touches[0].clientY;
    if (currentY - startY.current > 80) {
      setPulling(true);
    }
  };

  const handleTouchEnd = async () => {
    if (pulling) {
      await onRefresh();
      setPulling(false);
    }
  };

  return { pulling, handleTouchStart, handleTouchMove, handleTouchEnd };
}
```

#### 11.0.7 Haptic Feedback (iOS/Android)
**버튼 클릭 시 진동 피드백**:
```tsx
function triggerHaptic(type: 'light' | 'medium' | 'heavy' = 'light') {
  if ('vibrate' in navigator) {
    const patterns = {
      light: [10],
      medium: [20],
      heavy: [50],
    };
    navigator.vibrate(patterns[type]);
  }
}

// 사용 예시
<button onClick={() => {
  triggerHaptic('light');
  handleSubmit();
}}>
```

#### 11.0.8 Skeleton 개선
**Apple 스타일 Shimmer 효과**:
```tsx
// /shared/ui/Skeleton.tsx 개선
export function Skeleton({ className }: { className?: string }) {
  return (
    <div className={`animate-pulse bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 bg-[length:200%_100%] ${className}`}>
      <style jsx>{`
        @keyframes shimmer {
          0% { background-position: -200% 0; }
          100% { background-position: 200% 0; }
        }
        .animate-shimmer {
          animation: shimmer 2s infinite;
        }
      `}</style>
    </div>
  );
}
```

#### 11.0.9 Context Menu (Right-Click / Long Press)
**iOS 스타일 컨텍스트 메뉴**:
```tsx
// 이력서 목록, 채팅 메시지에 적용
- 길게 누르면 메뉴 표시
- 복사, 삭제, 공유 등
- Blur 배경 + 모서리 둥글게
- 애니메이션: Scale up + Fade in
```

#### 11.0.10 스와이프 제스처
**iMessage 스타일 스와이프**:
```tsx
// 채팅 메시지 왼쪽 스와이프 → 답장
// 이력서 목록 왼쪽 스와이프 → 삭제

import { useSwipeable } from 'react-swipeable';

const handlers = useSwipeable({
  onSwipedLeft: () => handleDelete(),
  onSwipedRight: () => handleReply(),
  preventScrollOnSwipe: true,
  trackMouse: true,
});

<div {...handlers}>메시지</div>
```

---

### 11.1 실시간 통신 기능 (우선순위: 높음)

#### 11.1.1 WebSocket 기반 실시간 채팅
**현재 한계**:
- 폴링 방식으로 새 메시지 확인 시 지연 발생
- 서버 리소스 낭비 (불필요한 반복 요청)

**개선 방안**:
- **기술 스택**: Spring WebSocket + STOMP + SockJS
- **구현 내용**:
  - `/ws` 엔드포인트로 WebSocket 연결
  - 채팅 세션별 토픽 구독: `/topic/session/{sessionToken}`
  - 메시지 전송 시 실시간 브로드캐스트
  - 연결 끊김 시 자동 재연결 처리
- **예상 효과**:
  - 메시지 전송/수신 즉시 반영
  - 서버 부하 50% 이상 감소
  - 사용자 경험 대폭 개선

**구현 예시**:
```java
@MessageMapping("/chat/{sessionToken}")
@SendTo("/topic/session/{sessionToken}")
public ChatMessage sendMessage(@DestinationVariable String sessionToken, ChatMessage message) {
    // 메시지 저장 및 브로드캐스트
}
```

#### 11.1.2 입력 중 표시 (Typing Indicator)
- 상대방이 타이핑 중일 때 "입력 중..." 표시
- WebSocket 이벤트로 실시간 전달
- 3초간 입력 없으면 자동 해제

#### 11.1.3 온라인 상태 표시
- 지원자/채용담당자 온라인 여부 실시간 표시
- 마지막 접속 시간 표시 (오프라인 시)
- Presence 관리 시스템 구현

---

### 11.2 알림 시스템 (우선순위: 높음)

#### 11.2.1 이메일 알림
**트리거 이벤트**:
- 새 채팅 메시지 수신 시
- 새 채용담당자가 세션 생성 시
- 24시간 이상 답장 없을 때 리마인더

**구현 방안**:
- **기술 스택**: Spring Mail + 비동기 처리 (@Async)
- **템플릿**: Thymeleaf 또는 HTML 템플릿
- **설정 옵션**: 사용자가 알림 on/off 설정 가능

**이메일 내용 예시**:
```
제목: [Resume Chat] 새 메시지가 도착했습니다
내용:
- 채용담당자: {recruiterName} ({recruiterCompany})
- 메시지 미리보기: "{message}"
- 바로가기: {채팅 링크}
```

#### 11.2.2 브라우저 푸시 알림
- Web Push API 활용
- 서비스 워커 등록
- 백그라운드에서도 알림 수신

#### 11.2.3 알림 센터
- 모든 알림 내역 확인
- 읽음/안읽음 표시
- 알림 클릭 시 해당 채팅으로 이동

---

### 11.3 파일 및 미디어 기능 (우선순위: 중간)

#### 11.3.1 채팅 내 파일 첨부
**지원 파일**:
- 문서: PDF, DOCX, PPTX (최대 10MB)
- 이미지: JPG, PNG, GIF (최대 5MB)
- 압축 파일: ZIP (최대 20MB)

**구현 내용**:
- 파일 업로드 API 추가
- 파일 메타데이터 저장 (파일명, 크기, MIME 타입)
- 다운로드 링크 생성
- 바이러스 스캔 (ClamAV 연동)

**데이터 모델 추가**:
```java
@Entity
class ChatAttachment {
    private Long id;
    private ChatMessage message;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
}
```

#### 11.3.2 이력서 미리보기
- PDF 뷰어 임베딩 (PDF.js)
- 채용담당자가 채팅 중 이력서 바로 확인
- 다운로드 없이 브라우저에서 직접 열람
- 확대/축소, 페이지 이동 기능

#### 11.3.3 이미지 인라인 표시
- 이미지 파일 전송 시 채팅창에 직접 표시
- 썸네일 생성 (ImageMagick)
- 라이트박스 모달로 원본 보기

---

### 11.4 검색 및 필터링 (우선순위: 중간)

#### 11.4.1 채팅 내 검색
- 특정 세션 내 메시지 전체 검색
- 키워드 하이라이트
- 검색 결과 페이지네이션
- Elasticsearch 연동 (대용량 데이터 대비)

**API 예시**:
```
GET /api/applicant/chat/{sessionToken}/search?q=면접&page=1
```

#### 11.4.2 채팅 세션 필터링
- 읽지 않은 메시지가 있는 세션만
- 특정 회사의 채용담당자만
- 날짜 범위 필터링
- 활성/비활성 세션 구분

#### 11.4.3 이력서 검색 (지원자)
- 제목 또는 설명으로 내 이력서 검색
- 생성일 기준 정렬
- 조회수 기준 정렬

---

### 11.5 데이터 관리 기능 (우선순위: 중간)

#### 11.5.1 채팅 히스토리 내보내기
**지원 형식**:
- PDF: 보기 좋은 포맷으로 대화 내용 정리
- TXT: 플레인 텍스트
- JSON: 데이터 백업용

**구현 내용**:
- 비동기 작업으로 처리
- 생성 완료 시 다운로드 링크 이메일 전송
- 24시간 후 자동 삭제

**API 예시**:
```
POST /api/applicant/chat/{sessionToken}/export
{
  "format": "pdf",
  "includeAttachments": true
}
```

#### 11.5.2 채팅 세션 아카이브
- 종료된 채팅을 아카이브로 이동
- 아카이브된 세션은 읽기 전용
- 복원 기능 제공
- 스토리지 절약 (압축 저장)

#### 11.5.3 이력서 버전 관리
**현재 한계**:
- 이력서 수정 시 이전 버전 복구 불가

**개선 방안**:
- 이력서 업데이트 시 기존 버전 보관
- 버전 히스토리 조회
- 특정 버전으로 롤백
- 버전별 채팅 링크는 동일하게 유지

**데이터 모델**:
```java
@Entity
class ResumeVersion {
    private Long id;
    private Resume resume;
    private Integer version;
    private String filePath;
    private LocalDateTime versionCreatedAt;
}
```

---

### 11.6 사용자 경험 개선 (우선순위: 중간)

#### 11.6.1 이력서 템플릿 제공
- 다양한 산업별 이력서 템플릿
- 드래그 앤 드롭 빌더
- 실시간 미리보기
- 마크다운 지원

#### 11.6.2 빠른 응답 (Quick Reply)
- 자주 사용하는 문구 저장
- 버튼 클릭으로 빠르게 전송
- 예시: "감사합니다", "검토 후 연락드리겠습니다"

#### 11.6.3 북마크/즐겨찾기
- 중요한 채팅 세션 즐겨찾기 등록
- 즐겨찾기한 세션 상단 고정
- 별표 아이콘으로 시각적 표시

#### 11.6.4 다크 모드
- 사용자 설정에 따른 테마 변경
- 시스템 설정 자동 감지
- LocalStorage에 설정 저장

---

### 11.7 분석 및 통계 (우선순위: 낮음)

#### 11.7.1 지원자 대시보드
**표시 정보**:
- 총 이력서 업로드 수
- 총 채팅 세션 수
- 이력서별 조회수 그래프
- 채용담당자별 응답률
- 시간대별 메시지 활동량
- 가장 많이 연락받은 이력서

**시각화**:
- Chart.js 또는 Recharts 활용
- 주간/월간 통계
- 비교 분석 (이전 기간 대비)

#### 11.7.2 채용담당자 인사이트
- 평균 응답 시간
- 메시지 전송 패턴
- 지원자 반응률

---

### 11.8 소셜 및 공유 기능 (우선순위: 낮음)

#### 11.8.1 소셜 로그인
- Google, LinkedIn, GitHub 연동
- OAuth 2.0 구현
- 기존 계정과 연동 가능

**구현 예시**:
```java
@GetMapping("/oauth2/authorization/google")
public String googleLogin() {
    // Spring Security OAuth2 처리
}
```

#### 11.8.2 이력서 링크 커스터마이징
**현재**: `/chat/{uuid}`
**개선**: `/chat/@{username}` 또는 `/chat/{custom-slug}`

- 사용자가 원하는 URL 설정
- 중복 검사
- SEO 친화적

#### 11.8.3 QR 코드 생성
- 이력서 채팅 링크 QR 코드 생성
- 명함에 인쇄 가능
- 오프라인 네트워킹에 활용

---

### 11.9 보안 강화 (우선순위: 높음)

#### 11.9.1 2단계 인증 (2FA)
- TOTP 기반 (Google Authenticator)
- SMS 인증 코드
- 이메일 인증 코드
- 로그인 시 선택적 활성화

#### 11.9.2 세션 관리 개선
- sessionToken 만료 시간 설정
- 일정 기간 미사용 시 세션 비활성화
- IP 기반 이상 접근 감지

#### 11.9.3 채용담당자 인증
**현재 문제**:
- sessionToken만 알면 누구나 채팅 가능

**개선 방안**:
- 이메일 인증 도입
- 첫 메시지 전송 시 이메일로 인증 링크 발송
- 인증 완료 후 채팅 활성화
- 스팸 방지

#### 11.9.4 스팸 방지
- Rate Limiting (IP당 분당 요청 제한)
- reCAPTCHA 통합
- 악성 사용자 차단 목록

---

### 11.10 고급 채팅 기능 (우선순위: 낮음)

#### 11.10.1 메시지 편집/삭제
- 전송 후 5분 이내 수정 가능
- 삭제된 메시지는 "삭제된 메시지입니다" 표시
- 편집 이력 보기

#### 11.10.2 반응(Reaction) 추가
- 메시지에 이모지 반응
- 좋아요, 하트, 엄지 등
- 빠른 피드백 제공

#### 11.10.3 스레드(Thread) 기능
- 특정 메시지에 대한 답글 스레드
- 복잡한 대화 구조화
- 사이드바에 스레드 표시

#### 11.10.4 음성 메시지
- 음성 녹음 및 전송
- Waveform 시각화
- 재생 컨트롤

---

### 11.11 AI 기반 기능 (우선순위: 낮음)

#### 11.11.1 AI 이력서 분석
- OpenAI API 연동
- 이력서 강점/약점 분석
- 개선 제안
- 키워드 추출

**API 예시**:
```
POST /api/applicant/resume/{resumeSlug}/analyze
응답: {
  "strengths": ["10년 경력", "다양한 기술 스택"],
  "improvements": ["프로젝트 성과 수치화 필요"],
  "keywords": ["Java", "Spring Boot", "AWS"]
}
```

#### 11.11.2 AI 채팅 어시스턴트
- 지원자를 위한 답변 제안
- 채용담당자를 위한 질문 템플릿
- 감정 분석 (긍정/부정 톤 감지)

#### 11.11.3 스마트 매칭
- 이력서 내용 기반 적합한 포지션 추천
- 채용담당자에게 관련 지원자 추천
- 머신러닝 모델 학습

---

### 11.12 통합 기능 (우선순위: 낮음)

#### 11.12.1 캘린더 연동
- Google Calendar, Outlook 연동
- 채팅 중 면접 일정 제안
- 자동으로 캘린더 이벤트 생성
- 리마인더 알림

#### 11.12.2 화상 면접 연동
- Zoom, Google Meet 링크 생성
- 채팅에서 바로 화상 회의 시작
- 회의 예약 및 관리

#### 11.12.3 ATS 연동
- Greenhouse, Lever 등 ATS와 연동
- 지원자 정보 자동 동기화
- 채용 파이프라인 연결

---

### 11.13 모바일 앱 (우선순위: 중간)

#### 11.13.1 React Native 앱
- iOS/Android 네이티브 앱
- 푸시 알림
- 카메라로 이력서 촬영 및 업로드
- 오프라인 모드 (메시지 캐싱)

#### 11.13.2 모바일 전용 기능
- 위치 기반 서비스 (선택적)
- 생체 인증 (Face ID, 지문)
- 앱 내 채팅 알림

---

### 11.14 다국어 및 접근성 (우선순위: 낮음)

#### 11.14.1 다국어 지원 (i18n)
- 영어, 한국어, 일본어, 중국어
- 사용자 언어 설정
- 자동 번역 (선택적, Google Translate API)

#### 11.14.2 접근성 개선
- 스크린 리더 지원
- 키보드 내비게이션
- 고대비 모드
- WCAG 2.1 AA 준수

---

### 11.15 운영 및 관리 (우선순위: 중간)

#### 11.15.1 관리자 대시보드
- 전체 사용자 통계
- 시스템 모니터링
- 신고된 콘텐츠 관리
- 사용자 계정 관리

#### 11.15.2 로깅 및 모니터링
- ELK Stack (Elasticsearch, Logstash, Kibana)
- 에러 추적 (Sentry)
- 성능 모니터링 (Prometheus + Grafana)
- 사용자 행동 분석 (Google Analytics)

#### 11.15.3 백업 및 복구
- 자동 데이터베이스 백업 (일일)
- Point-in-time 복구
- 재해 복구 계획 (DR)

---

## 12. 구현 우선순위 로드맵

### Phase 1: 기본 기능 강화 (1-2개월) ✅ 완료
- [x] 지원자 회원가입/로그인
- [x] 이력서 업로드/관리
- [x] 채팅 세션 생성
- [x] 메시지 전송/조회

### Phase 2: 실시간 & 알림 (2-3개월)
- [ ] WebSocket 실시간 채팅
- [ ] 이메일 알림 시스템
- [ ] 브라우저 푸시 알림
- [ ] 입력 중 표시

### Phase 3: 파일 & 검색 (2개월)
- [ ] 채팅 내 파일 첨부
- [ ] 이력서 PDF 미리보기
- [ ] 채팅 내 검색
- [ ] 채팅 히스토리 내보내기

### Phase 4: 보안 & UX (1-2개월)
- [ ] 2단계 인증
- [ ] 채용담당자 이메일 인증
- [ ] Rate Limiting
- [ ] 다크 모드
- [ ] 빠른 응답 기능

### Phase 5: 분석 & 통계 (1개월)
- [ ] 지원자 대시보드
- [ ] 이력서 조회수 그래프
- [ ] 응답률 분석

### Phase 6: 고급 기능 (3-4개월)
- [ ] AI 이력서 분석
- [ ] 캘린더 연동
- [ ] 화상 면접 연동
- [ ] 이력서 버전 관리

### Phase 7: 모바일 & 확장 (3-6개월)
- [ ] React Native 모바일 앱
- [ ] 다국어 지원
- [ ] ATS 연동

---

## 13. API 응답 형식

### 13.1 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2024-02-14T10:30:00"
}
```

### 13.2 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "A001",
    "message": "지원자를 찾을 수 없습니다"
  },
  "timestamp": "2024-02-14T10:30:00"
}
```

---

## 14. 환경 설정

### 14.1 애플리케이션 설정 (application.yml)
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

jwt:
  access-token-expiration: 3600000    # 1시간
  refresh-token-expiration: 604800000 # 7일

file:
  upload-dir: ./uploads/resumes
  max-file-size: 10485760  # 10MB
  allowed-extensions: pdf

app:
  frontend-url: http://localhost:3000
```

### 14.2 데이터베이스
- **개발 환경**: MySQL
- **테스트 환경**: H2 (In-Memory)

---

## 15. 테스트 전략

### 15.1 단위 테스트
- Service Layer 로직 테스트
- Repository Query 테스트
- Entity 생성 팩토리 메서드 테스트

### 15.2 통합 테스트
- Controller API 엔드포인트 테스트
- 전체 플로우 통합 테스트 (회원가입 → 이력서 업로드 → 채팅)

### 15.3 테스트 범위
- ApplicantServiceTest: 회원가입, 로그인, 프로필 조회
- ResumeServiceTest: 이력서 업로드, 조회, 삭제
- ChatServiceTest: 세션 생성, 메시지 전송/조회
- Integration Tests: 전체 API 시나리오

---

## 16. 문서 및 API 명세

### 16.1 Swagger UI
- **URL**: http://localhost:8080/swagger-ui/index.html
- **기능**:
  - 전체 API 엔드포인트 문서화
  - Try it out 기능으로 직접 테스트 가능
  - Request/Response 예시 포함

### 16.2 API 버전
- **현재 버전**: v1
- **Base URL**: /api

---

## 17. 주요 제약사항 및 고려사항

### 17.1 현재 제약사항
- 이력서 파일 형식: PDF만 지원
- 파일 저장소: 로컬 파일 시스템 (클라우드 스토리지 미지원)
- 채용담당자 인증: 없음 (sessionToken만으로 관리)
- 실시간 채팅: 미지원 (폴링 방식)

### 17.2 고려사항
- **확장성**: 향후 WebSocket, 클라우드 스토리지, 채용담당자 인증 추가 고려
- **보안**: sessionToken 유출 시 다른 사람이 채팅 접근 가능 (개선 필요)
- **성능**: 대용량 파일 업로드 시 서버 부하 고려
- **데이터 정합성**: Cascade 삭제로 인한 데이터 복구 불가 (소프트 삭제 고려)

---

## 18. 성공 지표 (KPI)

### 18.1 사용자 지표
- 월간 활성 사용자(MAU)
- 이력서 업로드 수
- 채팅 세션 생성 수
- 메시지 전송 수

### 18.2 참여 지표
- 채용담당자 응답률 (첫 메시지 수신 비율)
- 평균 응답 시간
- 채팅 세션당 평균 메시지 수

### 18.3 기술 지표
- API 응답 시간 (< 200ms 목표)
- 에러율 (< 1% 목표)
- 시스템 가동률 (> 99.5% 목표)

---

## 19. 용어 사전

| 용어 | 설명 |
|------|------|
| resumeSlug | 이력서의 고유 식별자 (UUID), 채팅 링크 생성에 사용 |
| sessionToken | 채팅 세션의 고유 식별자 (UUID String) |
| Applicant | 지원자 (이력서를 업로드하는 구직자) |
| Recruiter | 채용담당자 (이력서 링크를 통해 지원자와 소통) |
| ChatSession | 특정 이력서와 채용담당자 간의 대화 세션 |
| SenderType | 메시지 발신자 타입 (APPLICANT or RECRUITER) |
| readStatus | 메시지 읽음 여부 (true: 읽음, false: 안 읽음) |
| WebSocket | 실시간 양방향 통신 프로토콜 |
| STOMP | Simple Text Oriented Messaging Protocol |

---

**문서 버전**: 1.1
**최종 업데이트**: 2024-02-14
**작성자**: Resume Chat Team
