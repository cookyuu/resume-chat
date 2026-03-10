package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.domain.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

    boolean existsByEmail(String email);
    Optional<Applicant> findByEmail(String email);
    Optional<Applicant> findByUuid(UUID uuid);
}
