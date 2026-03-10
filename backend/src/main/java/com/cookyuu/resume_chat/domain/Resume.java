package com.cookyuu.resume_chat.domain;

import com.cookyuu.resume_chat.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(nullable = false)
    private String filePath;

    private String originalFileName;

    private int viewCnt;

    @Builder.Default
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatSession> chatSessions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.resumeSlug == null) {
            this.resumeSlug = UUID.randomUUID();
        }
        if (this.viewCnt == 0) {
            this.viewCnt = 0;
        }
    }

    public static Resume createNewResume(Applicant applicant, String title, String description,
                                         String filePath, String originalFileName) {
        return Resume.builder()
                .resumeSlug(UUID.randomUUID())
                .applicant(applicant)
                .title(title)
                .description(description)
                .filePath(filePath)
                .originalFileName(originalFileName)
                .viewCnt(0)
                .build();
    }

    public void incrementViewCount() {
        this.viewCnt++;
    }
}
