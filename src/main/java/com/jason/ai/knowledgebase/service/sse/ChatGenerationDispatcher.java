package com.jason.ai.knowledgebase.service.sse;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.model.enums.ChatRequestState;
import com.jason.ai.knowledgebase.config.ChatProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 保持 FIFO 启动顺序并限制活动模型订阅数量。
 */
@Slf4j
@Component
public class ChatGenerationDispatcher {

    private final ChatGenerationQueue queue;
    private final ChatGenerationWorker worker;
    private final Semaphore llmPermits;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread thread;

    /**
     * 创建 FIFO 调度器。
     */
    public ChatGenerationDispatcher(ChatGenerationQueue queue, ChatGenerationWorker worker,
            ChatProperties properties) {
        this.queue = queue;
        this.worker = worker;
        this.llmPermits = new Semaphore(properties.getSse().getLlmConcurrency(), true);
    }

    @PostConstruct
    void start() {
        thread = Thread.ofPlatform().name("chat-fifo-dispatcher").start(this::dispatchLoop);
    }

    /**
     * 将每个许可包装为幂等回调，避免异常路径和生命周期重复释放。
     */
    private void dispatchLoop() {
        while (running.get()) {
            Runnable pendingPermitRelease = null;
            try {
                llmPermits.acquire();
                AtomicBoolean released = new AtomicBoolean();
                pendingPermitRelease = () -> {
                    if (released.compareAndSet(false, true)) {
                        llmPermits.release();
                    }
                };

                GenerationTask task = queue.take();
                if (task.connection().state() != ChatRequestState.QUEUED) {
                    pendingPermitRelease.run();
                    pendingPermitRelease = null;
                    continue;
                }

                Runnable transferredPermitRelease = pendingPermitRelease;
                pendingPermitRelease = null;
                worker.start(task, transferredPermitRelease);
            } catch (InterruptedException exception) {
                if (pendingPermitRelease != null) {
                    pendingPermitRelease.run();
                }
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException exception) {
                if (pendingPermitRelease != null) {
                    pendingPermitRelease.run();
                }
                log.error("chat_dispatch_failed type={}", exception.getClass().getSimpleName(), exception);
            }
        }
    }

    @PreDestroy
    void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }
}
