package com.jason.ai.knowledgebase.model.response;

/** 额度接口响应数据。 */
public final class QuotaResponses {
    private QuotaResponses() {
    }

    public record QuotaView(int availableTimes) {
    }
}