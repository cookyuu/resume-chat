package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.command.ApplicantCommand;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.exception.GlobalExceptionHandler;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.dto.JoinApplicantDto;
import com.cookyuu.resume_chat.dto.LoginApplicantDto;
import com.cookyuu.resume_chat.service.ApplicantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

@WebMvcTest(
        controllers = ApplicantController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@Import(GlobalExceptionHandler.class)
@Disabled("Spring Security 통합으로 인한 MockMvc 설정 이슈 - Integration Test에서 테스트")
@DisplayName("ApplicantController 테스트")
class ApplicantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicantService applicantService;

    @MockBean
    private com.cookyuu.resume_chat.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.cookyuu.resume_chat.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.cookyuu.resume_chat.security.CustomUserDetailsService customUserDetailsService;

    @Nested
    @DisplayName("POST /api/applicant/join - 회원가입")
    class JoinApplicant {

        @Test
        @DisplayName("성공: 유효한 요청으로 회원가입 시 201 Created 응답")
        void success_validRequest_returns201Created() throws Exception {
            // Given
            JoinApplicantDto.Request request = new JoinApplicantDto.Request(
                    "test@example.com",
                    "password123",
                    "홍길동"
            );
            doNothing().when(applicantService).joinApplicant(any(ApplicantCommand.Create.class));

            // When & Then
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(applicantService, times(1)).joinApplicant(any(ApplicantCommand.Create.class));
        }

        @Test
        @DisplayName("실패: 이메일 형식 오류 시 400 Bad Request")
        void fail_invalidEmailFormat_returns400() throws Exception {
            // Given
            JoinApplicantDto.Request request = new JoinApplicantDto.Request(
                    "invalid-email",
                    "password123",
                    "홍길동"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.error.message").value("유효한 이메일 형식이 아닙니다"));

            verify(applicantService, never()).joinApplicant(any(ApplicantCommand.Create.class));
        }

        @Test
        @DisplayName("실패: 이메일 누락 시 400 Bad Request")
        void fail_missingEmail_returns400() throws Exception {
            // Given
            JoinApplicantDto.Request request = new JoinApplicantDto.Request(
                    null,
                    "password123",
                    "홍길동"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.error.message").exists());

            verify(applicantService, never()).joinApplicant(any(ApplicantCommand.Create.class));
        }

        @Test
        @DisplayName("실패: 비밀번호 길이 부족 시 400 Bad Request")
        void fail_passwordTooShort_returns400() throws Exception {
            // Given
            JoinApplicantDto.Request request = new JoinApplicantDto.Request(
                    "test@example.com",
                    "short",
                    "홍길동"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.error.message").value("비밀번호는 8자 이상 20자 이하로 입력해주세요"));

            verify(applicantService, never()).joinApplicant(any(ApplicantCommand.Create.class));
        }

        @Test
        @DisplayName("실패: 비밀번호 누락 시 400 Bad Request")
        void fail_missingPassword_returns400() throws Exception {
            // Given
            JoinApplicantDto.Request request = new JoinApplicantDto.Request(
                    "test@example.com",
                    null,
                    "홍길동"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.error.message").exists());

            verify(applicantService, never()).joinApplicant(any(ApplicantCommand.Create.class));
        }

        @Test
        @DisplayName("실패: 이름 길이 부족 시 400 Bad Request")
        void fail_nameTooShort_returns400() throws Exception {
            // Given
            JoinApplicantDto.Request request = new JoinApplicantDto.Request(
                    "test@example.com",
                    "password123",
                    "a"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.error.message").value("이름은 2자 이상 50자 이하로 입력해주세요"));

            verify(applicantService, never()).joinApplicant(any(ApplicantCommand.Create.class));
        }

        @Test
        @DisplayName("실패: 이름 누락 시 400 Bad Request")
        void fail_missingName_returns400() throws Exception {
            // Given
            JoinApplicantDto.Request request = new JoinApplicantDto.Request(
                    "test@example.com",
                    "password123",
                    null
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.error.message").exists());

            verify(applicantService, never()).joinApplicant(any(ApplicantCommand.Create.class));
        }

        @Test
        @DisplayName("실패: 이메일 중복 시 409 Conflict")
        void fail_duplicateEmail_returns409() throws Exception {
            // Given
            JoinApplicantDto.Request request = new JoinApplicantDto.Request(
                    "test@example.com",
                    "password123",
                    "홍길동"
            );
            doThrow(new BusinessException(ErrorCode.APPLICANT_ALREADY_EXISTS))
                    .when(applicantService).joinApplicant(any(ApplicantCommand.Create.class));

            // When & Then
            mockMvc.perform(post("/api/applicant/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.APPLICANT_ALREADY_EXISTS.getCode()))
                    .andExpect(jsonPath("$.error.message").value(ErrorCode.APPLICANT_ALREADY_EXISTS.getMessage()));

            verify(applicantService, times(1)).joinApplicant(any(ApplicantCommand.Create.class));
        }
    }

    @Nested
    @DisplayName("POST /api/applicant/login - 로그인")
    class LoginApplicant {

        @Test
        @DisplayName("성공: 앱 클라이언트 로그인 시 응답 본문에 JWT 토큰 모두 포함")
        void success_appClient_returnsTokensInBody() throws Exception {
            // Given
            LoginApplicantDto.Request request = new LoginApplicantDto.Request(
                    "test@example.com",
                    "password123"
            );
            LoginApplicantDto.Response response = new LoginApplicantDto.Response(
                    UUID.randomUUID(),
                    "test@example.com",
                    "홍길동",
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.access.token",
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.refresh.token"
            );
            when(applicantService.login(any(ApplicantCommand.Login.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/applicant/login")
                            .header("X-Client-Type", "app")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.uuid").exists())
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.name").value("홍길동"))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(applicantService, times(1)).login(any(ApplicantCommand.Login.class));
        }

        @Test
        @DisplayName("성공: 웹 클라이언트 로그인 시 Refresh Token은 쿠키로 전달")
        void success_webClient_returnsRefreshTokenInCookie() throws Exception {
            // Given
            LoginApplicantDto.Request request = new LoginApplicantDto.Request(
                    "test@example.com",
                    "password123"
            );
            LoginApplicantDto.Response response = new LoginApplicantDto.Response(
                    UUID.randomUUID(),
                    "test@example.com",
                    "홍길동",
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.access.token",
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.refresh.token"
            );
            when(applicantService.login(any(ApplicantCommand.Login.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/applicant/login")
                            .header("X-Client-Type", "web")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.uuid").exists())
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.name").value("홍길동"))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").doesNotExist()) // 웹은 응답에 없음
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andExpect(cookie().secure("refreshToken", true))
                    .andExpect(cookie().maxAge("refreshToken", 7 * 24 * 60 * 60))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(applicantService, times(1)).login(any(ApplicantCommand.Login.class));
        }

        @Test
        @DisplayName("성공: 헤더가 없는 경우 기본값(웹)으로 처리")
        void success_noHeader_defaultsToWeb() throws Exception {
            // Given
            LoginApplicantDto.Request request = new LoginApplicantDto.Request(
                    "test@example.com",
                    "password123"
            );
            LoginApplicantDto.Response response = new LoginApplicantDto.Response(
                    UUID.randomUUID(),
                    "test@example.com",
                    "홍길동",
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.access.token",
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.refresh.token"
            );
            when(applicantService.login(any(ApplicantCommand.Login.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/applicant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                    .andExpect(cookie().exists("refreshToken"));

            verify(applicantService, times(1)).login(any(ApplicantCommand.Login.class));
        }

        @Test
        @DisplayName("실패: 이메일 형식 오류 시 400 Bad Request")
        void fail_invalidEmailFormat_returns400() throws Exception {
            // Given
            LoginApplicantDto.Request request = new LoginApplicantDto.Request(
                    "invalid-email",
                    "password123"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.error.message").value("유효한 이메일 형식이 아닙니다"));

            verify(applicantService, never()).login(any(ApplicantCommand.Login.class));
        }

        @Test
        @DisplayName("실패: 이메일 누락 시 400 Bad Request")
        void fail_missingEmail_returns400() throws Exception {
            // Given
            LoginApplicantDto.Request request = new LoginApplicantDto.Request(
                    null,
                    "password123"
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()));

            verify(applicantService, never()).login(any(ApplicantCommand.Login.class));
        }

        @Test
        @DisplayName("실패: 비밀번호 누락 시 400 Bad Request")
        void fail_missingPassword_returns400() throws Exception {
            // Given
            LoginApplicantDto.Request request = new LoginApplicantDto.Request(
                    "test@example.com",
                    null
            );

            // When & Then
            mockMvc.perform(post("/api/applicant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()));

            verify(applicantService, never()).login(any(ApplicantCommand.Login.class));
        }

        @Test
        @DisplayName("실패: 잘못된 이메일/비밀번호 시 401 Unauthorized")
        void fail_invalidCredentials_returns401() throws Exception {
            // Given
            LoginApplicantDto.Request request = new LoginApplicantDto.Request(
                    "test@example.com",
                    "wrongpassword"
            );
            when(applicantService.login(any(ApplicantCommand.Login.class)))
                    .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

            // When & Then
            mockMvc.perform(post("/api/applicant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()))
                    .andExpect(jsonPath("$.error.message").value("이메일 또는 비밀번호가 일치하지 않습니다"));

            verify(applicantService, times(1)).login(any(ApplicantCommand.Login.class));
        }

        @Test
        @DisplayName("실패: 계정 잠김 시 403 Forbidden")
        void fail_accountLocked_returns403() throws Exception {
            // Given
            LoginApplicantDto.Request request = new LoginApplicantDto.Request(
                    "test@example.com",
                    "password123"
            );
            when(applicantService.login(any(ApplicantCommand.Login.class)))
                    .thenThrow(new BusinessException(ErrorCode.ACCOUNT_LOCKED));

            // When & Then
            mockMvc.perform(post("/api/applicant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.ACCOUNT_LOCKED.getCode()))
                    .andExpect(jsonPath("$.error.message").value("계정이 잠겨있습니다"));

            verify(applicantService, times(1)).login(any(ApplicantCommand.Login.class));
        }
    }
}
