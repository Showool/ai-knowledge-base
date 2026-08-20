package com.jason.ai.knowledgebase.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jason.ai.knowledgebase.common.api.ApiResponse;
import com.jason.ai.knowledgebase.model.request.AuthRequests.LoginRequest;
import com.jason.ai.knowledgebase.model.request.AuthRequests.RefreshRequest;
import com.jason.ai.knowledgebase.model.request.AuthRequests.RegisterRequest;
import com.jason.ai.knowledgebase.model.response.AuthResponses.TokenResponse;
import com.jason.ai.knowledgebase.model.response.AuthResponses.UserView;
import com.jason.ai.knowledgebase.security.SecurityUtils;
import com.jason.ai.knowledgebase.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 用户名密码认证接口。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    /**
     * 注册普通用户。
     *
     * @param request 注册参数
     * @return 用户公开信息
     */
    @PostMapping("/register")
    public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(service.register(request.username(), request.password()));
    }

    /**
     * 校验用户名和密码并创建唯一登录会话。
     *
     * @param request 登录参数
     * @return Access Token 与 Refresh Token
     */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(service.login(request.username(), request.password()));
    }

    /**
     * 轮换 Refresh Token 并签发新的令牌对。
     *
     * @param request 刷新参数
     * @return 新令牌对
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(service.refresh(request.refreshToken()));
    }

    /**
     * 注销当前账号的全部认证会话。
     *
     * @return 空成功响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        service.logout(SecurityUtils.currentUserId());
        return ApiResponse.success();
    }
}
