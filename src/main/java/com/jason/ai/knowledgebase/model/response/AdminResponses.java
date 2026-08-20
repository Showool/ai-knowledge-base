package com.jason.ai.knowledgebase.model.response;

import java.time.Instant;

/** 管理员用户管理接口响应数据。 */
public final class AdminResponses {
    private AdminResponses() {
    }

    public record UserListItem(String id, String username, String role, String status,
            Instant createTime, Instant updateTime) {
    }

    public record UserView(String username, String role, String status, Instant createTime, Instant updateTime) {
    }
}