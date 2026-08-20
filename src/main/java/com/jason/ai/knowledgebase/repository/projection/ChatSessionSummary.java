package com.jason.ai.knowledgebase.repository.projection;

import java.time.Instant;

import lombok.Data;

/**
 * 会话列表单次查询投影。
 */
@Data
public class ChatSessionSummary {
    private Long id;
    private String title;
    private String lastMessagePreview;
    private Instant createTime;
    private Instant updateTime;
}
