package com.cookyuu.resume_chat.entity;

import com.cookyuu.resume_chat.common.enums.ApplicantLoginFailReason;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "rc_applicant_login_history")
public class ApplicantLoginHstr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private boolean success;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicantLoginFailReason failReason;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    @Column(updatable = false, nullable = false)
    private LocalDateTime loginAt;
}
