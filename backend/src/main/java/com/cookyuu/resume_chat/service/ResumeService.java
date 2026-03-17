package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.config.AppProperties;
import com.cookyuu.resume_chat.dto.ResumeDto;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.Resume;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ApplicantRepository applicantRepository;
    private final FileStorageService fileStorageService;
    private final AppProperties appProperties;

    @Transactional
    public ResumeDto.UploadResponse uploadResume(UUID applicantUuid, String title, String description,
                                                   MultipartFile file) {
        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        String storedFileName = fileStorageService.storeFile(file);
        String originalFileName = file.getOriginalFilename();

        Resume resume = Resume.createNewResume(applicant, title, description, storedFileName, originalFileName);
        resumeRepository.save(resume);

        log.info("이력서 업로드 완료: applicantUuid={}, resumeSlug={}, fileName={}",
                applicantUuid, resume.getResumeSlug(), originalFileName);

        return ResumeDto.UploadResponse.from(resume, appProperties.getFrontendUrl());
    }

    public List<ResumeDto.ResumeInfo> getApplicantResumes(UUID applicantUuid) {
        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        List<Resume> resumes = resumeRepository.findByApplicant(applicant);

        String frontendUrl = appProperties.getFrontendUrl();
        return resumes.stream()
                .map(resume -> ResumeDto.ResumeInfo.from(resume, frontendUrl))
                .collect(Collectors.toList());
    }

    public Resume getResumeBySlug(UUID resumeSlug) {
        return resumeRepository.findByResumeSlug(resumeSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    }

    @Transactional
    public void deleteResume(UUID applicantUuid, UUID resumeSlug) {
        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        Resume resume = resumeRepository.findByResumeSlug(resumeSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        if (!resume.getApplicant().getId().equals(applicant.getId())) {
            throw new BusinessException(ErrorCode.RESUME_ACCESS_DENIED);
        }

        resumeRepository.delete(resume);

        log.info("이력서 삭제 완료: applicantUuid={}, resumeSlug={}", applicantUuid, resumeSlug);
    }

    /**
     * 이력서 PDF 파일 조회
     *
     * @param applicantUuid 지원자 UUID
     * @param resumeSlug 이력서 슬러그
     * @return PDF 파일 Resource
     * @throws BusinessException 파일을 찾을 수 없거나 권한이 없는 경우
     */
    public Resource getResumeFile(UUID applicantUuid, UUID resumeSlug) {
        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        Resume resume = resumeRepository.findByResumeSlug(resumeSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        if (!resume.getApplicant().getId().equals(applicant.getId())) {
            throw new BusinessException(ErrorCode.RESUME_ACCESS_DENIED);
        }

        try {
            Path filePath = fileStorageService.getFilePath(resume.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("이력서 파일 조회 성공: resumeSlug={}, fileName={}", resumeSlug, resume.getFilePath());
                return resource;
            } else {
                log.error("이력서 파일을 읽을 수 없음: resumeSlug={}, filePath={}", resumeSlug, filePath);
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            log.error("잘못된 파일 경로: resumeSlug={}, error={}", resumeSlug, e.getMessage());
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }
}
