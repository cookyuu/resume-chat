package com.cookyuu.resume_chat.entity;

import com.cookyuu.resume_chat.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "rc_resume")
public class Resume extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID resumeSlug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @Column(nullable = false)
    private String title;

    private String description;

    private int viewCnt;
}
