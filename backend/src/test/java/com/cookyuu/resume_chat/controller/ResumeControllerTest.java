package com.cookyuu.resume_chat.controller;

import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.dto.ResumeDto;
import com.cookyuu.resume_chat.security.CustomUserDetails;
import com.cookyuu.resume_chat.security.CustomUserDetailsService;
import com.cookyuu.resume_chat.security.JwtAuthenticationFilter;
import com.cookyuu.resume_chat.security.JwtTokenProvider;
import com.cookyuu.resume_chat.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResumeController.class)
@DisplayName("ResumeController 테스트")
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeService resumeService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("이력서 업로드 성공")
    @WithMockUser
    void uploadResume_Success() throws Exception {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        UUID resumeSlug = UUID.randomUUID();
        String title = "백엔드 개발자 이력서";
        String description = "3년차 Spring 개발자";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        ResumeDto.UploadResponse response = new ResumeDto.UploadResponse(
                resumeSlug,
                title,
                description,
                "resume.pdf",
                "http://localhost:3000/chat/" + resumeSlug,
                LocalDateTime.now()
        );

        CustomUserDetails userDetails = new CustomUserDetails(
                applicantUuid,
                "test@example.com",
                "password",
                false
        );

        given(resumeService.uploadResume(eq(applicantUuid), eq(title), eq(description), any()))
                .willReturn(response);

        // When & Then
        mockMvc.perform(multipart("/api/applicant/resume")
                        .file(file)
                        .param("title", title)
                        .param("description", description)
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resumeSlug").value(resumeSlug.toString()))
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.chatLink").exists());

        verify(resumeService, times(1))
                .uploadResume(eq(applicantUuid), eq(title), eq(description), any());
    }

    @Test
    @DisplayName("이력서 업로드 실패 - 파일 없음")
    @WithMockUser
    void uploadResume_Fail_NoFile() throws Exception {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        String title = "백엔드 개발자 이력서";

        CustomUserDetails userDetails = new CustomUserDetails(
                applicantUuid,
                "test@example.com",
                "password",
                false
        );

        // When & Then
        mockMvc.perform(multipart("/api/applicant/resume")
                        .param("title", title)
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(resumeService, never()).uploadResume(any(), any(), any(), any());
    }

    @Test
    @DisplayName("이력서 목록 조회 성공")
    @WithMockUser
    void getMyResumes_Success() throws Exception {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        UUID resumeSlug1 = UUID.randomUUID();
        UUID resumeSlug2 = UUID.randomUUID();

        List<ResumeDto.ResumeInfo> resumes = List.of(
                new ResumeDto.ResumeInfo(
                        resumeSlug1,
                        "이력서1",
                        "설명1",
                        "resume1.pdf",
                        "http://localhost:3000/chat/" + resumeSlug1,
                        0,
                        LocalDateTime.now()
                ),
                new ResumeDto.ResumeInfo(
                        resumeSlug2,
                        "이력서2",
                        "설명2",
                        "resume2.pdf",
                        "http://localhost:3000/chat/" + resumeSlug2,
                        5,
                        LocalDateTime.now()
                )
        );

        CustomUserDetails userDetails = new CustomUserDetails(
                applicantUuid,
                "test@example.com",
                "password",
                false
        );

        given(resumeService.getApplicantResumes(applicantUuid)).willReturn(resumes);

        // When & Then
        mockMvc.perform(get("/api/applicant/resume")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].resumeSlug").value(resumeSlug1.toString()))
                .andExpect(jsonPath("$.data[0].chatLink").exists())
                .andExpect(jsonPath("$.data[1].viewCnt").value(5));

        verify(resumeService, times(1)).getApplicantResumes(applicantUuid);
    }

    @Test
    @DisplayName("이력서 삭제 성공")
    @WithMockUser
    void deleteResume_Success() throws Exception {
        // Given
        UUID applicantUuid = UUID.randomUUID();
        UUID resumeSlug = UUID.randomUUID();

        CustomUserDetails userDetails = new CustomUserDetails(
                applicantUuid,
                "test@example.com",
                "password",
                false
        );

        doNothing().when(resumeService).deleteResume(applicantUuid, resumeSlug);

        // When & Then
        mockMvc.perform(delete("/api/applicant/resume/{resumeSlug}", resumeSlug)
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(resumeService, times(1)).deleteResume(applicantUuid, resumeSlug);
    }

    @Test
    @DisplayName("이력서 업로드 실패 - 인증 없음")
    void uploadResume_Fail_Unauthorized() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/applicant/resume")
                        .file(file)
                        .param("title", "이력서"))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(resumeService, never()).uploadResume(any(), any(), any(), any());
    }
}
