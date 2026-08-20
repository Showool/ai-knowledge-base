package com.jason.ai.knowledgebase.repository.cache;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.config.ChatProperties;
import lombok.RequiredArgsConstructor;

/**
 * Redis 单用户对话互斥锁。
 */
@Component
@RequiredArgsConstructor
public class ChatUserLockStore {

    private static final String KEY_PREFIX = "akb:chat:user-lock:";

    private final RedisService redisService;
    private final ChatProperties properties;

    /**
     * 尝试获取用户互斥锁。
     *
     * @param userId 用户 ID
     * @return 获取成功时返回锁凭据，锁已被占用时返回 null
     */
    public LockToken tryAcquire(long userId) {
        String key = KEY_PREFIX + userId;
        String token = UUID.randomUUID().toString();
        return redisService.setIfAbsent(key, token, properties.getSse().getUserLockTtl())
                ? new LockToken(key, token)
                : null;
    }

    /**
     * 仅在锁 Token 匹配时释放锁。
     *
     * @param lockToken 锁凭据
     * @return 是否实际删除了锁
     */
    public boolean release(LockToken lockToken) {
        return redisService.compareAndDelete(lockToken.key(), lockToken.token());
    }

    /**
     * 用户锁凭据。
     *
     * @param key Redis Key
     * @param token 所有权 Token
     */
    public record LockToken(String key, String token) {
    }
}
