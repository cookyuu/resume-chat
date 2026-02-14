package com.cookyuu.resume_chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {
    private String uploadDir;
    private long maxFileSize;
    private String allowedExtensions;
}
