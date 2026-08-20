package com.jason.ai.knowledgebase.model.enums;

/**
 * 活动对话请求失败时写入 SSE 和 metadata 的内部错误码。
 */
public enum ChatFailureCode {
    GENERATION_TIMEOUT,
    LLM_FAILED,
    INTERNAL_ERROR,
    QUEUE_TIMEOUT,
    MAX_LIFETIME,
    SERVER_SHUTDOWN
}
