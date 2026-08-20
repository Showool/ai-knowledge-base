package com.jason.ai.knowledgebase.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 管理员用户管理接口请求参数。 */
public final class AdminRequests {
    private AdminRequests() {
    }

    public record UserPageRequest(String username, Long page, Long size) {
    }

    public record UserStatusRequest(
            @NotNull Long id,
            @NotBlank @Pattern(regexp = "ENABLED|DISABLED", message = "账号状态必须为 ENABLED 或 DISABLED")
            String status) {
    }
}