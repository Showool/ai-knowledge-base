package com.jason.ai.knowledgebase.config;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/** 拒绝无法安全完成的容量与超时配置组合。 */
@Component
@RequiredArgsConstructor
public class SseConfigurationValidator {

    private final ChatProperties properties;

    @PostConstruct
    void validate() {
        ChatProperties.Sse sse = properties.getSse();
        if (sse.getLlmConcurrency() > sse.getMaxActiveConnections()) {
            throw new IllegalStateException("llm-concurrency 不能超过 max-active-connections");
        }
        if (sse.getQueueTimeout().plus(sse.getGenerationTimeout()).compareTo(sse.getMaxLifetime()) >= 0) {
            throw new IllegalStateException("queue-timeout 与 generation-timeout 之和必须小于 max-lifetime");
        }
        if (sse.getUserLockTtl().compareTo(sse.getMaxLifetime().plus(sse.getJanitorInterval())) <= 0) {
            throw new IllegalStateException("user-lock-ttl 必须大于 max-lifetime 与 janitor-interval 之和");
        }
    }
}
