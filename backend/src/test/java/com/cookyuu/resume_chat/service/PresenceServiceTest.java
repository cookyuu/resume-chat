package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.common.enums.SenderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenceService 테스트")
class PresenceServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PresenceService presenceService;

    private String webSocketSessionId1;
    private String webSocketSessionId2;
    private String chatSessionToken;
    private String applicantUuid;
    private String recruiterSessionToken;

    @BeforeEach
    void setUp() {
        webSocketSessionId1 = "ws-session-1";
        webSocketSessionId2 = "ws-session-2";
        chatSessionToken = UUID.randomUUID().toString();
        applicantUuid = UUID.randomUUID().toString();
        recruiterSessionToken = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("userConnected_지원자_접속_성공")
    void userConnected_applicant_success() {
        // When
        presenceService.userConnected(
                webSocketSessionId1,
                chatSessionToken,
                applicantUuid,
                SenderType.APPLICANT,
                "지원자"
        );

        // Then
        Set<PresenceService.UserPresence> connectedUsers = presenceService.getConnectedUsers(chatSessionToken);
        assertThat(connectedUsers).hasSize(1);
        assertThat(connectedUsers).extracting("userIdentifier").contains(applicantUuid);
        assertThat(connectedUsers).extracting("senderType").contains(SenderType.APPLICANT);

        // 브로드캐스트 확인
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/session/" + chatSessionToken + "/presence"),
                any(PresenceService.PresenceUpdate.class)
        );
    }

    @Test
    @DisplayName("userConnected_채용담당자_접속_성공")
    void userConnected_recruiter_success() {
        // When
        presenceService.userConnected(
                webSocketSessionId1,
                chatSessionToken,
                recruiterSessionToken,
                SenderType.RECRUITER,
                "채용담당자"
        );

        // Then
        Set<PresenceService.UserPresence> connectedUsers = presenceService.getConnectedUsers(chatSessionToken);
        assertThat(connectedUsers).hasSize(1);
        assertThat(connectedUsers).extracting("userIdentifier").contains(recruiterSessionToken);
        assertThat(connectedUsers).extracting("senderType").contains(SenderType.RECRUITER);
    }

    @Test
    @DisplayName("userConnected_여러사용자_접속_목록관리")
    void userConnected_multipleUsers_managedCorrectly() {
        // When
        presenceService.userConnected(
                webSocketSessionId1,
                chatSessionToken,
                applicantUuid,
                SenderType.APPLICANT,
                "지원자"
        );

        presenceService.userConnected(
                webSocketSessionId2,
                chatSessionToken,
                recruiterSessionToken,
                SenderType.RECRUITER,
                "채용담당자"
        );

        // Then
        Set<PresenceService.UserPresence> connectedUsers = presenceService.getConnectedUsers(chatSessionToken);
        assertThat(connectedUsers).hasSize(2);
    }

    @Test
    @DisplayName("userDisconnected_접속해제_성공")
    void userDisconnected_success() {
        // Given
        presenceService.userConnected(
                webSocketSessionId1,
                chatSessionToken,
                applicantUuid,
                SenderType.APPLICANT,
                "지원자"
        );

        // When
        presenceService.userDisconnected(webSocketSessionId1);

        // Then
        Set<PresenceService.UserPresence> connectedUsers = presenceService.getConnectedUsers(chatSessionToken);
        assertThat(connectedUsers).isEmpty();

        // 브로드캐스트 확인 (접속 1회 + 해제 1회)
        verify(messagingTemplate, times(2)).convertAndSend(
                eq("/topic/session/" + chatSessionToken + "/presence"),
                any(PresenceService.PresenceUpdate.class)
        );
    }

    @Test
    @DisplayName("isUserConnected_접속중인_사용자_true_반환")
    void isUserConnected_connectedUser_returnsTrue() {
        // Given
        presenceService.userConnected(
                webSocketSessionId1,
                chatSessionToken,
                applicantUuid,
                SenderType.APPLICANT,
                "지원자"
        );

        // When
        boolean result = presenceService.isUserConnected(chatSessionToken, applicantUuid);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isUserConnected_접속하지않은_사용자_false_반환")
    void isUserConnected_disconnectedUser_returnsFalse() {
        // When
        boolean result = presenceService.isUserConnected(chatSessionToken, applicantUuid);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getConnectedUsers_빈목록_반환")
    void getConnectedUsers_emptyList_returnsEmpty() {
        // When
        Set<PresenceService.UserPresence> connectedUsers = presenceService.getConnectedUsers(chatSessionToken);

        // Then
        assertThat(connectedUsers).isEmpty();
    }

    @Test
    @DisplayName("userDisconnected_존재하지않는_세션_안전하게처리")
    void userDisconnected_nonExistingSession_handledSafely() {
        // When & Then (예외가 발생하지 않아야 함)
        presenceService.userDisconnected("non-existing-session");
    }
}
