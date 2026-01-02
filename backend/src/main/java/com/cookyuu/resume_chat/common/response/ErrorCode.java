package com.cookyuu.resume_chat.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT("C001", "입력값이 올바르지 않습니다"),
    UNAUTHORIZED("C002", "인증이 필요합니다"),
    FORBIDDEN("C003", "권한이 없습니다"),
    NOT_FOUND("C004", "리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED("C005", "지원하지 않는 HTTP 메서드입니다"),
    JSON_PARSE_ERROR("C006", "요청 데이터 형식이 올바르지 않습니다"),
    MISSING_PARAMETER("C007", "필수 파라미터가 누락되었습니다"),
    TYPE_MISMATCH("C008", "파라미터 타입이 올바르지 않습니다"),
    DATA_INTEGRITY_VIOLATION("C009", "데이터 무결성 제약조건 위반입니다"),
    DUPLICATE_ENTRY("C010", "이미 존재하는 데이터입니다"),
    INTERNAL_SERVER_ERROR("C999", "서버 오류가 발생했습니다"),

    // Applicant
    APPLICANT_NOT_FOUND("A001", "지원자를 찾을 수 없습니다"),
    APPLICANT_ALREADY_EXISTS("A002", "이미 존재하는 이메일입니다"),
    INVALID_PASSWORD("A003", "비밀번호가 일치하지 않습니다"),
    ACCOUNT_LOCKED("A004", "계정이 잠겨있습니다"),

    // Resume
    RESUME_NOT_FOUND("R001", "이력서를 찾을 수 없습니다"),
    RESUME_ACCESS_DENIED("R002", "이력서 접근 권한이 없습니다"),

    // Chat
    SESSION_NOT_FOUND("S001", "채팅 세션을 찾을 수 없습니다"),
    INVALID_SESSION_TOKEN("S002", "유효하지 않은 세션 토큰입니다"),
    SESSION_EXPIRED("S003", "세션이 만료되었습니다");

    private final String code;
    private final String message;
}