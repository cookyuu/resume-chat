package com.cookyuu.resume_chat.entity;

import com.cookyuu.resume_chat.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "rc_chat_session",
    uniqueConstraints = @UniqueConstraint(columnNames = {"resume_id", "recruiter_email"}),
    indexes = @Index(name = "idx_session_token", columnList = "session_token")
)
public class ChatSession extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;

    @Column(nullable = false)
    private String recruiterName;

    @Column(nullable = false)
    private String recruiterEmail;

    @Column(nullable = false)
    private String recruiterCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    private boolean active;
}
