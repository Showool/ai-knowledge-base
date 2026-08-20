package com.jason.ai.knowledgebase.model.response;

/** 认证接口响应数据。 */
public final class AuthResponses {
    private AuthResponses() {
    }

    public record UserView(Long id, String username, String role) {
    }

    public record TokenResponse(String tokenType, String accessToken, Long accessExpiresInSeconds,
            String refreshToken, Long refreshExpiresInSeconds, UserView user) {
    }
}