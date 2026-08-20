package com.jason.ai.knowledgebase.model.internal;

/**
 * 对话持久化准入结果。
 *
 * @param requestId 请求 ID
 * @param userMessageId User 消息 ID
 * @param assistantMessageId Assistant 消息 ID
 * @param remainingQuota 扣减后的剩余额度
 */
public record AdmissionReceipt(Long requestId, Long userMessageId, Long assistantMessageId, int remainingQuota) {
}