package com.jason.ai.knowledgebase.repository.cache;

import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.config.AuthProperties;

import lombok.RequiredArgsConstructor;

/**
 * Redis 当前登录会话指针。
 */
@Component
@RequiredArgsConstructor
public class AuthSessionStore {

    private static final String KEY_PREFIX = "akb:auth:session:";

    private final RedisService redisService;
    private final AuthProperties properties;

    /**
     * 查询用户当前有效的认证会话 ID。
     *
     * @param userId 用户 ID
     * @return 会话 ID 字符串；不存在时返回 null
     */
    public String get(long userId) {
        return redisService.get(key(userId));
    }

    /**
     * 保存用户当前有效的认证会话 ID。
     *
     * @param userId 用户 ID
     * @param authSessionId 认证会话 ID
     */
    public void set(long userId, long authSessionId) {
        redisService.set(key(userId), String.valueOf(authSessionId), properties.getRefreshTokenTtl());
    }

    /**
     * 删除用户当前登录会话指针。
     *
     * @param userId 用户 ID
     */
    public void delete(long userId) {
        redisService.delete(key(userId));
    }

    private String key(long userId) {
        return KEY_PREFIX + userId;
    }
}
