package com.jason.ai.knowledgebase.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jason.ai.knowledgebase.common.api.ApiResponse;
import com.jason.ai.knowledgebase.common.api.PageResult;
import com.jason.ai.knowledgebase.model.response.ChatResponses.CreateSessionResponse;
import com.jason.ai.knowledgebase.model.response.ChatResponses.MessageView;
import com.jason.ai.knowledgebase.model.response.ChatResponses.SessionView;
import com.jason.ai.knowledgebase.security.SecurityUtils;
import com.jason.ai.knowledgebase.service.ChatSessionService;
import lombok.RequiredArgsConstructor;

/** 会话创建、列表、历史消息与软删除接口。 */
@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService service;

    /**
     * 为当前用户创建新会话。
     *
     * @return 新会话信息
     */
    @PostMapping("/create")
    public ApiResponse<CreateSessionResponse> create() {
        return ApiResponse.success(service.create(SecurityUtils.currentUserId()));
    }

    /**
     * 查询当前用户的全部会话。
     *
     * @return 按更新时间倒序排列的会话
     */
    @GetMapping("/list")
    public ApiResponse<List<SessionView>> list() {
        return ApiResponse.success(service.list(SecurityUtils.currentUserId()));
    }

    /**
     * 分页查询会话消息历史。
     *
     * @param sessionId 会话 ID
     * @param page 页码
     * @param size 每页数量
     * @return 消息分页
     */
    @GetMapping("/{sessionId}/messages")
    public ApiResponse<PageResult<MessageView>> messages(@PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") Long page, @RequestParam(defaultValue = "50") Long size) {
        return ApiResponse.success(service.messages(SecurityUtils.currentUserId(), sessionId, page, size));
    }

    /**
     * 软删除会话及其消息。
     *
     * @param sessionId 会话 ID
     * @return 空成功响应
     */
    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> delete(@PathVariable Long sessionId) {
        service.delete(SecurityUtils.currentUserId(), sessionId);
        return ApiResponse.success();
    }
}

