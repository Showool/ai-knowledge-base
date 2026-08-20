package com.jason.ai.knowledgebase.model.enums;

/**
 * 对话请求的终结原因。
 */
public enum ChatCloseReason {
    COMPLETED,
    GENERATION_TIMEOUT,
    LLM_FAILED,
    ENQUEUE_FAILED,
    QUEUE_TIMEOUT,
    USER_CANCELLED,
    CLIENT_DISCONNECTED,
    MAX_LIFETIME,
    SERVER_SHUTDOWN
}
