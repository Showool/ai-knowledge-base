package com.jason.ai.knowledgebase.service.sse;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.jason.ai.knowledgebase.repository.cache.ChatUserLockStore;
import com.jason.ai.knowledgebase.model.enums.ChatCloseReason;
import com.jason.ai.knowledgebase.model.enums.ChatFailureCode;
import com.jason.ai.knowledgebase.model.internal.ChatMessageMetadata;

import com.jason.ai.knowledgebase.model.enums.ChatRequestState;
import com.jason.ai.knowledgebase.model.enums.MessageStatus;
import com.jason.ai.knowledgebase.service.ChatPersistenceService;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.config.AiProperties;
import com.jason.ai.knowledgebase.config.ChatProperties;
import com.jason.ai.knowledgebase.common.util.JsonCodec;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 负责唯一终态迁移、Assistant 持久化和全部资源释放。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLifecycleService {

    private final SseConnectionRegistry registry;
    private final ChatGenerationQueue queue;
    private final ChatPersistenceService persistence;
    private final ChatUserLockStore userLockStore;
    private final AiProperties aiProperties;
    private final ChatProperties chatProperties;
    private final JsonCodec jsonCodec;
    private final ScheduledExecutorService retentionExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("chat-registry-retention-").factory());

    /**
     * 将已出队请求迁移为生成中，并接管 LLM 并发许可。
     *
     * @param connection 请求连接
     * @param permitRelease 只允许执行一次的许可释放回调
     * @return 成功开始生成时返回 true
     */
    public boolean beginGeneration(SseConnection connection, Runnable permitRelease) {
        connection.setLlmPermitRelease(permitRelease);
        if (!connection.transition(ChatRequestState.QUEUED, ChatRequestState.GENERATING)) {
            connection.releaseLlmPermit();
            return false;
        }
        connection.emit(com.jason.ai.knowledgebase.model.enums.ChatEventType.GENERATING, null,
                MessageStatus.GENERATING, null, null, null);
        return true;
    }

    /**
     * 聚合并转发一个非空模型增量。
     *
     * @param connection 请求连接
     * @param delta 模型增量
     */
    public void delta(SseConnection connection, String delta) {
        if (delta == null || delta.isEmpty() || connection.state() != ChatRequestState.GENERATING) {
            return;
        }
        connection.append(delta);
        connection.emit(com.jason.ai.knowledgebase.model.enums.ChatEventType.DELTA, delta,
                MessageStatus.GENERATING, null, null, null);
    }

    /**
     * 正常完成模型生成。
     */
    public void completed(SseConnection connection) {
        finish(connection, TerminalOutcome.completed(), null);
    }

    /**
     * 按异常 cause 链识别超时并终结生成。
     */
    public void generationFailed(SseConnection connection, Throwable error) {
        boolean timeout = isGenerationTimeout(error);
        ChatCloseReason reason = timeout ? ChatCloseReason.GENERATION_TIMEOUT : ChatCloseReason.LLM_FAILED;
        ChatFailureCode code = timeout ? ChatFailureCode.GENERATION_TIMEOUT : ChatFailureCode.LLM_FAILED;
        finish(connection, TerminalOutcome.generationFailure(reason, code),
                error.getClass().getSimpleName());
    }

    /**
     * 判断异常 cause 链是否代表模型生成超时。
     *
     * @param error 原始异常
     * @return 属于超时时返回 true
     */
    static boolean isGenerationTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof TimeoutException || current instanceof SocketTimeoutException) {
                return true;
            }
            if (current instanceof InterruptedIOException && hasTimeoutMessage(current.getMessage())) {
                return true;
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        return false;
    }

    private static boolean hasTimeoutMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("timeout") || normalized.contains("timed out");
    }

    /**
     * 终结注册或入队阶段发生异常的请求。
     */
    public void admissionFailed(SseConnection connection, Throwable error) {
        finish(connection, TerminalOutcome.admissionFailure(), error.getClass().getSimpleName());
    }

    /**
     * 终结仍停留在队列中的超时请求。
     */
    public void queueTimeout(long requestId) {
        SseConnection connection = registry.find(requestId);
        if (connection != null && connection.state() == ChatRequestState.QUEUED) {
            finish(connection, TerminalOutcome.queueTimeout(), "Queue timeout");
        }
    }

    /**
     * 取消属于指定用户的活动请求。
     *
     * @throws AppException 请求不存在、已结束或不属于用户时抛出
     */
    public void cancel(long requestId, long userId) {
        SseConnection connection = registry.find(requestId);
        if (connection == null || connection.userId() != userId) {
            throw new AppException(ErrorCode.REQUEST_NOT_ACTIVE);
        }
        if (!finish(connection, TerminalOutcome.cancelled(), null)) {
            throw new AppException(ErrorCode.REQUEST_NOT_ACTIVE);
        }
    }

    /**
     * 客户端断开时停止模型调用并保存部分回答。
     */
    public void disconnected(long requestId, long userId) {
        SseConnection connection = registry.find(requestId);
        if (connection != null && connection.userId() == userId) {
            finish(connection, TerminalOutcome.disconnected(), null);
        }
    }

    /**
     * 终结超过最大生命周期的请求。
     */
    public void maxLifetimeExceeded(SseConnection connection) {
        finish(connection, TerminalOutcome.maxLifetime(), "Maximum request lifetime exceeded");
    }

    /**
     * 服务关闭时终结尚未完成的请求。
     */
    public void shutdown(SseConnection connection) {
        finish(connection, TerminalOutcome.shutdown(), "Server is shutting down");
    }

    /**
     * 原子取得终态处理权，再按固定顺序取消任务、持久化、发送事件和释放资源。
     */
    private boolean finish(SseConnection connection, TerminalOutcome outcome, String errorMessage) {
        ChatRequestState previous = transitionToTerminal(connection, outcome.state());
        if (previous == null) {
            return false;
        }
        if (previous == ChatRequestState.QUEUED) {
            queue.cancel(connection.task());
        }
        if (outcome.disposeSubscription() && previous == ChatRequestState.GENERATING) {
            connection.disposeSubscription();
        }
        try {
            persistence.finishAssistant(connection.assistantMessageId(), connection.answer(),
                    outcome.messageStatus(), metadata(outcome));
        } catch (RuntimeException exception) {
            log.error("assistant_terminal_persistence_failed requestId={} type={}", connection.requestId(),
                    exception.getClass().getSimpleName(), exception);
        }
        if (outcome.eventType() != null) {
            connection.emit(outcome.eventType(), null, outcome.messageStatus(), outcome.closeReason(),
                    outcome.errorCode(), errorMessage);
        }
        releaseResources(connection);
        return true;
    }

    private ChatRequestState transitionToTerminal(SseConnection connection, ChatRequestState terminalState) {
        while (true) {
            ChatRequestState current = connection.state();
            if (current.terminal()) {
                return null;
            }
            if (connection.transition(current, terminalState)) {
                return current;
            }
        }
    }

    private String metadata(TerminalOutcome outcome) {
        String errorCode = outcome.errorCode() == null ? null : outcome.errorCode().name();
        return jsonCodec.write(ChatMessageMetadata.current(aiProperties.getModel(),
                outcome.closeReason().name(), errorCode));
    }

    /**
     * 使用连接内的一次性标记保证锁、容量和许可最多释放一次。
     */
    private void releaseResources(SseConnection connection) {
        if (!connection.markResourcesReleased()) {
            return;
        }
        connection.releaseLlmPermit();
        try {
            userLockStore.release(connection.lockToken());
        } catch (RuntimeException exception) {
            log.warn("chat_user_lock_release_failed requestId={} type={}", connection.requestId(),
                    exception.getClass().getSimpleName());
        }
        registry.releaseCapacity();
        try {
            retentionExecutor.schedule(() -> registry.remove(connection),
                    chatProperties.getSse().getTerminalRetention().toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException exception) {
            registry.remove(connection);
        }
    }

    @PreDestroy
    void closeRetentionExecutor() {
        retentionExecutor.shutdownNow();
    }
}
