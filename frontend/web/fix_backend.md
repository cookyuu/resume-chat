# 백엔드 수정 체크리스트

## 🔴 긴급 - 이력서 조회 API에 fileUrl 필드 추가

### 문제 상황
- 현재 응답에 `fileUrl` 필드가 없음
- 프론트엔드에서 이력서 미리보기 버튼 클릭 시 "파일 URL을 찾을 수 없습니다" 에러 발생

### 현재 응답 구조
```json
{
    "success": true,
    "data": [
        {
            "resumeSlug": "45f83f1b-689d-4bf9-87a2-16724e98f01a",
            "title": "BE",
            "description": "BE",
            "originalFileName": "백엔드개발자_임해규_이력서.pdf",
            "chatLink": "http://localhost:31000/chat/45f83f1b-689d-4bf9-87a2-16724e98f01a",
            "viewCnt": 0,
            "createdAt": "2026-03-10T23:01:50.765044"
        }
    ],
    "timestamp": "2026-03-17T13:12:58.748162"
}
```

### 필요한 응답 구조
```json
{
    "success": true,
    "data": [
        {
            "resumeSlug": "45f83f1b-689d-4bf9-87a2-16724e98f01a",
            "title": "BE",
            "description": "BE",
            "originalFileName": "백엔드개발자_임해규_이력서.pdf",
            "fileUrl": "/uploads/resumes/45f83f1b-689d-4bf9-87a2-16724e98f01a.pdf",  // ✅ 추가 필요
            "chatLink": "http://localhost:31000/chat/45f83f1b-689d-4bf9-87a2-16724e98f01a",
            "viewCnt": 0,
            "createdAt": "2026-03-10T23:01:50.765044"
        }
    ],
    "timestamp": "2026-03-17T13:12:58.748162"
}
```

---

## ✅ 수정 체크리스트

### 1️⃣ Resume Entity 확인
- [ ] `Resume` Entity에 `fileUrl` 또는 `filePath` 필드가 있는지 확인
- [ ] 없다면 데이터베이스 스키마 확인

```java
@Entity
public class Resume {
    @Id
    private String resumeSlug;
    private String title;
    private String description;
    private String originalFileName;
    private String fileUrl;  // ✅ 이 필드 확인
    private Integer viewCnt;
    private LocalDateTime createdAt;

    // getters, setters...
}
```

### 2️⃣ Resume DTO 수정
- [ ] `ResumeDTO` 또는 `ResumeListResponse`에 `fileUrl` 필드 추가

```java
public class ResumeDTO {
    private String resumeSlug;
    private String title;
    private String description;
    private String originalFileName;
    private String fileUrl;  // ✅ 추가
    private String chatLink;
    private Integer viewCnt;
    private String createdAt;

    public static ResumeDTO from(Resume resume) {
        ResumeDTO dto = new ResumeDTO();
        dto.setResumeSlug(resume.getResumeSlug());
        dto.setTitle(resume.getTitle());
        dto.setDescription(resume.getDescription());
        dto.setOriginalFileName(resume.getOriginalFileName());
        dto.setFileUrl(resume.getFileUrl());  // ✅ 추가
        dto.setChatLink(resume.getChatLink());
        dto.setViewCnt(resume.getViewCnt());
        dto.setCreatedAt(resume.getCreatedAt().toString());
        return dto;
    }
}
```

### 3️⃣ 이력서 업로드 시 fileUrl 저장
- [ ] 파일 업로드 시 저장 경로를 DB에 저장하는지 확인
- [ ] 저장되지 않는다면 로직 추가

```java
@PostMapping("/api/applicant/resumes")
public ResponseEntity<?> uploadResume(
    @RequestParam String title,
    @RequestParam String description,
    @RequestParam MultipartFile file
) {
    // 파일 저장
    String savedFileName = fileStorageService.save(file);

    // Resume 엔티티 생성
    Resume resume = new Resume();
    resume.setResumeSlug(UUID.randomUUID().toString());
    resume.setTitle(title);
    resume.setDescription(description);
    resume.setOriginalFileName(file.getOriginalFilename());

    // ✅ fileUrl 저장
    resume.setFileUrl("/uploads/resumes/" + savedFileName);

    resume.setViewCnt(0);
    resume.setCreatedAt(LocalDateTime.now());

    resumeRepository.save(resume);

    return ResponseEntity.ok(ResumeDTO.from(resume));
}
```

### 4️⃣ 이력서 목록 조회 API 확인
- [ ] `GET /api/applicant/resumes` 응답에 `fileUrl` 포함되는지 확인
- [ ] DTO의 `from()` 메서드가 올바르게 호출되는지 확인

---

## 📋 fileUrl 형식 가이드

### 상대 경로 (권장)
```
/uploads/resumes/{resumeSlug}.pdf
```
- 프론트엔드에서 자동으로 절대 경로로 변환
- 서버 도메인 변경에 유연하게 대응

### 절대 경로
```
http://localhost:8080/uploads/resumes/{resumeSlug}.pdf
```
- 바로 사용 가능
- 하지만 도메인 하드코딩 필요

**권장**: 상대 경로 사용

---

## 🧪 테스트 체크리스트

### 1. 이력서 업로드 테스트
- [ ] 새 이력서 업로드
- [ ] DB에 `fileUrl` 저장 확인
```sql
SELECT resume_slug, title, file_url FROM resume ORDER BY created_at DESC LIMIT 1;
```

### 2. 이력서 목록 조회 테스트
- [ ] API 호출
```bash
curl -X GET "http://localhost:8080/api/applicant/resumes" \
  -H "Authorization: Bearer {token}"
```
- [ ] 응답에 `fileUrl` 포함 확인
```json
{
  "data": [
    {
      "resumeSlug": "...",
      "fileUrl": "/uploads/resumes/xxx.pdf"  // ✅ 확인
    }
  ]
}
```

### 3. 파일 접근 테스트
- [ ] 브라우저에서 파일 URL 직접 접근
```
http://localhost:8080/uploads/resumes/xxx.pdf
```
- [ ] PDF 파일이 정상적으로 열리는지 확인
- [ ] CORS 설정 확인 (프론트엔드 도메인 허용)

---

## 🔥 우선순위
**HIGH** - 이력서 미리보기 기능이 전혀 작동하지 않습니다.

---

## 🔴 긴급 - 채팅 메시지 조회 API에 attachment 필드 추가

### 문제 상황
- 프론트엔드에서 파일 업로드 및 메시지 전송은 정상 작동
- 하지만 메시지 조회 API 응답에 `attachment` 필드가 누락되어 첨부파일이 UI에 표시되지 않음

### 현재 백엔드 응답 (추정)
```json
{
  "success": true,
  "data": {
    "session": { ... },
    "messages": [
      {
        "messageId": "xxx",
        "message": "2025년 2분기-신용보증기금.pdf",
        "senderType": "APPLICANT",
        "readStatus": true,
        "sentAt": "2026-03-17T10:48:00"
        // ❌ attachment 필드 누락
      }
    ]
  }
}
```

### 필요한 백엔드 응답 구조

#### 첨부파일이 **있는** 메시지:
```json
{
  "success": true,
  "data": {
    "session": { ... },
    "messages": [
      {
        "messageId": "xxx",
        "message": "",
        "senderType": "APPLICANT",
        "readStatus": true,
        "sentAt": "2026-03-17T10:48:00",
        "attachment": {  // ✅ 이 필드 추가 필요
          "attachmentId": "att-123",
          "fileName": "2025년 2분기-신용보증기금.pdf",
          "fileSize": 1024000,
          "fileType": "application/pdf",
          "fileUrl": "/uploads/files/att-123.pdf",
          "uploadedAt": "2026-03-17T10:48:00"
        }
      }
    ]
  }
}
```

#### 첨부파일이 **없는** 메시지:
```json
{
  "messageId": "msg-456",
  "message": "안녕하세요",
  "senderType": "RECRUITER",
  "readStatus": true,
  "sentAt": "2026-03-17T10:49:00",
  "attachment": null
}
```

---

## ✅ 수정 체크리스트

### 1️⃣ 데이터베이스 스키마 확인
- [ ] `message` 테이블에 `attachment_id` 컬럼이 존재하는지 확인
- [ ] `attachment_id`가 `attachment` 테이블의 외래키로 설정되어 있는지 확인

```sql
-- 필요시 실행
ALTER TABLE message ADD COLUMN attachment_id VARCHAR(255);
ALTER TABLE message ADD CONSTRAINT fk_message_attachment
    FOREIGN KEY (attachment_id) REFERENCES attachment(attachment_id);
```

### 2️⃣ Entity 클래스 수정
- [ ] `Message` Entity에 `Attachment` 연관관계 추가

```java
@Entity
public class Message {
    @Id
    private String messageId;
    private String message;
    private SenderType senderType;
    private boolean readStatus;
    private LocalDateTime sentAt;

    // ✅ 추가 필요
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Attachment attachment;

    // getters, setters...
}
```

### 3️⃣ DTO 클래스 수정

#### MessageDTO
- [ ] `MessageDTO`에 `AttachmentDTO attachment` 필드 추가
- [ ] `from()` 메서드에서 attachment 매핑 로직 추가

```java
public class MessageDTO {
    private String messageId;
    private String message;
    private SenderType senderType;
    private boolean readStatus;
    private String sentAt;
    private AttachmentDTO attachment;  // ✅ 추가

    public static MessageDTO from(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setMessage(message.getMessage());
        dto.setSenderType(message.getSenderType());
        dto.setReadStatus(message.isReadStatus());
        dto.setSentAt(message.getSentAt().toString());

        // ✅ 추가: 첨부파일이 있으면 포함, 없으면 null
        if (message.getAttachment() != null) {
            dto.setAttachment(AttachmentDTO.from(message.getAttachment()));
        } else {
            dto.setAttachment(null);
        }

        return dto;
    }
}
```

#### AttachmentDTO
- [ ] `AttachmentDTO` 클래스 생성 (또는 확인)

```java
public class AttachmentDTO {
    private String attachmentId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileUrl;
    private String uploadedAt;

    public static AttachmentDTO from(Attachment attachment) {
        AttachmentDTO dto = new AttachmentDTO();
        dto.setAttachmentId(attachment.getAttachmentId());
        dto.setFileName(attachment.getFileName());
        dto.setFileSize(attachment.getFileSize());
        dto.setFileType(attachment.getFileType());
        dto.setFileUrl(attachment.getFileUrl());
        dto.setUploadedAt(attachment.getUploadedAt().toString());
        return dto;
    }
}
```

### 4️⃣ SendMessageRequest DTO 확인
- [ ] `SendMessageRequest`에 `attachmentId` 필드가 있는지 확인

```java
public class SendMessageRequest {
    private String message;
    private String attachmentId;  // ✅ 이 필드 필요

    // getters, setters...
}
```

### 5️⃣ 메시지 전송 API 수정

#### 지원자용 메시지 전송
- [ ] `POST /api/applicant/chat/{sessionToken}/send` 수정
- [ ] `attachmentId`가 있으면 첨부파일 연결하는 로직 추가

```java
@PostMapping("/api/applicant/chat/{sessionToken}/send")
public ResponseEntity<?> sendMessage(
    @PathVariable String sessionToken,
    @RequestBody SendMessageRequest request
) {
    Message message = new Message();
    message.setMessage(request.getMessage());
    message.setSenderType(SenderType.APPLICANT);

    // ✅ attachmentId가 있으면 첨부파일 연결
    if (request.getAttachmentId() != null) {
        Attachment attachment = attachmentRepository
            .findById(request.getAttachmentId())
            .orElseThrow(() -> new NotFoundException("Attachment not found"));
        message.setAttachment(attachment);
    }

    messageRepository.save(message);

    // WebSocket 브로드캐스트 (첨부파일 정보 포함)
    MessageDTO dto = MessageDTO.from(message);
    messagingTemplate.convertAndSend("/topic/chat/" + sessionToken, dto);

    return ResponseEntity.ok(dto);
}
```

#### 채용담당자용 메시지 전송
- [ ] `POST /api/chat/session/{sessionToken}/send` 수정
- [ ] 동일한 로직 적용

### 6️⃣ 메시지 조회 API 확인

#### 지원자용 메시지 조회
- [ ] `GET /api/applicant/chat/{sessionToken}/messages`
- [ ] 응답에 `attachment` 필드가 포함되는지 확인

#### 채용담당자용 메시지 조회
- [ ] `GET /api/chat/session/{sessionToken}/messages`
- [ ] 응답에 `attachment` 필드가 포함되는지 확인

> **참고**: MessageDTO의 `from()` 메서드가 올바르게 구현되어 있다면 자동으로 포함됩니다.

### 7️⃣ WebSocket 브로드캐스트 확인
- [ ] 새 메시지 전송 시 WebSocket으로 브로드캐스트할 때 `attachment` 포함
- [ ] Topic: `/topic/chat/{sessionToken}`
- [ ] `MessageDTO.from(message)` 사용하여 첨부파일 정보 자동 포함

---

## 🧪 테스트 체크리스트

### 1. 파일 업로드 테스트
- [ ] 파일 업로드 API 호출 성공
- [ ] `attachmentId` 반환 확인

### 2. 메시지 전송 테스트
- [ ] `attachmentId`를 포함한 메시지 전송 성공
- [ ] 응답에 `attachment` 객체 포함 확인

### 3. 메시지 조회 테스트
- [ ] 메시지 목록 조회 시 `attachment` 필드 포함 확인
- [ ] 첨부파일이 없는 메시지는 `attachment: null` 확인

### 4. WebSocket 테스트
- [ ] 새 메시지 브로드캐스트 시 `attachment` 포함 확인
- [ ] 두 개의 브라우저 창에서 실시간 업데이트 확인

---

## 🔍 확인 방법

### API 테스트
```bash
# 메시지 조회
curl -X GET "http://localhost:8080/api/applicant/chat/{sessionToken}/messages" \
  -H "Authorization: Bearer {token}" \
  | jq '.data.messages[0].attachment'

# 기대 결과: attachment 객체 또는 null
```

### 데이터베이스 확인
```sql
-- 메시지와 첨부파일 연결 확인
SELECT m.message_id, m.message, a.file_name, a.file_url
FROM message m
LEFT JOIN attachment a ON m.attachment_id = a.attachment_id
WHERE m.message_id = 'xxx';
```

---

## 🔥 우선순위
**HIGH** - 이 수정이 없으면 첨부파일 기능이 전혀 작동하지 않습니다.

---

## ✅ 프론트엔드 준비 상태
프론트엔드는 이미 완전히 구현되어 있습니다:
- ✅ 파일 업로드 API 호출
- ✅ 메시지 전송 시 `attachmentId` 포함
- ✅ `msg.attachment` 체크 후 AttachmentDisplay 컴포넌트 렌더링
- ✅ 다운로드 링크 구현
- ✅ 파일 타입별 아이콘 및 스타일링
- ✅ 이미지 인라인 미리보기

**백엔드에서 위 체크리스트를 완료하면 바로 작동합니다!**
