package com.jason.ai.knowledgebase.model.request;

import jakarta.validation.constraints.NotBlank;

/** 认证接口请求参数。 */
public final class AuthRequests {
    private AuthRequests() {
    }

    public record RegisterRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }
}