# 백엔드 수정 요청사항

> 이 문서는 프론트엔드 개발 중 발견된 백엔드 수정 필요사항을 정리한 문서입니다.

**작성일**: 2026-03-18
**작성자**: Frontend Team

---

## 🟢 완료 - Refresh Token API 구현 완료 ✅

### 구현 상태
- ✅ JWT Access Token 만료 시 자동 갱신 가능
- ✅ 프론트엔드에서 `/api/applicant/refresh` API 호출 정상 동작
- ✅ HttpOnly 쿠키 기반 Refresh Token 처리
- ✅ 토큰 만료 시 로그인 페이지 리다이렉트

### API 명세

#### Endpoint
```
POST /api/applicant/refresh
```

#### Request
- **Headers**: 없음 (HttpOnly 쿠키에서 Refresh Token 자동 읽기)
- **Body**: 없음

#### Response (성공)
```json
{
  "code": 200,
  "message": "토큰 갱신 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

#### Response (실패)
```json
{
  "code": 401,
  "message": "유효하지 않은 Refresh Token입니다.",
  "error": {
    "code": "INVALID_REFRESH_TOKEN",
    "message": "유효하지 않은 Refresh Token입니다."
  }
}
```

### 구현 체크리스트

**백엔드 구현 완료**:
- [x] `POST /api/applicant/refresh` 엔드포인트 생성 ✅
- [x] HttpOnly 쿠키에서 Refresh Token 읽기 ✅
  ```java
  @PostMapping("/refresh")
  public ResponseEntity<?> refreshToken(
      @CookieValue(name = "refreshToken", required = false) String refreshToken
  ) {
      // ...
  }
  ```
- [x] Refresh Token 유효성 검증 ✅
  - [x] 토큰 만료 여부 확인 ✅
  - [x] 토큰 서명 검증 ✅
  - [x] DB에 저장된 토큰과 일치 여부 확인 (선택) ✅
- [x] 새로운 Access Token 발급 ✅
  ```java
  String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
  ```
- [ ] 응답 JSON 반환
  ```java
  return ResponseEntity.ok(new ApiResponse<>(200, "토큰 갱신 성공",
      Map.of("accessToken", newAccessToken)));
  ```
- [x] 예외 처리 ✅
  - [x] Refresh Token 없음 → 401 ✅
  - [x] Refresh Token 만료 → 401 ✅
  - [x] Refresh Token 유효하지 않음 → 401 ✅

**CORS 설정 확인**:
- [x] `/api/applicant/refresh` 경로 CORS 허용 확인 ✅
- [x] `withCredentials: true` 설정 시 쿠키 전송 가능하도록 설정 ✅
  ```java
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowCredentials(true); // 중요!
      config.addAllowedOrigin("http://localhost:5173");
      config.addAllowedHeader("*");
      config.addAllowedMethod("*");
      // ...
  }
  ```

**보안 고려사항**:
- [x] HttpOnly 쿠키 사용 (XSS 방지) ✅
- [x] Secure 플래그 설정 (HTTPS 환경) ✅
- [x] SameSite=Strict or Lax 설정 (CSRF 방지) ✅
- [ ] Refresh Token 로테이션 고려 (선택) - 필요 시 구현
  - 새로운 Access Token 발급 시 Refresh Token도 재발급

**프론트엔드 연동 상태**:
- [x] Axios Response Interceptor에서 401 에러 감지 ✅ (client.ts:51)
- [x] `/api/applicant/refresh` API 호출 ✅ (client.ts:73)
- [x] `withCredentials: true` 설정 ✅ (client.ts:74)
- [x] 새 Access Token으로 zustand store 업데이트 ✅ (client.ts:80)
- [x] 실패한 요청 재시도 ✅ (client.ts:85)
- [x] Refresh 실패 시 로그인 페이지 리다이렉트 ✅ (client.ts:98)
- [x] 중복 팝업 방지 로직 ✅ (client.ts:91-92)
- [x] 동시 요청 큐 처리 ✅ (client.ts:59-65)

**테스트 시나리오**:
```bash
# 1. 로그인 (Refresh Token 쿠키 저장됨)
curl -X POST http://localhost:31000/api/applicant/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  -c cookies.txt

# 2. Access Token 만료 후 갱신 요청
curl -X POST http://localhost:31000/api/applicant/refresh \
  -b cookies.txt

# 3. 새 Access Token으로 API 호출
curl -X GET http://localhost:31000/api/applicant/profile \
  -H "Authorization: Bearer NEW_ACCESS_TOKEN"
```

---

## 🟡 이력서 목록 API - 채팅 세션 개수 필드 추가

### 요청 배경
- 현재 이력서 목록에서 "조회수"(`viewCnt`)를 표시 중
- UX 개선을 위해 **각 이력서당 생성된 채팅 세션 개수**를 표시하고자 함
- 채팅 세션 개수가 이력서의 활용도를 더 잘 나타냄

### API 수정 요청

#### Endpoint
```
GET /api/applicant/resume
```

#### 현재 응답 구조
```json
{
  "code": 200,
  "message": "이력서 목록 조회 성공",
  "data": {
    "resumes": [
      {
        "resumeId": 1,
        "slug": "abc123",
        "title": "프론트엔드 개발자 이력서",
        "viewCnt": 42,  // ← 현재 필드
        "isPublic": true,
        "createdAt": "2024-03-10T10:30:00",
        "updatedAt": "2024-03-15T14:20:00"
      }
    ]
  }
}
```

#### 요청하는 응답 구조
```json
{
  "code": 200,
  "message": "이력서 목록 조회 성공",
  "data": {
    "resumes": [
      {
        "resumeId": 1,
        "slug": "abc123",
        "title": "프론트엔드 개발자 이력서",
        "sessionCount": 7,  // ← 추가 필드 (또는 viewCnt 대체)
        "isPublic": true,
        "createdAt": "2024-03-10T10:30:00",
        "updatedAt": "2024-03-15T14:20:00"
      }
    ]
  }
}
```

### 구현 체크리스트

**백엔드 수정 필요사항**:
- [ ] DTO 클래스 수정
  ```java
  public class ResumeListResponseDto {
      private Long resumeId;
      private String slug;
      private String title;
      private Integer sessionCount;  // 새로운 필드
      // private Integer viewCnt;  // 제거하거나 유지
      private Boolean isPublic;
      private LocalDateTime createdAt;
      private LocalDateTime updatedAt;
  }
  ```

- [ ] SQL 쿼리 수정 (각 이력서당 세션 개수 집계)
  ```sql
  SELECT
      r.resume_id,
      r.slug,
      r.title,
      r.is_public,
      r.created_at,
      r.updated_at,
      COUNT(cs.session_id) AS session_count  -- 채팅 세션 개수
  FROM resumes r
  LEFT JOIN chat_sessions cs ON r.resume_id = cs.resume_id
  WHERE r.applicant_id = :applicantId
  GROUP BY r.resume_id
  ORDER BY r.created_at DESC;
  ```

- [ ] JPA 방식 구현 (선택)
  ```java
  @Repository
  public interface ResumeRepository extends JpaRepository<Resume, Long> {

      @Query("SELECT new com.example.dto.ResumeListResponseDto(" +
             "r.resumeId, r.slug, r.title, " +
             "COUNT(cs.sessionId), " +  // sessionCount
             "r.isPublic, r.createdAt, r.updatedAt) " +
             "FROM Resume r " +
             "LEFT JOIN ChatSession cs ON cs.resume.resumeId = r.resumeId " +
             "WHERE r.applicant.applicantId = :applicantId " +
             "GROUP BY r.resumeId")
      List<ResumeListResponseDto> findAllByApplicantIdWithSessionCount(@Param("applicantId") Long applicantId);
  }
  ```

- [ ] Service 레이어 수정
  ```java
  public List<ResumeListResponseDto> getMyResumes(Long applicantId) {
      return resumeRepository.findAllByApplicantIdWithSessionCount(applicantId);
  }
  ```

**테이블 관계 확인 필요**:
- [ ] `resumes` 테이블과 `chat_sessions` 테이블의 관계 확인
  - FK: `chat_sessions.resume_id` → `resumes.resume_id`
- [ ] 테이블명, 컬럼명 확인 (실제 스키마에 맞게 수정)

**성능 고려사항**:
- [ ] `chat_sessions` 테이블에 `resume_id` 인덱스 존재 여부 확인
  ```sql
  CREATE INDEX idx_chat_sessions_resume_id ON chat_sessions(resume_id);
  ```
- [ ] 이력서 수가 많을 경우 페이지네이션 고려

**테스트 시나리오**:
```bash
# 1. 이력서 목록 조회
curl -X GET http://localhost:31000/api/applicant/resume \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 2. 응답에서 sessionCount 필드 확인
# {
#   "code": 200,
#   "data": {
#     "resumes": [
#       {
#         "resumeId": 1,
#         "sessionCount": 7,  // ← 이 필드가 있어야 함
#         ...
#       }
#     ]
#   }
# }
```

**마이그레이션 계획** (viewCnt 제거하는 경우):
- [ ] 프론트엔드 배포 전에 백엔드 먼저 배포 (필드 추가)
- [ ] 프론트엔드에서 `viewCnt` 사용하는 코드 모두 `sessionCount`로 변경
- [ ] 프론트엔드 배포 후 백엔드에서 `viewCnt` 필드 제거 (선택)

**또는 점진적 마이그레이션** (둘 다 제공):
- [ ] 당분간 `viewCnt`와 `sessionCount` 둘 다 제공
- [ ] 프론트엔드에서 `sessionCount` 사용 확인 후
- [ ] 추후 `viewCnt` 필드 deprecated → 제거

---

## 📋 기타 수정 요청사항

### (추후 추가 예정)

---

**작성 규칙**:
- 긴급도: 🔴 긴급 / 🟡 중간 / 🟢 낮음
- 각 요청사항마다 구현 체크리스트 포함
- API 명세 및 예시 코드 제공
- 테스트 시나리오 포함
