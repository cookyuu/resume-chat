package com.cookyuu.resume_chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 *
 * <p>이메일 발송 등의 비동기 작업을 위한 Thread Pool 설정</p>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 이메일 발송용 비동기 Executor
     *
     * <h3>Thread Pool 설정</h3>
     * <ul>
     *   <li>Core Pool Size: 2 (기본 스레드 수)</li>
     *   <li>Max Pool Size: 5 (최대 스레드 수)</li>
     *   <li>Queue Capacity: 100 (대기 큐 크기)</li>
     * </ul>
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Email-");
        executor.setRejectedExecutionHandler((r, exec) -> {
            log.warn("이메일 발송 작업 거부됨 - 큐가 가득 참");
        });
        executor.initialize();
        return executor;
    }
}
