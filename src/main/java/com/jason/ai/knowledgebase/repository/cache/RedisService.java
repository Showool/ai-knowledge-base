package com.jason.ai.knowledgebase.repository.cache;

import java.time.Duration;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** 提供字符串、集合与令牌安全解锁所需的最小 Redis 能力。 */
@Component
@RequiredArgsConstructor
public class RedisService {

    private static final RedisScript<Long> COMPARE_AND_DELETE = RedisScript.of(
            new ClassPathResource("redis/compare-and-delete.lua"), Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 读取字符串值。
     *
     * @param key Redis Key
     * @return 值；不存在时返回 null
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 写入带过期时间的字符串值。
     *
     * @param key Redis Key
     * @param value 值
     * @param ttl 有效期
     */
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 仅在 Key 不存在时写入字符串值。
     *
     * @param key Redis Key
     * @param value 值
     * @param ttl 有效期
     * @return 成功写入时返回 true
     */
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, ttl));
    }

    /**
     * 删除指定 Key。
     *
     * @param key Redis Key
     * @return 实际删除时返回 true
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 仅当锁值仍等于给定令牌时原子删除 Key。
     *
     * @param key 锁 Key
     * @param token 锁令牌
     * @return 成功删除时返回 true
     */
    public boolean compareAndDelete(String key, String token) {
        Long result = redisTemplate.execute(COMPARE_AND_DELETE, List.of(key), token);
        return result != null && result > 0;
    }

    /**
     * 判断成员是否存在于集合中。
     *
     * @param key 集合 Key
     * @param member 成员
     * @return 存在时返回 true
     */
    public boolean isMember(String key, String member) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, member));
    }

    /**
     * 使用临时 Key 重建并原子替换目标集合。
     *
     * @param targetKey 目标 Key
     * @param temporaryKey 临时 Key
     * @param values 新集合内容
     */
    public void replaceSet(String targetKey, String temporaryKey, List<String> values) {
        if (values.isEmpty()) {
            redisTemplate.delete(targetKey);
            return;
        }
        redisTemplate.opsForSet().add(temporaryKey, values.toArray(String[]::new));
        redisTemplate.rename(temporaryKey, targetKey);
    }
}