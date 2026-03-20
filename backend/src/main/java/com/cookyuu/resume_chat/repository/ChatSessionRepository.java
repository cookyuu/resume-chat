package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findBySessionToken(String sessionToken);
    List<ChatSession> findByResume(Resume resume);
    Optional<ChatSession> findByResumeAndRecruiterEmail(Resume resume, String recruiterEmail);
}
