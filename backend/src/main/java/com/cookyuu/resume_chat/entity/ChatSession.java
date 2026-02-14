package com.cookyuu.resume_chat.entity;

import com.cookyuu.resume_chat.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    private long totalMessages;

    private LocalDateTime lastMessageAt;

    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.sessionToken == null) {
            this.sessionToken = UUID.randomUUID().toString();
        }
        if (!this.active) {
            this.active = true;
        }
        if (this.totalMessages == 0) {
            this.totalMessages = 0;
        }
    }

    public static ChatSession createNewSession(Resume resume, String recruiterName,
                                                String recruiterEmail, String recruiterCompany) {
        return ChatSession.builder()
                .sessionToken(UUID.randomUUID().toString())
                .recruiterName(recruiterName)
                .recruiterEmail(recruiterEmail)
                .recruiterCompany(recruiterCompany)
                .resume(resume)
                .active(true)
                .totalMessages(0)
                .build();
    }

    public void incrementMessageCount() {
        this.totalMessages++;
        this.lastMessageAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
    }
}
