package com.jason.ai.knowledgebase.repository.cache;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redis 无意义短语集合。
 */
@Component
@RequiredArgsConstructor
public class MeaninglessPhraseCache {

    private static final String CACHE_KEY = "akb:admission:meaningless-phrases";

    private final RedisService redisService;

    /**
     * 判断规范化短语是否命中缓存。
     *
     * @param phrase 用于比较的规范化短语
     * @return 命中时返回 true
     */
    public boolean contains(String phrase) {
        return redisService.isMember(CACHE_KEY, phrase);
    }

    /**
     * 原子替换完整短语集合。
     *
     * @param phrases 已启用且完成规范化的短语
     */
    public void replace(List<String> phrases) {
        String temporaryKey = CACHE_KEY + ":refresh:" + UUID.randomUUID();
        redisService.replaceSet(CACHE_KEY, temporaryKey, phrases);
    }
}
