package com.jason.ai.knowledgebase.service;

import java.time.Instant;
import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.model.response.PageResult;
import com.jason.ai.knowledgebase.model.response.ChatResponses.CreateSessionResponse;
import com.jason.ai.knowledgebase.model.response.ChatResponses.MessageView;
import com.jason.ai.knowledgebase.model.response.ChatResponses.SessionView;
import com.jason.ai.knowledgebase.model.entity.ChatSession;
import com.jason.ai.knowledgebase.model.entity.ConversationMessage;
import com.jason.ai.knowledgebase.repository.mapper.ChatSessionMapper;
import com.jason.ai.knowledgebase.repository.mapper.ConversationMessageMapper;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.common.util.PageBounds;
import com.jason.ai.knowledgebase.common.util.SnowflakeIdGenerator;
import com.jason.ai.knowledgebase.service.converter.ChatResponseConverter;

import lombok.RequiredArgsConstructor;

/**
 * 管理用户会话和 MySQL 完整聊天历史。
 */
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final String DEFAULT_TITLE = "新对话";
    private static final long MAXIMUM_PAGE_SIZE = 100;

    private final ChatSessionMapper sessionMapper;
    private final ConversationMessageMapper messageMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ChatMemory chatMemory;
    private final ActiveRequestLookup activeRequests;
    private final ChatResponseConverter responseConverter;

    /**
     * 创建属于指定用户的新会话。
     *
     * @param userId 用户 ID
     * @return 包含新会话信息的成功响应
     */
    public ApiResponse<CreateSessionResponse> create(long userId) {
        ChatSession session = new ChatSession();
        session.setId(idGenerator.nextId());
        session.setUserId(userId);
        session.setTitle(DEFAULT_TITLE);
        session.setDeleted(0);
        sessionMapper.insert(session);
        return ApiResponse.success(responseConverter.toCreateResponse(session));
    }

    /**
     * 查询用户全部会话及最新消息预览。
     *
     * @param userId 用户 ID
     * @return 包含按更新时间和 ID 倒序排列会话的成功响应
     */
    public ApiResponse<List<SessionView>> list(long userId) {
        List<SessionView> sessions = sessionMapper.findSummaries(userId).stream()
                .map(responseConverter::toSessionView)
                .toList();
        return ApiResponse.success(sessions);
    }

    /**
     * 分页查询指定会话的完整消息历史。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @param page 页码
     * @param size 每页数量
     * @return 消息分页成功响应
     * @throws AppException 会话不存在或不属于当前用户时抛出
     */
    public ApiResponse<PageResult<MessageView>> messages(long userId, long sessionId, long page, long size) {
        requireOwned(userId, sessionId);
        PageBounds bounds = PageBounds.of(page, size, MAXIMUM_PAGE_SIZE);
        Page<ConversationMessage> result = messageMapper.selectPage(new Page<>(bounds.page(), bounds.size()),
                Wrappers.<ConversationMessage>lambdaQuery()
                        .eq(ConversationMessage::getSessionId, sessionId)
                        .eq(ConversationMessage::getUserId, userId)
                        .orderByAsc(ConversationMessage::getId));
        return ApiResponse.page(result.convert(responseConverter::toMessageView));
    }

    /**
     * 软删除会话和消息，并在事务提交后清理 Redis ChatMemory。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @throws AppException 会话不存在、无权访问或仍有活动请求时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(long userId, long sessionId) {
        requireOwned(userId, sessionId);
        if (activeRequests.hasActiveSession(userId, sessionId)) {
            throw new AppException(ErrorCode.CONFLICT, "会话存在活动请求，请先取消");
        }
        Instant now = Instant.now();
        if (sessionMapper.softDeleteOwned(sessionId, userId, now) != 1) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        messageMapper.softDeleteBySession(sessionId, userId, now);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatMemory.clear(String.valueOf(sessionId));
            }
        });
    }

    /**
     * 查询并验证会话归属。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return 用户拥有的会话
     * @throws AppException 会话不存在或不属于当前用户时抛出
     */
    public ChatSession requireOwned(long userId, long sessionId) {
        ChatSession session = sessionMapper.selectOne(Wrappers.<ChatSession>lambdaQuery()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId));
        if (session == null) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        return session;
    }

}
