package com.cookyuu.resume_chat.domain;

import com.cookyuu.resume_chat.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "rc_blocked_email")
public class BlockedEmail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 500)
    private String reason;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    public static BlockedEmail createBlocked(String email, String reason) {
        return BlockedEmail.builder()
                .email(email)
                .reason(reason)
                .blockedAt(LocalDateTime.now())
                .build();
    }
}
