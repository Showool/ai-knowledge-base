package com.jason.ai.knowledgebase.service.sse;

import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.codec.ServerSentEvent;

import com.jason.ai.knowledgebase.repository.cache.ChatUserLockStore.LockToken;
import com.jason.ai.knowledgebase.model.response.ChatSseEvent;
import com.jason.ai.knowledgebase.model.enums.ChatCloseReason;
import com.jason.ai.knowledgebase.model.enums.ChatEventType;
import com.jason.ai.knowledgebase.model.enums.ChatFailureCode;
import com.jason.ai.knowledgebase.model.enums.ChatRequestState;
import com.jason.ai.knowledgebase.model.enums.MessageStatus;

import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 单个对话请求的线程安全状态、文本缓冲区、取消句柄和事件通道。
 */
@Slf4j
public class SseConnection {

    private final long requestId;
    private final long userId;
    private final long sessionId;
    private final long userMessageId;
    private final long assistantMessageId;
    private final int remainingQuota;
    private final LockToken lockToken;
    private final Instant createdAt = Instant.now();
    private final AtomicReference<ChatRequestState> state = new AtomicReference<>(ChatRequestState.QUEUED);
    private final AtomicLong sequence = new AtomicLong();
    private final StringBuilder answer = new StringBuilder();
    private final AtomicReference<Disposable> subscription = new AtomicReference<>();
    private final AtomicReference<Runnable> llmPermitRelease = new AtomicReference<>();
    private final AtomicBoolean resourcesReleased = new AtomicBoolean();
    private final Sinks.Many<ChatSseEvent> sink;
    private volatile GenerationTask task;

    /**
     * 创建已通过持久化准入的连接。
     *
     * @param requestId 请求 ID
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @param userMessageId User 消息 ID
     * @param assistantMessageId Assistant 消息 ID
     * @param remainingQuota 扣减后的剩余额度
     * @param lockToken Redis 用户锁凭据
     * @param bufferSize SSE 事件缓冲区容量
     */
    public SseConnection(long requestId, long userId, long sessionId, long userMessageId,
            long assistantMessageId, int remainingQuota, LockToken lockToken, int bufferSize) {
        this.requestId = requestId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.userMessageId = userMessageId;
        this.assistantMessageId = assistantMessageId;
        this.remainingQuota = remainingQuota;
        this.lockToken = lockToken;
        Queue<ChatSseEvent> queue = new ArrayBlockingQueue<>(bufferSize);
        this.sink = Sinks.many().unicast().onBackpressureBuffer(queue);
    }

    /**
     * 返回转换为 ServerSentEvent 的事件流。
     *
     * @return 当前连接的单订阅事件流
     */
    public Flux<ServerSentEvent<ChatSseEvent>> eventFlux() {
        return sink.asFlux().map(event -> ServerSentEvent.<ChatSseEvent>builder()
                .id(requestId + ":" + event.sequence())
                .event(event.event().wireValue())
                .data(event)
                .build());
    }

    /**
     * 发布一个业务事件，并显式检查 Sink 发布结果。
     *
     * @param type 事件类型
     * @param delta 可选文本增量
     * @param messageStatus 消息状态
     * @param closeReason 可选关闭原因
     * @param errorCode 可选错误码
     * @param errorMessage 可选错误信息
     * @return 发布成功时返回 true
     */
    public boolean emit(ChatEventType type, String delta, MessageStatus messageStatus,
            ChatCloseReason closeReason, ChatFailureCode errorCode, String errorMessage) {
        ChatSseEvent event = new ChatSseEvent(requestId, sessionId, type, sequence.incrementAndGet(), delta,
                assistantMessageId, messageStatus.name(), remainingQuota,
                closeReason == null ? null : closeReason.name(),
                errorCode == null ? null : errorCode.name(),
                errorMessage, Instant.now());
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure()) {
            log.warn("chat_sse_emit_failed requestId={} event={} result={}",
                    requestId, type.wireValue(), result);
        }
        return result.isSuccess();
    }

    /**
     * 原子迁移请求状态。
     *
     * @param expected 预期当前状态
     * @param next 目标状态
     * @return 当前状态与预期一致且迁移成功时返回 true
     */
    public boolean transition(ChatRequestState expected, ChatRequestState next) {
        return state.compareAndSet(expected, next);
    }

    /**
     * 线程安全地追加一个模型增量。
     *
     * @param delta 模型文本增量
     */
    public void append(String delta) {
        synchronized (answer) {
            answer.append(delta);
        }
    }

    /**
     * 返回目前已聚合的完整或部分回答。
     *
     * @return 回答快照
     */
    public String answer() {
        synchronized (answer) {
            return answer.toString();
        }
    }

    /**
     * 保存模型订阅；连接已终结或已有订阅时立即取消新订阅。
     *
     * @param disposable 模型流订阅
     */
    public void setSubscription(Disposable disposable) {
        if (!subscription.compareAndSet(null, disposable)) {
            disposable.dispose();
            return;
        }
        if (state.get().terminal()) {
            disposable.dispose();
        }
    }

    /**
     * 取消模型订阅。
     */
    public void disposeSubscription() {
        Disposable disposable = subscription.getAndSet(null);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    /**
     * 注册 LLM 并发许可的一次性释放回调。
     *
     * @param release 许可释放回调
     * @throws IllegalStateException 已注册过释放回调时抛出
     */
    public void setLlmPermitRelease(Runnable release) {
        if (!llmPermitRelease.compareAndSet(null, release)) {
            release.run();
            throw new IllegalStateException("LLM 并发许可释放回调已注册");
        }
    }

    /**
     * 至多执行一次 LLM 并发许可释放回调。
     */
    public void releaseLlmPermit() {
        Runnable release = llmPermitRelease.getAndSet(null);
        if (release != null) {
            release.run();
        }
    }

    /**
     * 完成事件通道。
     */
    public void completeChannel() {
        sink.tryEmitComplete();
    }

    /** @return 请求 ID */
    public long requestId() {
        return requestId;
    }

    /** @return 用户 ID */
    public long userId() {
        return userId;
    }

    /** @return 会话 ID */
    public long sessionId() {
        return sessionId;
    }

    /** @return User 消息 ID */
    public long userMessageId() {
        return userMessageId;
    }

    /** @return Assistant 消息 ID */
    public long assistantMessageId() {
        return assistantMessageId;
    }

    /** @return 扣减后的剩余额度 */
    public int remainingQuota() {
        return remainingQuota;
    }

    /** @return Redis 用户锁凭据 */
    public LockToken lockToken() {
        return lockToken;
    }

    /** @return 连接创建时间 */
    public Instant createdAt() {
        return createdAt;
    }

    /** @return 当前请求状态 */
    public ChatRequestState state() {
        return state.get();
    }

    /**
     * 标记本地和 Redis 资源已释放。
     *
     * @return 本次调用首次取得释放权时返回 true
     */
    public boolean markResourcesReleased() {
        return resourcesReleased.compareAndSet(false, true);
    }

    /** @return 当前关联的队列任务 */
    public GenerationTask task() {
        return task;
    }

    /**
     * 关联已提交的队列任务。
     *
     * @param task 队列任务
     */
    public void task(GenerationTask task) {
        this.task = task;
    }
}
