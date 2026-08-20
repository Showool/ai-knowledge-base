package com.jason.ai.knowledgebase.model.enums;

/**
 * 账号角色。
 */
public enum UserRole {
    USER,
    ADMIN;

    /**
     * 返回 Spring Security 使用的权限名称。
     *
     * @return 带有 ROLE_ 前缀的权限名称
     */
    public String authority() {
        return "ROLE_" + name();
    }
}
