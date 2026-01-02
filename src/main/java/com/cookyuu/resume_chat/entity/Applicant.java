package com.cookyuu.resume_chat.entity;

import com.cookyuu.resume_chat.common.entity.BaseTimeEntity;
import com.cookyuu.resume_chat.common.enums.ApplicantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
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
}
