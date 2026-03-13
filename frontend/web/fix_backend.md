# 백엔드 수정사항 체크리스트

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

### 체크리스트
- [ ] ChatSessionDto에 applicantEmail, applicantName 필드 추가
- [ ] Service 레이어에서 지원자 정보 조회 로직 추가
- [ ] GET /chat/session/{sessionToken}/messages API 응답에 지원자 정보 포함
- [ ] GET /applicant/chat/{sessionToken}/messages API 응답 확인 (기존 동작 유지)
- [ ] API 문서 업데이트

---

**작성일**: 2026-03-12
**최종 수정일**: 2026-03-12
**관련 문서**: [fix.md](./fix.md) (프론트엔드 수정사항)
