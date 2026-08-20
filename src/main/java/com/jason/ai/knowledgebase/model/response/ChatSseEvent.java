package com.jason.ai.knowledgebase.model.response;

import java.time.Instant;

import com.jason.ai.knowledgebase.model.enums.ChatEventType;

/**
 * 非心跳 SSE 事件的统一数据载荷。
 *
 * @param requestId 请求 ID
 * @param sessionId 会话 ID
 * @param event 事件类型
 * @param sequence 连接内单调递增序号
 * @param delta 增量文本
 * @param assistantMessageId Assistant 消息 ID
 * @param messageStatus 消息状态
 * @param remainingQuota 剩余额度
 * @param closeReason 终结原因
 * @param errorCode 错误码名称
 * @param errorMessage 错误说明
 * @param timestamp 事件时间
 */
public record ChatSseEvent(Long requestId, Long sessionId, ChatEventType event, Long sequence, String delta,
        Long assistantMessageId, String messageStatus, int remainingQuota, String closeReason,
        String errorCode, String errorMessage, Instant timestamp) {
}
