package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findByResumeSlug(UUID resumeSlug);
    List<Resume> findByApplicant(Applicant applicant);
}
