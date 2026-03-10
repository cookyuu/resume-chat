# Resume Chat - 코딩 관례 (Coding Conventions)

> 이 문서는 Resume Chat 프로젝트의 코딩 컨벤션 및 개발 가이드라인을 정의합니다.

**작성일**: 2026-03-10
**버전**: 1.0

---

## 목차

1. [프로젝트 구조](#프로젝트-구조)
2. [패키지 네이밍](#패키지-네이밍)
3. [Domain 계층](#domain-계층)
4. [Service 계층](#service-계층)
5. [Repository 계층](#repository-계층)
6. [Controller 계층](#controller-계층)
7. [DTO 규칙](#dto-규칙)
8. [테스트 전략](#테스트-전략)
9. [예외 처리](#예외-처리)
10. [커밋 메시지](#커밋-메시지)

---

## 프로젝트 구조

```
src/main/java/com/cookyuu/resume_chat/
├── common/           # 공통 컴포넌트
│   ├── domain/      # BaseTimeEntity 등
│   ├── enums/       # Enum 클래스들
│   └── exception/   # 공통 예외 처리
├── config/          # 설정 클래스
├── controller/      # REST API 컨트롤러
├── domain/          # 도메인 엔티티 (JPA Entity)
├── dto/             # Data Transfer Object
├── repository/      # JPA Repository
├── security/        # 보안 관련
└── service/         # 비즈니스 로직
```

**원칙**:
- 계층별로 명확히 분리
- 순환 의존성 금지
- 단방향 의존: `Controller → Service → Repository → Domain`

---

## 패키지 네이밍

### ✅ DO
- `domain`: 도메인 엔티티 (JPA Entity)
- `service`: 비즈니스 로직
- `repository`: 데이터 접근 계층
- `controller`: API 엔드포인트
- `dto`: Request/Response 객체

### ❌ DON'T
- `entity` 사용 금지 → `domain` 사용
- `model`, `models` 혼용 금지
- `util`, `utils` 혼용 금지 (하나로 통일)

---

## Domain 계층

### 기본 원칙

1. **JPA 라이프사이클 훅 사용 금지**
   - `@PrePersist`, `@PostPersist` 사용 금지
   - 생성 로직은 정적 팩토리 메서드에서 명시적으로 처리

2. **정적 팩토리 메서드 필수**
   - 엔티티 생성 시 `createNew...()` 패턴 사용
   - 모든 필수 필드를 파라미터로 받음
   - UUID, 기본값은 팩토리 메서드에서 설정

3. **불변성 최대한 보장**
   - `@Setter` 사용 금지
   - 상태 변경은 명시적인 비즈니스 메서드로만

### 예시

```java
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "rc_applicant")
public class Applicant extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @Column(nullable = false)
    private String email;

    // ✅ 정적 팩토리 메서드 - 모든 생성은 이 메서드를 통해서만
    public static Applicant createNewApplicant(String email, String name, String encodedPassword) {
        return Applicant.builder()
                .uuid(UUID.randomUUID())  // UUID는 여기서 생성
                .email(email)
                .name(name)
                .password(encodedPassword)
                .status(ApplicantStatus.ACTIVE)  // 기본값 설정
                .loginFailCnt(0)
                .build();
    }

    // ✅ 비즈니스 로직 메서드 - 상태 변경은 명시적으로
    public void loginFailed() {
        incrementLoginFailCount();
        if (this.loginFailCnt >= 5) {
            lockAccount();
        }
    }

    private void incrementLoginFailCount() {
        this.loginFailCnt++;
    }

    private void lockAccount() {
        this.status = ApplicantStatus.INACTIVE;
    }
}
```

### ❌ 금지 사항

```java
// ❌ @PrePersist 사용 금지
@PrePersist
public void prePersist() {
    if (this.uuid == null) {
        this.uuid = UUID.randomUUID();
    }
}

// ❌ @Setter 사용 금지
@Setter
private String email;

// ❌ 생성자를 public으로 노출 금지
public Applicant() {}
```

### ✅ 필수 사항

```java
// ✅ Builder 패턴 사용
@Builder

// ✅ protected 기본 생성자 (JPA 요구사항)
@NoArgsConstructor(access = AccessLevel.PROTECTED)

// ✅ 정적 팩토리 메서드로 생성
public static Applicant createNewApplicant(...) {
    return Applicant.builder()
            .uuid(UUID.randomUUID())
            // ...
            .build();
}
```

---

## Service 계층

### 기본 원칙

1. **트랜잭션 관리**
   - 쓰기 작업: `@Transactional`
   - 읽기 전용: `@Transactional(readOnly = true)`

2. **비즈니스 예외 처리**
   - 커스텀 예외(`BusinessException`) 사용
   - 명확한 에러 코드와 메시지 제공

3. **단일 책임 원칙**
   - 하나의 서비스는 하나의 도메인만 담당
   - 예: `ApplicantService`, `ResumeService`, `ChatService`

### 예시

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicantService {
    private final ApplicantRepository applicantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApplicantResponse join(JoinRequest request) {
        // 1. 비즈니스 검증
        validateDuplicateEmail(request.getEmail());

        // 2. 도메인 객체 생성 (정적 팩토리 메서드 사용)
        Applicant applicant = Applicant.createNewApplicant(
            request.getEmail(),
            request.getName(),
            passwordEncoder.encode(request.getPassword())
        );

        // 3. 저장
        Applicant saved = applicantRepository.save(applicant);

        // 4. DTO 변환 후 반환
        return ApplicantResponse.from(saved);
    }

    private void validateDuplicateEmail(String email) {
        if (applicantRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }
}
```

### 네이밍 규칙

- **조회**: `get`, `find`, `search`
  - `getApplicantByUuid(UUID uuid)`
  - `findResumesByApplicant(UUID applicantUuid)`

- **생성**: `create`, `register`
  - `createResume(CreateResumeRequest request)`
  - `registerApplicant(JoinRequest request)`

- **수정**: `update`, `modify`
  - `updateResume(UUID resumeSlug, UpdateRequest request)`

- **삭제**: `delete`, `remove`
  - `deleteResume(UUID resumeSlug)`

- **검증**: `validate`, `check`
  - `validateDuplicateEmail(String email)`

---

## Repository 계층

### 기본 원칙

1. **JpaRepository 상속**
2. **메서드 네이밍 규칙 준수**
   - Spring Data JPA Query Method 컨벤션 따름
3. **복잡한 쿼리는 `@Query` 사용**

### 예시

```java
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    // ✅ 메서드 이름으로 쿼리 생성
    Optional<Applicant> findByEmail(String email);
    Optional<Applicant> findByUuid(UUID uuid);
    boolean existsByEmail(String email);

    // ✅ 복잡한 쿼리는 @Query 사용
    @Query("SELECT a FROM Applicant a WHERE a.status = :status AND a.loginFailCnt < :maxFailCount")
    List<Applicant> findActiveApplicants(
        @Param("status") ApplicantStatus status,
        @Param("maxFailCount") int maxFailCount
    );
}
```

---

## Controller 계층

### 기본 원칙

1. **RESTful API 설계**
   - 리소스 중심 URL
   - HTTP 메서드 의미에 맞게 사용

2. **응답 형식 통일**
   - `ApiResponse<T>` 래퍼 사용
   - 성공/실패 일관된 형식

3. **검증**
   - `@Valid` + Bean Validation 사용
   - Controller에서는 형식 검증만, 비즈니스 검증은 Service에서

### 예시

```java
@RestController
@RequestMapping("/api/applicant")
@RequiredArgsConstructor
public class ApplicantController {
    private final ApplicantService applicantService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<ApplicantResponse>> join(
            @Valid @RequestBody JoinRequest request
    ) {
        ApplicantResponse response = applicantService.join(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ApplicantResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ApplicantResponse response = applicantService.getProfile(userDetails.getUuid());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### URL 네이밍

- **컬렉션**: 복수형 명사
  - `GET /api/resumes` - 목록 조회
  - `POST /api/resumes` - 생성

- **단일 리소스**: 복수형 명사 + ID
  - `GET /api/resumes/{resumeSlug}` - 단건 조회
  - `DELETE /api/resumes/{resumeSlug}` - 삭제

- **하위 리소스**: 계층 구조
  - `GET /api/resumes/{resumeSlug}/chats` - 이력서의 채팅 세션 목록

- **액션**: 동사 사용 (예외적)
  - `POST /api/applicant/login` - 로그인
  - `POST /api/applicant/join` - 회원가입

---

## DTO 규칙

### 기본 원칙

1. **Request/Response 분리**
   - `XxxRequest`: 요청 DTO
   - `XxxResponse`: 응답 DTO

2. **Inner Class 활용**
   - 관련된 DTO는 하나의 클래스 내부에 정의

3. **정적 팩토리 메서드**
   - `from(Entity)`: Entity → DTO 변환
   - `toEntity()`: DTO → Entity 변환 (필요 시)

### 예시

```java
public class ApplicantDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JoinRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "이름은 필수입니다")
        private String name;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        private String password;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApplicantResponse {
        private UUID uuid;
        private String email;
        private String name;
        private ApplicantStatus status;
        private LocalDateTime createdAt;

        public static ApplicantResponse from(Applicant applicant) {
            return ApplicantResponse.builder()
                    .uuid(applicant.getUuid())
                    .email(applicant.getEmail())
                    .name(applicant.getName())
                    .status(applicant.getStatus())
                    .createdAt(applicant.getCreatedAt())
                    .build();
        }
    }
}
```

---

## 테스트 전략

### 1. Domain Unit Test (필수)

**목적**: 도메인 로직, 비즈니스 규칙 검증

**특징**:
- JPA 없이 순수 자바 객체로 테스트
- `@SpringBootTest` 사용 안 함
- Mock 사용 안 함 (순수 단위 테스트)

**예시**:
```java
@DisplayName("Applicant 도메인 테스트")
class ApplicantTest {

    @Test
    @DisplayName("createNewApplicant - UUID 자동 생성")
    void createNewApplicant_generatesUuid() {
        // Given
        String email = "test@example.com";
        String name = "홍길동";
        String password = "password123";

        // When
        Applicant applicant = Applicant.createNewApplicant(email, name, password);

        // Then
        assertThat(applicant.getUuid()).isNotNull();
        assertThat(applicant.getEmail()).isEqualTo(email);
        assertThat(applicant.getStatus()).isEqualTo(ApplicantStatus.ACTIVE);
    }

    @Test
    @DisplayName("loginFailed - 5회 실패 시 계정 잠금")
    void loginFailed_locksAccountAfter5Failures() {
        // Given
        Applicant applicant = Applicant.createNewApplicant("test@example.com", "홍길동", "password");

        // When
        for (int i = 0; i < 5; i++) {
            applicant.loginFailed();
        }

        // Then
        assertThat(applicant.isAccountLocked()).isTrue();
        assertThat(applicant.getLoginFailCnt()).isEqualTo(5);
    }
}
```

### 2. Service Layer Test

**목적**: 비즈니스 로직 검증

**특징**:
- `@ExtendWith(MockitoExtension.class)` 사용
- Mock 객체로 의존성 격리
- 트랜잭션 동작은 테스트하지 않음

**예시**:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicantService 테스트")
class ApplicantServiceTest {

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ApplicantService applicantService;

    @Test
    @DisplayName("join - 성공")
    void join_success() {
        // Given
        JoinRequest request = new JoinRequest("test@example.com", "홍길동", "password123");
        when(applicantRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ApplicantResponse response = applicantService.join(request);

        // Then
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        verify(applicantRepository).save(any(Applicant.class));
    }

    @Test
    @DisplayName("join - 중복 이메일 예외")
    void join_duplicateEmail_throwsException() {
        // Given
        JoinRequest request = new JoinRequest("test@example.com", "홍길동", "password123");
        when(applicantRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> applicantService.join(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DUPLICATE_EMAIL");
    }
}
```

### 3. Repository Test

**목적**: 데이터베이스 쿼리 동작 검증

**특징**:
- `@DataJpaTest` 사용
- 실제 DB 동작 테스트 (H2 인메모리)

**예시**:
```java
@DataJpaTest
@DisplayName("ApplicantRepository 테스트")
class ApplicantRepositoryTest {

    @Autowired
    private ApplicantRepository applicantRepository;

    @Test
    @DisplayName("findByEmail - 존재하는 이메일 조회")
    void findByEmail_existingEmail() {
        // Given
        Applicant applicant = Applicant.createNewApplicant("test@example.com", "홍길동", "password");
        applicantRepository.save(applicant);

        // When
        Optional<Applicant> found = applicantRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }
}
```

### 4. Controller Test

**목적**: API 엔드포인트, 요청/응답 검증

**특징**:
- `@WebMvcTest` 사용
- MockMvc로 HTTP 테스트
- Service는 Mock으로 처리

**예시**:
```java
@WebMvcTest(ApplicantController.class)
@DisplayName("ApplicantController 테스트")
class ApplicantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicantService applicantService;

    @Test
    @DisplayName("POST /api/applicant/join - 성공")
    void join_success() throws Exception {
        // Given
        JoinRequest request = new JoinRequest("test@example.com", "홍길동", "password123");
        ApplicantResponse response = new ApplicantResponse(UUID.randomUUID(), "test@example.com", "홍길동", ApplicantStatus.ACTIVE, LocalDateTime.now());
        when(applicantService.join(any(JoinRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/applicant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }
}
```

### 5. Integration Test

**목적**: 전체 플로우 E2E 검증

**특징**:
- `@SpringBootTest` 사용
- 실제 애플리케이션 컨텍스트 로드
- 여러 계층 통합 테스트

**예시**:
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Applicant 통합 테스트")
class ApplicantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Test
    @DisplayName("회원가입 → 로그인 → 프로필 조회 플로우")
    void fullAuthFlow() throws Exception {
        // 1. 회원가입
        JoinRequest joinRequest = new JoinRequest("test@example.com", "홍길동", "password123");
        mockMvc.perform(post("/api/applicant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated());

        // 2. 로그인
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/applicant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");

        // 3. 프로필 조회
        mockMvc.perform(get("/api/applicant/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }
}
```

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

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
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

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

---

## 체크리스트

새로운 기능 개발 시 다음 항목을 확인하세요:

### Domain
- [ ] 정적 팩토리 메서드로 객체 생성
- [ ] `@PrePersist` 사용 안 함
- [ ] `@Setter` 사용 안 함
- [ ] 비즈니스 로직 메서드 명확히 명명
- [ ] Domain Unit Test 작성

### Service
- [ ] `@Transactional` 적절히 사용
- [ ] 커스텀 예외로 에러 처리
- [ ] 단일 책임 원칙 준수
- [ ] Service Layer Test 작성

### Repository
- [ ] 메서드 네이밍 규칙 준수
- [ ] 복잡한 쿼리는 `@Query` 사용
- [ ] Repository Test 작성 (필요 시)

### Controller
- [ ] RESTful API 설계
- [ ] `@Valid`로 입력 검증
- [ ] `ApiResponse` 래퍼 사용
- [ ] Controller Test 작성 (필요 시)

### DTO
- [ ] Request/Response 분리
- [ ] Bean Validation 어노테이션 추가
- [ ] `from()` 정적 팩토리 메서드 구현

### Test
- [ ] 최소 Service Layer Test 작성
- [ ] Given-When-Then 패턴
- [ ] AssertJ 사용
- [ ] `@DisplayName`으로 한글 설명

### Git
- [ ] 커밋 메시지 형식 준수
- [ ] 하나의 커밋은 하나의 의미 있는 변경사항
- [ ] 테스트 통과 확인 후 커밋

---

**문서 버전**: 1.0
**최종 업데이트**: 2026-03-10
**관련 문서**: [README.md](./README.md), [plan.md](./plan.md), [tasks.md](./tasks.md)
