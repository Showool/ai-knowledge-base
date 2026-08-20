package com.jason.ai.knowledgebase.model.enums;

/**
 * MySQL 中保存的消息状态。
 */
public enum MessageStatus {
    GENERATING,
    COMPLETED,
    FAILED,
    CANCELLED,
    CLIENT_DISCONNECTED
}
