package com.jason.ai.knowledgebase.model.event;

/** 表示事务提交后需要重建 Redis 短语集合。 */
public record MeaninglessPhraseChangedEvent() {
}
