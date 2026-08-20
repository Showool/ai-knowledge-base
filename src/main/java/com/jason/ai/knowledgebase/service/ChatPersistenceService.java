package com.jason.ai.knowledgebase.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jason.ai.knowledgebase.model.internal.AdmissionReceipt;
import com.jason.ai.knowledgebase.model.entity.ConversationMessage;
import com.jason.ai.knowledgebase.model.enums.MessageRole;
import com.jason.ai.knowledgebase.model.enums.MessageStatus;
import com.jason.ai.knowledgebase.repository.mapper.ChatSessionMapper;
import com.jason.ai.knowledgebase.repository.mapper.ConversationMessageMapper;
import com.jason.ai.knowledgebase.common.util.UnicodeText;
import com.jason.ai.knowledgebase.common.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;

/**
 * 在同一事务中扣减额度并保存同轮 User/Assistant 消息。
 */
@Service
@RequiredArgsConstructor
public class ChatPersistenceService {

    private static final int TITLE_LENGTH = 30;

    private final ChatSessionMapper sessionMapper;
    private final ConversationMessageMapper messageMapper;
    private final QuotaService quotaService;
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 持久化已通过准入的请求。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @param requestId 请求 ID
     * @param originalMessage 通过校验的原始问题，用于 MySQL 和模型
     * @param normalizedTitle 规范化问题，仅用于首次标题
     * @return 消息 ID 和剩余额度
     */
    @Transactional(rollbackFor = Exception.class)
    public AdmissionReceipt admit(long userId, long sessionId, long requestId, String originalMessage,
            String normalizedTitle) {
        long userMessageId = idGenerator.nextId();
        long assistantMessageId = idGenerator.nextId();
        Instant now = Instant.now();
        int remaining = quotaService.consume(userId);
        sessionMapper.initializeTitle(sessionId, userId,
                UnicodeText.truncate(normalizedTitle, TITLE_LENGTH), now);

        ConversationMessage userMessage = message(userMessageId, sessionId, userId, requestId,
                MessageRole.USER, originalMessage, null);
        ConversationMessage assistant = message(assistantMessageId, sessionId, userId, requestId,
                MessageRole.ASSISTANT, "", MessageStatus.GENERATING);
        messageMapper.insert(userMessage);
        messageMapper.insert(assistant);
        sessionMapper.touch(sessionId, now);
        return new AdmissionReceipt(requestId, userMessageId, assistantMessageId, remaining);
    }

    /**
     * 保存 Assistant 的最终内容、状态和 metadata。
     *
     * @param messageId Assistant 消息 ID
     * @param content 完整或部分回答
     * @param status 终态消息状态
     * @param metadata JSON metadata
     */
    public void finishAssistant(long messageId, String content, MessageStatus status, String metadata) {
        messageMapper.finishAssistant(messageId, content, status.name(), metadata, Instant.now());
    }

    private ConversationMessage message(long id, long sessionId, long userId, long requestId, MessageRole role,
            String content, MessageStatus status) {
        ConversationMessage message = new ConversationMessage();
        message.setId(id);
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRequestId(requestId);
        message.setRole(role.name());
        message.setContent(content);
        message.setStatus(status == null ? null : status.name());
        message.setDeleted(0);
        return message;
    }
}
