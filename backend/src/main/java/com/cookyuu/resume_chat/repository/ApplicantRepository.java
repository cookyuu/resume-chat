package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

    boolean existsByEmail(String email);
}
