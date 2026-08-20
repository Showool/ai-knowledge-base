package com.jason.ai.knowledgebase.service.sse;

import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

/** 服务优雅停机时终结本地活动请求。 */
@Component
@RequiredArgsConstructor
public class SseShutdownCoordinator {

    private final SseConnectionRegistry registry;
    private final ChatLifecycleService lifecycle;

    @PreDestroy
    void shutdown() {
        registry.all().stream().filter(connection -> !connection.state().terminal()).forEach(lifecycle::shutdown);
    }
}
