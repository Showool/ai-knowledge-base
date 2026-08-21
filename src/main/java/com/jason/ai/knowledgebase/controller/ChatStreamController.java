package com.jason.ai.knowledgebase.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jason.ai.knowledgebase.model.request.ChatRequests.StreamRequest;
import com.jason.ai.knowledgebase.security.SecurityUtils;
import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.model.response.ChatSseEvent;
import com.jason.ai.knowledgebase.service.sse.ChatSseOrchestrator;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import lombok.RequiredArgsConstructor;

/** 真实增量 SSE 生成与取消接口。 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatStreamController {

    private final ChatSseOrchestrator orchestrator;

    /**
     * 受理问题并返回真实增量 SSE 事件流。
     *
     * @param request 会话 ID 与原始问题
     * @return SSE 事件流
     */
    @PostMapping(value = "/sessions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatSseEvent>> stream(@Valid @RequestBody StreamRequest request) {
        long userId = SecurityUtils.currentUserId();
        return orchestrator.start(userId, request.sessionId(), request.message());
    }

    /**
     * 取消当前用户的活动生成请求。
     *
     * @param requestId 请求 ID
     * @return 空成功响应
     */
    @PostMapping("/requests/{requestId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long requestId) {
        orchestrator.cancel(SecurityUtils.currentUserId(), requestId);
        return ApiResponse.success();
    }
}
