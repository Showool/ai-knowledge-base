package com.jason.ai.knowledgebase.service.sse;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.config.ChatProperties;
import com.jason.ai.knowledgebase.service.ActiveRequestLookup;

/** 单实例 JVM 内请求生命周期状态的唯一权威注册表。 */
@Component
public class SseConnectionRegistry implements ActiveRequestLookup {

    private final ConcurrentHashMap<Long, SseConnection> connections = new ConcurrentHashMap<>();
    private final Semaphore capacity;

    public SseConnectionRegistry(ChatProperties properties) {
        this.capacity = new Semaphore(properties.getSse().getMaxActiveConnections());
    }

    /**
     * 尝试预留一个活动连接容量。
     *
     * @return 预留成功时返回 true
     */
    public boolean reserveCapacity() {
        return capacity.tryAcquire();
    }

    /** 释放一个尚未被连接终态流程接管的容量。 */
    public void releaseCapacity() {
        capacity.release();
    }

    /**
     * 注册已预留容量的连接。
     *
     * @param connection 连接
     * @throws IllegalStateException 请求 ID 重复时抛出
     */
    public void registerReserved(SseConnection connection) {
        if (connections.putIfAbsent(connection.requestId(), connection) != null) {
            throw new IllegalStateException("请求 ID 重复");
        }
    }

    /**
     * 按请求 ID 查询连接。
     *
     * @param requestId 请求 ID
     * @return 连接；不存在时返回 null
     */
    public SseConnection find(long requestId) {
        return connections.get(requestId);
    }

    /**
     * 按实例条件移除连接并完成事件通道。
     *
     * @param connection 连接
     */
    public void remove(SseConnection connection) {
        if (connections.remove(connection.requestId(), connection)) {
            connection.completeChannel();
        }
    }

    /**
     * 返回当前连接的只读快照。
     *
     * @return 连接快照
     */
    public Collection<SseConnection> all() {
        return List.copyOf(connections.values());
    }

    /**
     * 判断会话是否仍有未终结请求。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return 存在活动请求时返回 true
     */
    @Override
    public boolean hasActiveSession(long userId, long sessionId) {
        return connections.values().stream().anyMatch(connection -> connection.userId() == userId
                && connection.sessionId() == sessionId && !connection.state().terminal());
    }
}