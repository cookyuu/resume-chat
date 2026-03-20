package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.command.ApplicantCommand;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.dto.ApplicantDto;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantService {
    private final ApplicantRepository applicantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void joinApplicant(ApplicantCommand.Create command) {
        if (applicantRepository.existsByEmail(command.getEmail())) {
            throw new BusinessException(ErrorCode.APPLICANT_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(command.getPassword());

        Applicant applicant = Applicant.createNewApplicant(
                command.getEmail(),
                command.getName(),
                encodedPassword
        );

        applicantRepository.save(applicant);
        log.info("지원자 회원가입 완료: email={}, uuid={}", applicant.getEmail(), applicant.getUuid());
    }

    @Transactional
    public ApplicantDto.LoginResponse login(ApplicantCommand.Login command) {
        Applicant applicant = applicantRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (applicant.isAccountLocked()) {
            log.warn("계정이 잠겨있습니다: email={}", command.getEmail());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        boolean isMatch = passwordEncoder.matches(command.getPassword(), applicant.getPassword());

        if (!isMatch) {
            applicant.loginFailed();
            applicantRepository.saveAndFlush(applicant);
            log.warn("로그인 실패: email={}, failCount={}", command.getEmail(), applicant.getLoginFailCnt());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        applicant.loginSuccess();
        log.info("로그인 성공: email={}, uuid={}", applicant.getEmail(), applicant.getUuid());

        String accessToken = jwtTokenProvider.generateAccessToken(applicant.getUuid(), applicant.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(applicant.getUuid());

        return new ApplicantDto.LoginResponse(
                applicant.getUuid(),
                applicant.getEmail(),
                applicant.getName(),
                accessToken,
                refreshToken
        );
    }

    public ApplicantDto.ProfileResponse getProfile(UUID uuid) {
        Applicant applicant = applicantRepository.findById(findApplicantIdByUuid(uuid))
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        log.info("프로필 조회: email={}, uuid={}", applicant.getEmail(), applicant.getUuid());
        return ApplicantDto.ProfileResponse.from(applicant);
    }

    public ApplicantDto.RefreshTokenResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh Token이 제공되지 않음");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            log.warn("만료된 Refresh Token");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        UUID applicantUuid = jwtTokenProvider.getApplicantUuidFromToken(refreshToken);
        Applicant applicant = applicantRepository.findByUuid(applicantUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));

        if (applicant.isAccountLocked()) {
            log.warn("잠긴 계정의 토큰 갱신 시도: email={}", applicant.getEmail());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(applicant.getUuid(), applicant.getEmail());
        log.info("Access Token 갱신 성공: email={}, uuid={}", applicant.getEmail(), applicant.getUuid());

        return new ApplicantDto.RefreshTokenResponse(newAccessToken);
    }

    private Long findApplicantIdByUuid(UUID uuid) {
        return applicantRepository.findByUuid(uuid)
                .map(Applicant::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICANT_NOT_FOUND));
    }
}
