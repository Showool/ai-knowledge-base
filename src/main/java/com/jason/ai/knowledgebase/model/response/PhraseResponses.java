package com.jason.ai.knowledgebase.model.response;

import java.time.Instant;

/** 无意义短语管理接口响应数据。 */
public final class PhraseResponses {
    private PhraseResponses() {
    }

    public record PhraseView(Long id, String phrase, String category, boolean enabled, int priority,
            String remark, Instant createTime, Instant updateTime) {
    }
}