package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.command.ApplicantCommand;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.exception.GlobalExceptionHandler;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.dto.JoinApplicantDto;
import com.cookyuu.resume_chat.service.ApplicantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicantController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ApplicantController 테스트")
class ApplicantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicantService applicantService;

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
}
