package com.jason.ai.knowledgebase.common.api;

import java.time.Instant;

/** 非 SSE 接口的稳定响应信封。 */
public record ApiResponse<T>(int code, String message, T data, Instant timestamp) {

    private static final int SUCCESS_CODE = 200;
    private static final String SUCCESS_MESSAGE = "OK";

    /**
     * 创建包含数据的成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, Instant.now());
    }

    /**
     * 创建不包含数据的成功响应。
     *
     * @return 成功响应
     */
    public static ApiResponse<Void> success() {
        return success(null);
    }

    /**
     * 创建业务失败响应。
     *
     * @param code 全局业务码
     * @param message 错误信息
     * @return 失败响应
     */
    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}