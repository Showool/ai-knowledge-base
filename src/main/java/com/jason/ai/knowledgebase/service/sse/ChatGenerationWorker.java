package com.jason.ai.knowledgebase.service.sse;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.config.ChatProperties;

import lombok.RequiredArgsConstructor;
import reactor.core.Disposable;

/**
 * 订阅 Spring AI 增量内容流并转发模型增量。
 */
@Component
@RequiredArgsConstructor
public class ChatGenerationWorker {

    private final ChatClient chatClient;
    private final ChatLifecycleService lifecycle;
    private final ChatProperties properties;

    /**
     * 启动一次模型生成；同步构建或订阅异常也由统一生命周期终结。
     *
     * @param task 已出队任务
     * @param permitRelease LLM 并发许可的一次性释放回调
     */
    public void start(GenerationTask task, Runnable permitRelease) {
        SseConnection connection = task.connection();
        try {
            if (!lifecycle.beginGeneration(connection, permitRelease)) {
                return;
            }
            Disposable subscription = chatClient.prompt()
                    .user(task.message())
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID,
                            String.valueOf(connection.sessionId())))
                    .stream()
                    .content()
                    .timeout(properties.getSse().getGenerationTimeout())
                    .subscribe(delta -> lifecycle.delta(connection, delta),
                            error -> lifecycle.generationFailed(connection, error),
                            () -> lifecycle.completed(connection));
            connection.setSubscription(subscription);
        } catch (RuntimeException exception) {
            lifecycle.generationFailed(connection, exception);
        }
    }
}
