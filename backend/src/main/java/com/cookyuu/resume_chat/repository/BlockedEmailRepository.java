package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.domain.BlockedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockedEmailRepository extends JpaRepository<BlockedEmail, Long> {

    /**
     * 이메일로 차단 정보 조회
     */
    Optional<BlockedEmail> findByEmail(String email);

    /**
     * 이메일이 차단되어 있는지 확인
     */
    boolean existsByEmail(String email);
}
