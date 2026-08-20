package com.jason.ai.knowledgebase.model.response;

import java.time.Instant;

/** 会话与消息接口响应数据。 */
public final class ChatResponses {
    private ChatResponses() {
    }

    public record CreateSessionResponse(Long sessionId, String title, Instant createTime) {
    }

    public record SessionView(Long id, String title, String lastMessagePreview, Instant createTime,
            Instant updateTime) {
    }

    public record MessageView(Long id, Long requestId, String role, String content, String status,
            Object metadata, Instant createTime, Instant updateTime) {
    }
}