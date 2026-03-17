# 백엔드 수정 체크리스트 - 첨부파일 표시 기능

## 🔴 문제 상황
- [ ] 프론트엔드에서 파일 업로드 및 메시지 전송은 정상 작동
- [ ] 하지만 메시지 조회 API 응답에 `attachment` 필드가 누락되어 첨부파일이 UI에 표시되지 않음

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

---

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

---

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

---

### 4️⃣ SendMessageRequest DTO 확인
- [ ] `SendMessageRequest`에 `attachmentId` 필드가 있는지 확인

```java
public class SendMessageRequest {
    private String message;
    private String attachmentId;  // ✅ 이 필드 필요

    // getters, setters...
}
```

---

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

---

### 6️⃣ 메시지 조회 API 확인

#### 지원자용 메시지 조회
- [ ] `GET /api/applicant/chat/{sessionToken}/messages`
- [ ] 응답에 `attachment` 필드가 포함되는지 확인

#### 채용담당자용 메시지 조회
- [ ] `GET /api/chat/session/{sessionToken}/messages`
- [ ] 응답에 `attachment` 필드가 포함되는지 확인

> **참고**: MessageDTO의 `from()` 메서드가 올바르게 구현되어 있다면 자동으로 포함됩니다.

---

### 7️⃣ WebSocket 브로드캐스트 확인
- [ ] 새 메시지 전송 시 WebSocket으로 브로드캐스트할 때 `attachment` 포함
- [ ] Topic: `/topic/chat/{sessionToken}`
- [ ] `MessageDTO.from(message)` 사용하여 첨부파일 정보 자동 포함

---

## 📋 예상 응답 구조

### 첨부파일이 있는 메시지
```json
{
  "messageId": "msg-123",
  "message": "",
  "senderType": "APPLICANT",
  "readStatus": true,
  "sentAt": "2026-03-17T10:48:00",
  "attachment": {
    "attachmentId": "att-123",
    "fileName": "2025년 2분기-신용보증기금.pdf",
    "fileSize": 1024000,
    "fileType": "application/pdf",
    "fileUrl": "/uploads/files/att-123.pdf",
    "uploadedAt": "2026-03-17T10:48:00"
  }
}
```

### 첨부파일이 없는 메시지
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
