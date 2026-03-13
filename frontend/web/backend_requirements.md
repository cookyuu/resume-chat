# 백엔드 API 요구사항

> 프론트엔드에서 구현된 기능 중 백엔드 API가 필요한 항목들입니다.

---

## 🟡 알림 설정 API 추가

### 개요
- 프론트엔드에서 알림 설정 페이지(`/settings/notifications`)가 구현됨
- 사용자가 이메일/푸시 알림 설정을 저장하고 조회할 수 있는 API 필요

### 필요한 수정

#### 1. NotificationSettings 엔티티/테이블 생성

```sql
CREATE TABLE notification_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email_notification BOOLEAN DEFAULT TRUE,
    push_notification BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### 2. API 엔드포인트

**GET /applicant/settings/notifications** - 알림 설정 조회
```json
// Response
{
  "code": "SUCCESS",
  "message": "알림 설정 조회 성공",
  "data": {
    "emailNotification": true,
    "pushNotification": false
  }
}
```

**PUT /applicant/settings/notifications** - 알림 설정 업데이트
```json
// Request
{
  "emailNotification": true,
  "pushNotification": false
}

// Response
{
  "code": "SUCCESS",
  "message": "알림 설정 업데이트 성공",
  "data": {
    "emailNotification": true,
    "pushNotification": false
  }
}
```

#### 3. 구현 예시

```java
@RestController
@RequestMapping("/applicant/settings")
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingsDto>> getSettings(
        @AuthenticationPrincipal User user
    ) {
        NotificationSettingsDto settings = settingsService.getSettings(user.getId());
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingsDto>> updateSettings(
        @AuthenticationPrincipal User user,
        @RequestBody @Valid UpdateNotificationSettingsRequest request
    ) {
        NotificationSettingsDto settings = settingsService.updateSettings(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }
}
```

#### 4. Service 로직

```java
@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository settingsRepository;

    public NotificationSettingsDto getSettings(Long userId) {
        // 설정이 없으면 기본값으로 생성
        NotificationSettings settings = settingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));

        return NotificationSettingsDto.from(settings);
    }

    public NotificationSettingsDto updateSettings(Long userId, UpdateNotificationSettingsRequest request) {
        NotificationSettings settings = settingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));

        settings.setEmailNotification(request.getEmailNotification());
        settings.setPushNotification(request.getPushNotification());

        NotificationSettings saved = settingsRepository.save(settings);
        return NotificationSettingsDto.from(saved);
    }

    private NotificationSettings createDefaultSettings(Long userId) {
        NotificationSettings settings = NotificationSettings.builder()
            .userId(userId)
            .emailNotification(true)
            .pushNotification(false)
            .build();
        return settingsRepository.save(settings);
    }
}
```

### 우선순위
🟡 **중간** - 기능 완성을 위해 필요하나 긴급하지 않음

### 체크리스트
- [ ] NotificationSettings 엔티티 생성
- [ ] notification_settings 테이블 마이그레이션
- [ ] NotificationSettingsRepository 생성
- [ ] GET /applicant/settings/notifications 구현
- [ ] PUT /applicant/settings/notifications 구현
- [ ] 기본값 생성 로직 구현 (첫 조회 시)
- [ ] API 문서 업데이트

---

## 🔴 ChatSession에 지원자 정보 추가

### 개요
- 채용담당자 채팅 화면에서 지원자 정보(이름, 이메일) 표시 필요
- 현재 ChatSession 응답에 이력서 제목만 포함되어 있음

### 필요한 수정

ChatSessionDto에 다음 필드 추가:

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

### 영향받는 API
- `GET /chat/session/{sessionToken}/messages` - 채용담당자용 메시지 조회
- `GET /applicant/chat/{sessionToken}/messages` - 지원자용 메시지 조회

### 우선순위
🔴 **높음** - 사용자 경험에 직접적인 영향

### 체크리스트
- [ ] ChatSessionDto에 applicantEmail, applicantName 필드 추가
- [ ] Service 레이어에서 지원자 정보 조회 로직 추가
- [ ] GET /chat/session/{sessionToken}/messages API 응답에 지원자 정보 포함
- [ ] API 문서 업데이트

---

**작성일**: 2026-03-13
**관련 문서**: [fix.md](./fix.md), [tasks.md](./tasks.md)
