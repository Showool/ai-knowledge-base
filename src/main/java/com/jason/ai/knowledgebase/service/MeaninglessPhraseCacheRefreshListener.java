package com.jason.ai.knowledgebase.service;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jason.ai.knowledgebase.model.entity.MeaninglessPhrase;
import com.jason.ai.knowledgebase.model.event.MeaninglessPhraseChangedEvent;
import com.jason.ai.knowledgebase.repository.cache.MeaninglessPhraseCache;
import com.jason.ai.knowledgebase.repository.mapper.MeaninglessPhraseMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 在应用启动和短语事务提交后重建 Redis 短语集合。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeaninglessPhraseCacheRefreshListener {

    private final MeaninglessPhraseCache cache;
    private final MeaninglessPhraseMapper mapper;

    /**
     * 应用启动完成后加载启用的短语。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refreshSafely();
    }

    /**
     * 短语变更事务提交后刷新缓存。
     *
     * @param event 短语已变更事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refreshAfterCommit(MeaninglessPhraseChangedEvent event) {
        refreshSafely();
    }

    /**
     * 查询权威数据并替换缓存；刷新失败时保留现有缓存并记录日志。
     */
    private void refreshSafely() {
        try {
            List<String> phrases = mapper.selectList(Wrappers.<MeaninglessPhrase>lambdaQuery()
                    .eq(MeaninglessPhrase::getEnabled, true)
                    .orderByDesc(MeaninglessPhrase::getPriority)
                    .orderByAsc(MeaninglessPhrase::getId))
                    .stream()
                    .map(MeaninglessPhrase::getPhrase)
                    .distinct()
                    .toList();
            cache.replace(phrases);
        } catch (RuntimeException exception) {
            log.warn("meaningless_phrase_cache_refresh_failed type={}", exception.getClass().getSimpleName());
        }
    }
}
