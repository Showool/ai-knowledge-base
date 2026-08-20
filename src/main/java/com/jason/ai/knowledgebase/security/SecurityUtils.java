package com.jason.ai.knowledgebase.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;

/** 读取当前请求线程中的认证主体。 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前已认证用户。
     *
     * @return 已认证用户
     * @throws AppException 当前请求未认证时抛出
     */
    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * 获取当前已认证用户 ID。
     *
     * @return 用户 ID
     * @throws AppException 当前请求未认证时抛出
     */
    public static long currentUserId() {
        return currentUser().userId();
    }
}