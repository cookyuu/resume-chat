package com.cookyuu.resume_chat.service;

import com.cookyuu.resume_chat.domain.ChatMessage;
import com.cookyuu.resume_chat.domain.ChatSession;
import com.cookyuu.resume_chat.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 채팅 히스토리 내보내기 서비스
 *
 * <p>채팅 메시지를 다양한 형식(TEXT, JSON)으로 내보냅니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatExportService {

    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * 채팅 히스토리를 TEXT 형식으로 내보내기
     *
     * @param session 채팅 세션
     * @return TEXT 형식의 채팅 내용 (바이트 배열)
     */
    @Async
    public byte[] exportToText(ChatSession session) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(50)).append("\n");
        sb.append("Resume Chat - 채팅 히스토리\n");
        sb.append("=".repeat(50)).append("\n\n");

        sb.append("세션 정보:\n");
        sb.append("  회사: ").append(session.getRecruiterCompany()).append("\n");
        sb.append("  채용담당자: ").append(session.getRecruiterName())
                .append(" (").append(session.getRecruiterEmail()).append(")\n");
        sb.append("  이력서: ").append(session.getResume().getTitle()).append("\n");
        sb.append("  생성일: ").append(session.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        sb.append("=".repeat(50)).append("\n");
        sb.append("메시지 내역 (총 ").append(messages.size()).append("건)\n");
        sb.append("=".repeat(50)).append("\n\n");

        for (ChatMessage message : messages) {
            String sender = message.getSenderType().name().equals("APPLICANT") ? "지원자" : "채용담당자";
            String time = message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            sb.append("[").append(sender).append("] ").append(time).append("\n");
            sb.append(message.getContent()).append("\n\n");
        }

        sb.append("=".repeat(50)).append("\n");
        sb.append("End of Chat History\n");
        sb.append("=".repeat(50)).append("\n");

        log.info("채팅 히스토리 TEXT 내보내기 완료: sessionToken={}, messages={}",
                session.getSessionToken(), messages.size());

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 채팅 히스토리를 JSON 형식으로 내보내기
     *
     * @param session 채팅 세션
     * @return JSON 형식의 채팅 내용 (바이트 배열)
     */
    @Async
    public byte[] exportToJson(ChatSession session) {
        try {
            List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);

            Map<String, Object> exportData = new HashMap<>();

            // 세션 정보
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionToken", session.getSessionToken());
            sessionInfo.put("recruiterCompany", session.getRecruiterCompany());
            sessionInfo.put("recruiterName", session.getRecruiterName());
            sessionInfo.put("recruiterEmail", session.getRecruiterEmail());
            sessionInfo.put("resumeTitle", session.getResume().getTitle());
            sessionInfo.put("createdAt", session.getCreatedAt());
            exportData.put("session", sessionInfo);

            // 메시지 목록
            List<Map<String, Object>> messageList = messages.stream()
                    .map(msg -> {
                        Map<String, Object> msgMap = new HashMap<>();
                        msgMap.put("messageId", msg.getMessageId());
                        msgMap.put("senderType", msg.getSenderType());
                        msgMap.put("messageType", msg.getMessageType());
                        msgMap.put("content", msg.getContent());
                        msgMap.put("readStatus", msg.isReadStatus());
                        msgMap.put("createdAt", msg.getCreatedAt());
                        return msgMap;
                    })
                    .collect(Collectors.toList());

            exportData.put("messages", messageList);
            exportData.put("totalMessages", messages.size());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, exportData);

            log.info("채팅 히스토리 JSON 내보내기 완료: sessionToken={}, messages={}",
                    session.getSessionToken(), messages.size());

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("채팅 히스토리 JSON 내보내기 실패: sessionToken={}, error={}",
                    session.getSessionToken(), e.getMessage(), e);
            throw new RuntimeException("JSON 내보내기 실패", e);
        }
    }
}
