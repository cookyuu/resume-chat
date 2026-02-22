package com.cookyuu.resume_chat.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClientType {
    WEB("web"),
    APP("app");

    private final String value;

    public static ClientType from(String value) {
        if (value == null) {
            return WEB; // 기본값
        }

        for (ClientType type : ClientType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return WEB; // 기본값
    }
}
