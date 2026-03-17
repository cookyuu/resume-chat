package com.cookyuu.resume_chat.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT("C001", "입력값이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("C002", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("C003", "권한이 없습니다", HttpStatus.FORBIDDEN),
    NOT_FOUND("C004", "리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("C005", "지원하지 않는 HTTP 메서드입니다", HttpStatus.METHOD_NOT_ALLOWED),
    JSON_PARSE_ERROR("C006", "요청 데이터 형식이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER("C007", "필수 파라미터가 누락되었습니다", HttpStatus.BAD_REQUEST),
    TYPE_MISMATCH("C008", "파라미터 타입이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    DATA_INTEGRITY_VIOLATION("C009", "데이터 무결성 제약조건 위반입니다", HttpStatus.CONFLICT),
    DUPLICATE_ENTRY("C010", "이미 존재하는 데이터입니다", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR("C999", "서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),

    // Applicant
    APPLICANT_NOT_FOUND("A001", "지원자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    APPLICANT_ALREADY_EXISTS("A002", "이미 존재하는 이메일입니다", HttpStatus.CONFLICT),
    INVALID_PASSWORD("A003", "비밀번호가 일치하지 않습니다", HttpStatus.BAD_REQUEST),
    ACCOUNT_LOCKED("A004", "계정이 잠겨있습니다", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("A005", "이메일 또는 비밀번호가 일치하지 않습니다", HttpStatus.UNAUTHORIZED),

    // Resume
    RESUME_NOT_FOUND("R001", "이력서를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    RESUME_ACCESS_DENIED("R002", "이력서 접근 권한이 없습니다", HttpStatus.FORBIDDEN),

    // Chat
    SESSION_NOT_FOUND("S001", "채팅 세션을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    INVALID_SESSION_TOKEN("S002", "유효하지 않은 세션 토큰입니다", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED("S003", "세션이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    SESSION_ACCESS_DENIED("S004", "채팅 세션 접근 권한이 없습니다", HttpStatus.FORBIDDEN),
    WEBSOCKET_AUTH_REQUIRED("S005", "WebSocket 연결에 인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    INVALID_MESSAGE_TYPE("S006", "지원하지 않는 메시지 타입입니다", HttpStatus.BAD_REQUEST),

    // File
    INVALID_FILE("F001", "유효하지 않은 파일입니다", HttpStatus.BAD_REQUEST),
    INVALID_FILE_NAME("F002", "유효하지 않은 파일명입니다", HttpStatus.BAD_REQUEST),
    INVALID_FILE_EXTENSION("F003", "허용되지 않은 파일 확장자입니다", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("F004", "파일 크기가 제한을 초과했습니다", HttpStatus.BAD_REQUEST),
    FILE_STORAGE_ERROR("F005", "파일 저장 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND("F006", "파일을 찾을 수 없습니다", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}