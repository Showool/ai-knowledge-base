package com.jason.ai.knowledgebase.service.sse;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.jason.ai.knowledgebase.model.internal.NormalizedInput;
import com.jason.ai.knowledgebase.service.RequestAdmissionService;
import com.jason.ai.knowledgebase.repository.cache.ChatUserLockStore;
import com.jason.ai.knowledgebase.repository.cache.ChatUserLockStore.LockToken;
import com.jason.ai.knowledgebase.model.internal.AdmissionReceipt;
import com.jason.ai.knowledgebase.model.response.ChatSseEvent;
import com.jason.ai.knowledgebase.model.enums.ChatEventType;
import com.jason.ai.knowledgebase.model.enums.MessageStatus;
import com.jason.ai.knowledgebase.service.ChatPersistenceService;
import com.jason.ai.knowledgebase.service.ChatSessionService;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.config.ChatProperties;
import com.jason.ai.knowledgebase.common.util.SnowflakeIdGenerator;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 协调请求准入、额度扣减、本地注册和 FIFO 入队。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSseOrchestrator {

    private final ChatSessionService sessionService;
    private final RequestAdmissionService admissionService;
    private final SseConnectionRegistry registry;
    private final ChatGenerationQueue queue;
    private final ChatPersistenceService persistence;
    private final ChatLifecycleService lifecycle;
    private final ChatUserLockStore userLockStore;
    private final ChatProperties properties;
    private final SnowflakeIdGenerator idGenerator;
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("chat-queue-timeout-").factory());

    /**
     * 受理用户流式对话请求。
     *
     * @param userId 当前用户 ID
     * @param sessionId 会话 ID
     * @param message 用户原始问题
     * @return 包含状态、增量和唯一终态的 SSE 流
     */
    public Flux<ServerSentEvent<ChatSseEvent>> start(long userId, long sessionId, String message) {
        long requestId = idGenerator.nextId();
        try {
            return admit(requestId, userId, sessionId, message);
        } catch (AppException exception) {
            return failed(requestId, sessionId, null, 0, exception.errorCode(), exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("chat_admission_failed requestId={} type={}", requestId,
                    exception.getClass().getSimpleName(), exception);
            return failed(requestId, sessionId, null, 0, ErrorCode.INTERNAL_ERROR,
                    ErrorCode.INTERNAL_ERROR.message());
        }
    }

    /**
     * 取消属于指定用户的活动请求。
     *
     * @param userId 用户 ID
     * @param requestId 请求 ID
     */
    public void cancel(long userId, long requestId) {
        lifecycle.cancel(requestId, userId);
    }

    /**
     * 严格按照容量、队列槽、用户锁、事务持久化、Registry 和入队顺序执行准入。
     */
    private Flux<ServerSentEvent<ChatSseEvent>> admit(long requestId, long userId, long sessionId,
            String message) {
        sessionService.requireOwned(userId, sessionId);
        NormalizedInput normalized = admissionService.evaluate(message);
        if (!registry.reserveCapacity()) {
            return failed(requestId, sessionId, null, 0, ErrorCode.CAPACITY_FULL,
                    ErrorCode.CAPACITY_FULL.message());
        }
        if (!queue.reserveSlot()) {
            registry.releaseCapacity();
            return failed(requestId, sessionId, null, 0, ErrorCode.CAPACITY_FULL,
                    ErrorCode.CAPACITY_FULL.message());
        }

        LockToken lockToken;
        try {
            lockToken = userLockStore.tryAcquire(userId);
        } catch (RuntimeException exception) {
            releasePreAdmission(null);
            log.warn("chat_user_lock_acquire_failed requestId={} type={}", requestId,
                    exception.getClass().getSimpleName());
            return failed(requestId, sessionId, null, 0, ErrorCode.DEPENDENCY_UNAVAILABLE,
                    ErrorCode.DEPENDENCY_UNAVAILABLE.message());
        }
        if (lockToken == null) {
            releasePreAdmission(null);
            return failed(requestId, sessionId, null, 0, ErrorCode.USER_BUSY,
                    ErrorCode.USER_BUSY.message());
        }

        AdmissionReceipt receipt;
        try {
            receipt = persistence.admit(userId, sessionId, requestId,
                    normalized.original(), normalized.normalized());
        } catch (AppException exception) {
            releasePreAdmission(lockToken);
            return failed(requestId, sessionId, null, 0, exception.errorCode(), exception.getMessage());
        } catch (RuntimeException exception) {
            releasePreAdmission(lockToken);
            log.error("chat_persistence_admission_failed requestId={} type={}", requestId,
                    exception.getClass().getSimpleName(), exception);
            return failed(requestId, sessionId, null, 0, ErrorCode.INTERNAL_ERROR,
                    ErrorCode.INTERNAL_ERROR.message());
        }

        SseConnection connection;
        try {
            connection = createConnection(userId, sessionId, normalized, receipt, lockToken);
        } catch (RuntimeException exception) {
            releasePreAdmission(lockToken);
            finishUnregisteredAssistant(receipt.assistantMessageId());
            log.error("chat_connection_creation_failed requestId={} type={}", requestId,
                    exception.getClass().getSimpleName(), exception);
            return failed(requestId, sessionId, receipt.assistantMessageId(), receipt.remainingQuota(),
                    ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message());
        }

        boolean registered = false;
        try {
            registry.registerReserved(connection);
            registered = true;
            queue.submitReserved(connection.task());
            connection.emit(ChatEventType.QUEUED, null, MessageStatus.GENERATING, null, null, null);
        } catch (RuntimeException exception) {
            if (!registered) {
                queue.releaseReservation();
            }
            lifecycle.admissionFailed(connection, exception);
            return terminalFlux(connection);
        }

        try {
            timeoutExecutor.schedule(() -> lifecycle.queueTimeout(connection.requestId()),
                    properties.getSse().getQueueTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException exception) {
            lifecycle.admissionFailed(connection, exception);
            return terminalFlux(connection);
        }
        return activeFlux(connection, userId);
    }

    private SseConnection createConnection(long userId, long sessionId, NormalizedInput normalized,
            AdmissionReceipt receipt, LockToken lockToken) {
        SseConnection connection = new SseConnection(receipt.requestId(), userId, sessionId,
                receipt.userMessageId(), receipt.assistantMessageId(), receipt.remainingQuota(), lockToken,
                properties.getSse().getSinkBufferSize());
        connection.task(new GenerationTask(connection, normalized.original()));
        return connection;
    }

    private Flux<ServerSentEvent<ChatSseEvent>> activeFlux(SseConnection connection, long userId) {
        Duration heartbeatInterval = properties.getSse().getHeartbeatInterval();
        Flux<ServerSentEvent<ChatSseEvent>> heartbeat = Flux.interval(heartbeatInterval)
                .map(ignored -> ServerSentEvent.<ChatSseEvent>builder().comment("heartbeat").build());
        return Flux.merge(connection.eventFlux(), heartbeat)
                .takeUntil(event -> event.data() != null && event.data().event().terminal())
                .doOnCancel(() -> lifecycle.disconnected(connection.requestId(), userId));
    }

    private Flux<ServerSentEvent<ChatSseEvent>> terminalFlux(SseConnection connection) {
        return connection.eventFlux()
                .takeUntil(event -> event.data() != null && event.data().event().terminal());
    }

    private Flux<ServerSentEvent<ChatSseEvent>> failed(long requestId, long sessionId, Long assistantMessageId,
            int remainingQuota, ErrorCode error, String errorMessage) {
        ChatSseEvent event = new ChatSseEvent(requestId, sessionId, ChatEventType.FAILED, 1L, null,
                assistantMessageId, MessageStatus.FAILED.name(), remainingQuota,
                error.name(), error.name(), errorMessage, Instant.now());
        return Flux.just(ServerSentEvent.<ChatSseEvent>builder()
                .id(requestId + ":1")
                .event(ChatEventType.FAILED.wireValue())
                .data(event)
                .build());
    }

    private void finishUnregisteredAssistant(long assistantMessageId) {
        try {
            persistence.finishAssistant(assistantMessageId, "", MessageStatus.FAILED, null);
        } catch (RuntimeException exception) {
            log.error("assistant_admission_failure_persistence_failed messageId={} type={}", assistantMessageId,
                    exception.getClass().getSimpleName(), exception);
        }
    }

    /**
     * 回滚尚未注册到 Registry 的准入资源。
     */
    private void releasePreAdmission(LockToken lockToken) {
        if (lockToken != null) {
            try {
                userLockStore.release(lockToken);
            } catch (RuntimeException ignored) {
                // 用户锁 TTL 是依赖异常时的最终清理兜底。
            }
        }
        queue.releaseReservation();
        registry.releaseCapacity();
    }

    @PreDestroy
    void stopTimeoutExecutor() {
        timeoutExecutor.shutdownNow();
    }
}
