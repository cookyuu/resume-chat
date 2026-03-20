package com.cookyuu.resume_chat.common.enums;

/**
 * 채팅 메시지 타입
 *
 * <p>채팅 메시지의 콘텐츠 타입을 정의합니다.</p>
 *
 * <ul>
 *   <li>TEXT: 텍스트 메시지 (현재 지원)</li>
 *   <li>IMAGE: 이미지 파일 (향후 확장 예정)</li>
 *   <li>FILE: 파일 첨부 (향후 확장 예정)</li>
 *   <li>SYSTEM: 시스템 메시지 (예: "사용자가 입장했습니다") (향후 확장 예정)</li>
 * </ul>
 */
public enum MessageType {
    /**
     * 텍스트 메시지
     */
    TEXT,

    /**
     * 이미지 파일 (향후 확장 예정)
     */
    IMAGE,

    /**
     * 파일 첨부 (향후 확장 예정)
     */
    FILE,

    /**
     * 시스템 메시지 (향후 확장 예정)
     */
    SYSTEM
}
