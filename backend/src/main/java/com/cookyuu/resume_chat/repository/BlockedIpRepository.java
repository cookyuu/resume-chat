package com.cookyuu.resume_chat.repository;

import com.cookyuu.resume_chat.domain.BlockedIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockedIpRepository extends JpaRepository<BlockedIp, Long> {

    /**
     * IP 주소로 차단 정보 조회
     */
    Optional<BlockedIp> findByIpAddress(String ipAddress);

    /**
     * IP 주소가 차단되어 있는지 확인
     */
    boolean existsByIpAddress(String ipAddress);
}
