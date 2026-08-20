package com.jason.ai.knowledgebase.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 对话接口请求参数。 */
public final class ChatRequests {
    private ChatRequests() {
    }

    public record StreamRequest(@NotNull Long sessionId, @NotBlank String message) {
    }
}