# Email Notification Integration Guide

## 개요

Phase 2.4 이메일 알림 시스템이 구현되었습니다. ChatService에 통합하여 실제 알림을 발송할 수 있습니다.

## 알림 발송 정책

- **읽지 않은 메시지 최초 발생** → 5분 타이머 시작
- **5분 이내 읽으면** → 타이머 취소, 알림 발송 안 함
- **5분 경과 후** → 1회만 알림 발송 (여러 메시지 포함)

## ChatService 통합 방법

### 1. EmailService 주입

```java
@Service
@RequiredArgsConstructor
public class ChatService {
    // 기존 의존성들...
    private final EmailService emailService;  // 추가
}
```

### 2. 메시지 전송 시 알림 예약

**sendMessageByApplicant()** (지원자 → 채용담당자):
```java
public ChatDto.ApplicantSendMessageResponse sendMessageByApplicant(
        UUID applicantUuid, String sessionToken, ChatDto.ApplicantSendMessageRequest request) {

    // 기존 메시지 저장 로직...
    chatMessageRepository.save(message);
    session.incrementMessageCount();
    broadcastMessage(sessionToken, message);

    // ✅ 이메일 알림 예약 추가
    emailService.scheduleNewMessageNotification(
            sessionToken,
            session.getRecruiterEmail(),          // 수신자: 채용담당자 이메일
            session.getRecruiterName(),           // 수신자 이름
            applicant.getName(),                  // 발신자: 지원자 이름
            SenderType.APPLICANT,                 // 발신자 타입
            request.getMessage()                  // 메시지 미리보기
    );

    return ChatDto.ApplicantSendMessageResponse.from(session, message);
}
```

**sendRecruiterMessage()** (채용담당자 → 지원자):
```java
public ChatDto.RecruiterSendMessageResponse sendRecruiterMessage(
        String sessionToken, ChatDto.RecruiterSendMessageRequest request) {

    // 기존 메시지 저장 로직...
    chatMessageRepository.save(message);
    session.incrementMessageCount();
    broadcastMessage(sessionToken, message);

    // ✅ 이메일 알림 예약 추가
    emailService.scheduleNewMessageNotification(
            sessionToken,
            session.getResume().getApplicant().getEmail(),  // 수신자: 지원자 이메일
            session.getResume().getApplicant().getName(),   // 수신자 이름
            session.getRecruiterName(),                     // 발신자: 채용담당자 이름
            SenderType.RECRUITER,                           // 발신자 타입
            request.getMessage()                            // 메시지 미리보기
    );

    return ChatDto.RecruiterSendMessageResponse.from(session, message);
}
```

### 3. 메시지 읽음 처리 시 알림 취소

**getApplicantSessionMessages()** (지원자가 메시지 조회 시):
```java
@Transactional
public ChatDto.ChatDetailResponse getApplicantSessionMessages(UUID applicantUuid, String sessionToken) {
    ChatSession session = getApplicantSessionWithValidation(applicantUuid, sessionToken);

    // 읽지 않은 메시지 읽음 처리
    List<ChatMessage> unreadMessages = chatMessageRepository
            .findBySessionAndReadStatusFalseAndSenderType(session, SenderType.RECRUITER);

    if (!unreadMessages.isEmpty()) {
        unreadMessages.forEach(ChatMessage::markAsRead);

        // ✅ 알림 취소 추가
        emailService.cancelNotification(sessionToken);
    }

    // 기존 메시지 조회 로직...
    return ChatDto.ChatDetailResponse.from(session, messages, 0);
}
```

### 4. 신규 세션 생성 시 알림 발송

**sendMessage()** (채용담당자 첫 메시지):
```java
@Transactional
public ChatDto.SendMessageResponse sendMessage(UUID resumeSlug, ChatDto.SendMessageRequest request) {
    Resume resume = getResumeBySlug(resumeSlug);

    // 세션 생성 또는 조회
    ChatSession session = chatSessionRepository
            .findByResumeAndRecruiterEmail(resume, request.getRecruiterEmail())
            .orElseGet(() -> {
                ChatSession newSession = ChatSession.createNewSession(
                        resume,
                        request.getRecruiterName(),
                        request.getRecruiterEmail(),
                        request.getRecruiterCompany()
                );
                return chatSessionRepository.save(newSession);
            });

    // 첫 메시지인지 확인
    boolean isFirstMessage = session.getTotalMessages() == 0;

    // 메시지 저장
    ChatMessage message = ChatMessage.createMessage(session, SenderType.RECRUITER, request.getMessage());
    chatMessageRepository.save(message);
    session.incrementMessageCount();
    broadcastMessage(session.getSessionToken(), message);

    // ✅ 첫 메시지면 신규 세션 알림 발송
    if (isFirstMessage) {
        emailService.sendNewSessionNotification(session, request.getMessage());
    }

    return ChatDto.SendMessageResponse.from(session, message);
}
```

## 환경변수 설정

### 로컬 개발 환경

1. `.env` 파일이 이미 생성되어 있습니다:
```env
MAIL_USERNAME=biz.cookyuu@gmail.com
MAIL_PASSWORD=hcmp srxn jssx wfpj
```

2. IntelliJ IDEA에서 환경변수 로드:
   - Run → Edit Configurations
   - Environment variables: `MAIL_USERNAME=biz.cookyuu@gmail.com;MAIL_PASSWORD=hcmp srxn jssx wfpj`

### 프로덕션 환경

시스템 환경변수 또는 Docker secrets로 설정:
```bash
export MAIL_USERNAME=biz.cookyuu@gmail.com
export MAIL_PASSWORD=hcmp-srxn-jssx-wfpj
```

## 테스트 방법

### 1. 이메일 발송 테스트

```java
@SpringBootTest
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    void testSendNewMessageEmail() {
        emailService.sendNewMessageEmail(
                "test@example.com",
                "테스터",
                "채용담당자",
                SenderType.RECRUITER,
                "test-session-token",
                1,
                "안녕하세요, 테스트 메시지입니다."
        );

        // 이메일 수신 확인
    }
}
```

### 2. 지연 알림 테스트

```java
@Test
void testDelayedNotification() throws InterruptedException {
    // 알림 예약
    emailService.scheduleNewMessageNotification(
            "test-session",
            "test@example.com",
            "테스터",
            "발신자",
            SenderType.RECRUITER,
            "메시지 내용"
    );

    // 5분 대기 (테스트 시에는 application.yml에서 delay를 1분으로 줄일 수 있음)
    Thread.sleep(6 * 60 * 1000);

    // 이메일 수신 확인
}
```

### 3. 알림 취소 테스트

```java
@Test
void testCancelNotification() throws InterruptedException {
    // 알림 예약
    emailService.scheduleNewMessageNotification(
            "test-session",
            "test@example.com",
            "테스터",
            "발신자",
            SenderType.RECRUITER,
            "메시지 내용"
    );

    // 1분 후 취소
    Thread.sleep(60 * 1000);
    emailService.cancelNotification("test-session");

    // 5분 대기
    Thread.sleep(5 * 60 * 1000);

    // 이메일이 발송되지 않았는지 확인
}
```

## 설정 커스터마이징

### 알림 지연 시간 변경

`application.yml`:
```yaml
app:
  mail:
    notification-delay-minutes: 5  # 5분 → 원하는 시간으로 변경
```

### 발신자 정보 변경

`application.yml`:
```yaml
app:
  mail:
    from: ${MAIL_USERNAME:biz.cookyuu@gmail.com}
    from-name: Resume Chat  # 원하는 이름으로 변경
```

### Thread Pool 크기 조정

`AsyncConfig.java`:
```java
executor.setCorePoolSize(2);    // 기본 스레드 수
executor.setMaxPoolSize(5);     // 최대 스레드 수
executor.setQueueCapacity(100); // 대기 큐 크기
```

## 주의사항

1. **Gmail 앱 비밀번호**: 일반 비밀번호가 아닌 앱 비밀번호를 사용해야 합니다.
2. **환경변수 보안**: `.env` 파일은 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.
3. **SMTP 제한**: Gmail은 일일 발송 제한이 있습니다 (무료: 500통/일).
4. **프로덕션 환경**: 대용량 발송이 필요하면 SendGrid, AWS SES 등 전문 서비스 사용 권장.

## 트러블슈팅

### 이메일이 발송되지 않는 경우

1. **SMTP 인증 실패**:
   - Gmail 앱 비밀번호가 올바른지 확인
   - Google 계정에서 2단계 인증이 활성화되어 있는지 확인

2. **방화벽 차단**:
   - 포트 587(TLS) 또는 465(SSL)이 열려있는지 확인

3. **로그 확인**:
   ```
   tail -f logs/application.log | grep Email
   ```

### 이메일이 스팸함으로 가는 경우

1. SPF, DKIM, DMARC 레코드 설정 (프로덕션 환경)
2. 발신자 이름 및 내용 최적화
3. 전문 이메일 서비스 사용 고려

## 다음 단계

Phase 2.4가 완료되었습니다! 다음 단계는:

- **Phase 2.5**: 브라우저 푸시 알림 (Web Push API)
- **Phase 2.6**: 알림 설정 페이지 (사용자가 알림 on/off 선택)
