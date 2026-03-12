package com.cookyuu.resume_chat.integration;

import com.cookyuu.resume_chat.common.enums.MessageType;
import com.cookyuu.resume_chat.common.enums.SenderType;
import com.cookyuu.resume_chat.dto.ChatDto;
import com.cookyuu.resume_chat.domain.Applicant;
import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.domain.Resume;
import com.cookyuu.resume_chat.repository.ApplicantRepository;
import com.cookyuu.resume_chat.repository.ChatMessageRepository;
import com.cookyuu.resume_chat.repository.ChatSessionRepository;
import com.cookyuu.resume_chat.repository.ResumeRepository;
import com.cookyuu.resume_chat.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket 통합 테스트
 *
 * 실제 WebSocket 연결을 통해 여러 클라이언트의 동시 접속 및 재연결 시나리오를 검증합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DisplayName("WebSocket 통합 테스트")
class WebSocketIntegrationTest {

    private static final String WEBSOCKET_URL = "ws://localhost:8080/ws";
    private static final int PORT = 8080;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private Applicant testApplicant;
    private Resume testResume;
    private ChatSession testSession;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // WebSocket STOMP 클라이언트 설정
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);

        // Jackson ObjectMapper에 JavaTimeModule 추가
        ObjectMapper objectMapperWithJavaTime = new ObjectMapper();
        objectMapperWithJavaTime.registerModule(new JavaTimeModule());

        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(objectMapperWithJavaTime);

        stompClient.setMessageConverter(messageConverter);

        // 테스트 데이터 생성
        testApplicant = applicantRepository.save(Applicant.createNewApplicant(
                "websocket-test@example.com",
                "웹소켓테스터",
                passwordEncoder.encode("password123")
        ));

        testResume = Resume.createNewResume(
                testApplicant,
                "웹소켓 테스트 이력서",
                "실시간 채팅 기능 테스트",
                "/uploads/test-resume.pdf",
                "test-resume.pdf"
        );
        resumeRepository.save(testResume);

        testSession = ChatSession.createNewSession(
                testResume,
                "채용담당자",
                "recruiter@test.com",
                "테스트회사"
        );
        chatSessionRepository.save(testSession);

        // JWT 토큰 생성 (지원자용)
        accessToken = jwtTokenProvider.generateAccessToken(testApplicant.getUuid(), testApplicant.getEmail());
    }

    @AfterEach
    void tearDown() {
        chatMessageRepository.deleteAll();
        chatSessionRepository.deleteAll();
        resumeRepository.deleteAll();
        applicantRepository.deleteAll();
    }

    @Test
    @DisplayName("여러 클라이언트 동시 접속 및 브로드캐스트 테스트")
    void multipleClientsSimultaneousConnection_broadcastsToAll() throws Exception {
        // Given - 3개의 클라이언트 연결 준비
        int clientCount = 3;
        CountDownLatch connectLatch = new CountDownLatch(clientCount);
        CountDownLatch messageLatch = new CountDownLatch(clientCount); // 모든 클라이언트가 메시지를 받을 때까지 대기

        List<StompSession> sessions = new CopyOnWriteArrayList<>();
        List<ChatDto.WebSocketChatMessage> receivedMessages = new CopyOnWriteArrayList<>();

        // When - 3개의 클라이언트 동시 연결 (모두 채용담당자)
        for (int i = 0; i < clientCount; i++) {
            final int clientIndex = i;

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            // 모든 클라이언트는 채용담당자 역할 (인증 불필요)

            StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    sessions.add(session);

                    // 세션 구독
                    session.subscribe("/topic/session/" + testSession.getSessionToken(), new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return ChatDto.WebSocketChatMessage.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            ChatDto.WebSocketChatMessage message =
                                    (ChatDto.WebSocketChatMessage) payload;
                            receivedMessages.add(message);
                            messageLatch.countDown();
                        }
                    });

                    connectLatch.countDown();
                }

                @Override
                public void handleException(StompSession session, StompCommand command,
                                           StompHeaders headers, byte[] payload, Throwable exception) {
                    exception.printStackTrace();
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    exception.printStackTrace();
                }
            };

            stompClient.connectAsync(WEBSOCKET_URL, headers, sessionHandler);
        }

        // 모든 클라이언트 연결 대기 (최대 5초)
        boolean allConnected = connectLatch.await(5, TimeUnit.SECONDS);
        assertThat(allConnected).isTrue();
        assertThat(sessions).hasSize(clientCount);

        // 첫 번째 클라이언트(채용담당자)가 메시지 전송
        StompSession recruiterSession = sessions.get(0);
        ChatDto.WebSocketChatMessage sendMessage = ChatDto.WebSocketChatMessage.builder()
                .sessionToken(testSession.getSessionToken())
                .senderType(SenderType.RECRUITER)
                .messageType(MessageType.TEXT)
                .content("여러 클라이언트 브로드캐스트 테스트 메시지")
                .sentAt(LocalDateTime.now())
                .build();

        recruiterSession.send("/app/chat/" + testSession.getSessionToken(), sendMessage);

        // 모든 클라이언트가 메시지를 받을 때까지 대기 (최대 5초)
        boolean allReceived = messageLatch.await(5, TimeUnit.SECONDS);

        // Then
        assertThat(allReceived).isTrue();
        assertThat(receivedMessages).hasSize(clientCount); // 3개의 클라이언트 모두 메시지 수신

        // 모든 클라이언트가 동일한 메시지를 받았는지 확인
        for (ChatDto.WebSocketChatMessage received : receivedMessages) {
            assertThat(received.getContent()).isEqualTo("여러 클라이언트 브로드캐스트 테스트 메시지");
            assertThat(received.getSenderType()).isEqualTo(SenderType.RECRUITER);
            assertThat(received.getSessionToken()).isEqualTo(testSession.getSessionToken());
            assertThat(received.getMessageId()).isNotNull();
        }

        // 데이터베이스에 메시지가 저장되었는지 확인
        List<ChatMessage> savedMessages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(testSession);
        assertThat(savedMessages).hasSize(1);
        assertThat(savedMessages.get(0).getContent()).isEqualTo("여러 클라이언트 브로드캐스트 테스트 메시지");

        // 각 클라이언트의 독립적인 세션 관리 확인
        for (StompSession session : sessions) {
            assertThat(session.isConnected()).isTrue();
        }

        // 세션 정리
        for (StompSession session : sessions) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Test
    @DisplayName("클라이언트 재연결 시나리오 테스트")
    void clientReconnection_maintainsConsistency() throws Exception {
        // Given - 초기 연결
        CountDownLatch connectLatch1 = new CountDownLatch(1);
        CountDownLatch messageLatch1 = new CountDownLatch(1);
        CountDownLatch connectLatch2 = new CountDownLatch(1);
        CountDownLatch messageLatch2 = new CountDownLatch(1);

        List<ChatDto.WebSocketChatMessage> receivedMessages = new CopyOnWriteArrayList<>();
        StompSession[] sessionHolder = new StompSession[1];

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        // 첫 번째 연결
        StompSessionHandler firstConnectionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                sessionHolder[0] = session;

                // 세션 구독
                session.subscribe("/topic/session/" + testSession.getSessionToken(), new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return ChatDto.WebSocketChatMessage.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        ChatDto.WebSocketChatMessage message =
                                (ChatDto.WebSocketChatMessage) payload;
                        receivedMessages.add(message);
                        messageLatch1.countDown();
                    }
                });

                connectLatch1.countDown();
            }
        };

        stompClient.connectAsync(WEBSOCKET_URL, headers, firstConnectionHandler);

        // 연결 대기
        boolean connected1 = connectLatch1.await(5, TimeUnit.SECONDS);
        assertThat(connected1).isTrue();

        // When - 첫 번째 메시지 전송
        ChatDto.WebSocketChatMessage message1 = ChatDto.WebSocketChatMessage.builder()
                .sessionToken(testSession.getSessionToken())
                .senderType(SenderType.APPLICANT)
                .messageType(MessageType.TEXT)
                .content("재연결 전 메시지")
                .sentAt(LocalDateTime.now())
                .build();

        sessionHolder[0].send("/app/chat/" + testSession.getSessionToken(), message1);

        // 메시지 수신 대기
        boolean received1 = messageLatch1.await(5, TimeUnit.SECONDS);
        assertThat(received1).isTrue();

        // 연결 끊기
        if (sessionHolder[0] != null && sessionHolder[0].isConnected()) {
            sessionHolder[0].disconnect();
        }

        // 연결이 끊어질 때까지 잠시 대기
        Thread.sleep(1000);

        // Then - 재연결
        StompSessionHandler secondConnectionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                sessionHolder[0] = session;

                // 세션 구독
                session.subscribe("/topic/session/" + testSession.getSessionToken(), new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return ChatDto.WebSocketChatMessage.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        ChatDto.WebSocketChatMessage message =
                                (ChatDto.WebSocketChatMessage) payload;
                        receivedMessages.add(message);
                        messageLatch2.countDown();
                    }
                });

                connectLatch2.countDown();
            }
        };

        stompClient.connectAsync(WEBSOCKET_URL, headers, secondConnectionHandler);

        // 재연결 대기
        boolean connected2 = connectLatch2.await(5, TimeUnit.SECONDS);
        assertThat(connected2).isTrue();
        assertThat(sessionHolder[0].isConnected()).isTrue();

        // 재연결 후 메시지 전송
        ChatDto.WebSocketChatMessage message2 = ChatDto.WebSocketChatMessage.builder()
                .sessionToken(testSession.getSessionToken())
                .senderType(SenderType.APPLICANT)
                .messageType(MessageType.TEXT)
                .content("재연결 후 메시지")
                .sentAt(LocalDateTime.now())
                .build();

        sessionHolder[0].send("/app/chat/" + testSession.getSessionToken(), message2);

        // 메시지 수신 대기
        boolean received2 = messageLatch2.await(5, TimeUnit.SECONDS);
        assertThat(received2).isTrue();

        // 검증
        assertThat(receivedMessages).hasSize(2);
        assertThat(receivedMessages.get(0).getContent()).isEqualTo("재연결 전 메시지");
        assertThat(receivedMessages.get(1).getContent()).isEqualTo("재연결 후 메시지");

        // 재연결 전후 세션 일관성 확인 - 데이터베이스에 두 메시지 모두 저장됨
        List<ChatMessage> savedMessages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(testSession);
        assertThat(savedMessages).hasSize(2);
        assertThat(savedMessages.get(0).getContent()).isEqualTo("재연결 전 메시지");
        assertThat(savedMessages.get(1).getContent()).isEqualTo("재연결 후 메시지");
        assertThat(savedMessages.get(0).getSenderType()).isEqualTo(SenderType.APPLICANT);
        assertThat(savedMessages.get(1).getSenderType()).isEqualTo(SenderType.APPLICANT);

        // 세션 메시지 카운트 확인
        ChatSession updatedSession = chatSessionRepository.findById(testSession.getId()).orElseThrow();
        assertThat(updatedSession.getTotalMessages()).isEqualTo(2);

        // 세션 정리
        if (sessionHolder[0] != null && sessionHolder[0].isConnected()) {
            sessionHolder[0].disconnect();
        }
    }
}
