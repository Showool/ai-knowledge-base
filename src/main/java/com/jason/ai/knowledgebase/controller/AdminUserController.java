package com.jason.ai.knowledgebase.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jason.ai.knowledgebase.common.api.ApiResponse;
import com.jason.ai.knowledgebase.common.api.PageResult;
import com.jason.ai.knowledgebase.model.response.AdminResponses.UserListItem;
import com.jason.ai.knowledgebase.model.request.AdminRequests.UserPageRequest;
import com.jason.ai.knowledgebase.model.request.AdminRequests.UserStatusRequest;
import com.jason.ai.knowledgebase.model.response.AdminResponses.UserView;
import com.jason.ai.knowledgebase.service.AdminUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 仅管理员可用的账号管理接口。 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    /**
     * 分页查询用户，可按用户名模糊过滤。
     *
     * @param request 分页与过滤参数
     * @return 用户分页
     */
    @PostMapping("/list")
    public ApiResponse<PageResult<UserListItem>> list(@Valid @RequestBody UserPageRequest request) {
        return ApiResponse.success(service.list(request));
    }

    /**
     * 按 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public ApiResponse<UserView> get(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    /**
     * 幂等更新账号状态；停用时同步注销认证会话。
     *
     * @param request 用户 ID 与目标状态
     * @return 空成功响应
     */
    @PutMapping("/status")
    public ApiResponse<Void> updateStatus(@Valid @RequestBody UserStatusRequest request) {
        service.updateStatus(request.id(), request.status());
        return ApiResponse.success();
    }
}

