package com.jason.ai.knowledgebase.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 对话输入和 SSE 资源配置。 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {
    @Valid
    private Sse sse = new Sse();

    /** SSE 队列、并发、超时和连接生命周期配置。 */
    @Getter
    @Setter
    public static class Sse {
        @Min(1)
        private int maxActiveConnections = 1000;
        @Min(1)
        private int maxQueuedRequests = 1000;
        @Min(1)
        private int llmConcurrency = 100;
        @NotNull
        private Duration queueTimeout = Duration.ofSeconds(30);
        @NotNull
        private Duration generationTimeout = Duration.ofSeconds(85);
        @NotNull
        private Duration heartbeatInterval = Duration.ofSeconds(15);
        @NotNull
        private Duration maxLifetime = Duration.ofSeconds(140);
        @NotNull
        private Duration userLockTtl = Duration.ofSeconds(180);
        @Min(8)
        private int sinkBufferSize = 64;
        @NotNull
        private Duration terminalRetention = Duration.ofSeconds(10);
        @NotNull
        private Duration janitorInterval = Duration.ofSeconds(5);
    }
}