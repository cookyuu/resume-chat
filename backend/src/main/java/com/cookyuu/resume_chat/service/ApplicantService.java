package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.command.ApplicantCommand;
import com.cookyuu.resume_chat.common.exception.BusinessException;
import com.cookyuu.resume_chat.common.response.ErrorCode;
import com.cookyuu.resume_chat.entity.Applicant;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantService {
    private final ApplicantRepository applicantRepository;
    private final PasswordEncoder passwordEncoder;

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
}
