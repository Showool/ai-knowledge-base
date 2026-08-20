package com.jason.ai.knowledgebase.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 无意义短语管理接口请求参数。 */
public final class PhraseRequests {
    private PhraseRequests() {
    }

    public record SaveRequest(@NotBlank @Size(max = 256) String phrase, @Size(max = 64) String category,
            Integer priority, @Size(max = 500) String remark) {
    }

    public record StatusRequest(@NotNull Boolean enabled) {
    }
}