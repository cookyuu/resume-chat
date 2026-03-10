package com.cookyuu.resume_chat.domain;

import com.cookyuu.resume_chat.common.domain.BaseTimeEntity;
import com.cookyuu.resume_chat.common.enums.ApplicantStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "rc_applicant")
public class Applicant extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private ApplicantStatus status;

    private int loginFailCnt;

    public static Applicant createNewApplicant(String email, String name, String encodedPassword) {
        return Applicant.builder()
                .uuid(UUID.randomUUID())
                .email(email)
                .name(name)
                .password(encodedPassword)
                .status(ApplicantStatus.ACTIVE)
                .loginFailCnt(0)
                .build();
    }

    public void incrementLoginFailCount() {
        this.loginFailCnt++;
    }
    public void resetLoginFailCount() {
        this.loginFailCnt = 0;
    }
    public void lockAccount() {
        this.status = ApplicantStatus.INACTIVE;
    }
    public boolean isAccountLocked() {
        return this.status == ApplicantStatus.INACTIVE;
    }
    public void loginSuccess() {
        resetLoginFailCount();
    }
    public void loginFailed() {
        incrementLoginFailCount();
        if (this.loginFailCnt >= 5) {
            lockAccount();
        }
    }
}
