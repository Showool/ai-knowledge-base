package com.jason.ai.knowledgebase.service.sse;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.config.ChatProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

/** 执行请求绝对生命周期上限。 */
@Component
@RequiredArgsConstructor
public class SseConnectionJanitor {

    private final SseConnectionRegistry registry;
    private final ChatLifecycleService lifecycle;
    private final ChatProperties properties;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("chat-janitor-").factory());

    @PostConstruct
    void start() {
        long interval = properties.getSse().getJanitorInterval().toMillis();
        executor.scheduleWithFixedDelay(this::sweep, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void sweep() {
        Instant now = Instant.now();
        Duration maximum = properties.getSse().getMaxLifetime();
        for (SseConnection connection : registry.all()) {
            if (!connection.state().terminal() && Duration.between(connection.createdAt(), now).compareTo(maximum) > 0) {
                lifecycle.maxLifetimeExceeded(connection);
            }
        }
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }
}
