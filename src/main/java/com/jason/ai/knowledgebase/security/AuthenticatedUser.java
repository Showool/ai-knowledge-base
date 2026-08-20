package com.jason.ai.knowledgebase.security;

/** 异步 SSE 开始前复制的不可变认证主体。 */
public record AuthenticatedUser(long userId, String username, String role, long authSessionId) {
}
