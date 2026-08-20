package com.jason.ai.knowledgebase.model.enums;

import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;

/**
 * 账号状态。
 */
public enum UserStatus {
    ENABLED,
    DISABLED;

    /**
     * 将接口字符串转换为账号状态。
     *
     * @param value 接口传入的状态值
     * @return 对应的账号状态
     * @throws AppException 状态为空或不受支持时抛出
     */
    public static UserStatus parse(String value) {
        try {
            return UserStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "账号状态必须为 ENABLED 或 DISABLED");
        }
    }
}
