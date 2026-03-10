# Resume Chat - 코딩 관례 (Coding Conventions)

> 이 문서는 Resume Chat 프로젝트의 코딩 컨벤션 및 개발 가이드라인을 정의합니다.

## 테스트 전략
### 1. Domain Unit Test (필수)

**목적**: 도메인 로직, 비즈니스 규칙 검증

**특징**:
- JPA 없이 순수 자바 객체로 테스트
- `@SpringBootTest` 사용 안 함
- Mock 사용 안 함 (순수 단위 테스트)

### 2. Service Layer Test

**목적**: 비즈니스 로직 검증

**특징**:
- `@ExtendWith(MockitoExtension.class)` 사용
- Mock 객체로 의존성 격리
- 트랜잭션 동작은 테스트하지 않음

### 3. Repository Test

**목적**: 데이터베이스 쿼리 동작 검증

**특징**:
- `@DataJpaTest` 사용
- 실제 DB 동작 테스트 (H2 인메모리)

### 4. Controller Test

**목적**: API 엔드포인트, 요청/응답 검증

**특징**:
- `@WebMvcTest` 사용
- MockMvc로 HTTP 테스트
- Service는 Mock으로 처리

### 5. Integration Test

**목적**: 전체 플로우 E2E 검증

**특징**:
- `@SpringBootTest` 사용
- 실제 애플리케이션 컨텍스트 로드
- 여러 계층 통합 테스트


### 테스트 필수 규칙

1. **모든 기능 개발 시 테스트 작성 필수**
   - 최소한 Service Layer Test 작성
   - 복잡한 도메인 로직은 Domain Unit Test 추가

2. **테스트 네이밍**
   - Given-When-Then 패턴
   - 메서드명: `{메서드명}_{시나리오}_{예상결과}`
   - `@DisplayName`으로 한글 설명 추가

3. **AssertJ 사용**
   - JUnit Assertion 대신 AssertJ 사용
   - 가독성 좋은 체이닝 스타일

4. **테스트 격리**
   - 각 테스트는 독립적으로 실행 가능해야 함
   - 테스트 간 의존성 금지

---

## 예외 처리

### 커스텀 예외

```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

### ErrorCode Enum

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Applicant
    DUPLICATE_EMAIL("A001", "이미 사용 중인 이메일입니다"),
    APPLICANT_NOT_FOUND("A002", "존재하지 않는 사용자입니다"),
    INVALID_PASSWORD("A003", "비밀번호가 일치하지 않습니다"),
    ACCOUNT_LOCKED("A004", "계정이 잠겨있습니다"),

    // Resume
    RESUME_NOT_FOUND("R001", "존재하지 않는 이력서입니다"),
    RESUME_ACCESS_DENIED("R002", "이력서에 접근할 권한이 없습니다"),

    // File
    FILE_UPLOAD_FAILED("F001", "파일 업로드에 실패했습니다"),
    INVALID_FILE_TYPE("F002", "지원하지 않는 파일 형식입니다");

    private final String code;
    private final String message;
}
```

### 전역 예외 핸들러

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("V001", message));
    }
}
```

---

## 커밋 메시지

### 형식

```
<type>: <subject>
<body>
```

### Type

- `feature`: 새로운 기능 추가
- `fix`: 버그 수정
- `refactor`: 리팩토링 (기능 변경 없음)
- `docs`: 문서 수정
- `test`: 테스트 코드 추가/수정
- `chore`: 빌드, 설정 파일 수정
- `style`: 코드 포맷팅 (기능 변경 없음)

### 예시

```
feature: 채팅 메시지 읽음 처리 기능 구현

- ChatMessage에 readStatus 필드 추가
- markAsRead() 메서드 구현
- 메시지 조회 시 자동 읽음 처리 로직 추가
- 테스트 코드 작성 (Unit, Service, Integration)
```

---

## 체크리스트

새로운 기능 개발 시 다음 항목을 확인하세요:

### Test
- [ ] 최소 Service Layer Test 작성
- [ ] Given-When-Then 패턴
- [ ] AssertJ 사용
- [ ] `@DisplayName`으로 한글 설명

### Git
- [ ] 커밋 메시지 형식 준수
- [ ] 하나의 커밋은 하나의 의미 있는 변경사항
- [ ] 테스트 통과 확인 후 커밋

**관련 문서**: [README.md](./README.md), [plan.md](./plan.md), [tasks.md](./tasks.md)
