package com.jason.ai.knowledgebase.common.exception;

/** 使用全局唯一三位业务码表示的错误类型。 */
public enum ErrorCode {
    INVALID_ARGUMENT(400, "请求参数不正确"),
    UNAUTHORIZED(401, "未登录或登录已失效"),
    FORBIDDEN(403, "无权执行该操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源状态冲突"),
    USERNAME_EXISTS(410, "用户名已存在"),
    INVALID_CREDENTIALS(402, "用户名或密码错误"),
    QUOTA_EXHAUSTED(429, "可用额度不足"),
    USER_BUSY(411, "当前账号已有活动请求"),
    CAPACITY_FULL(430, "服务繁忙，请稍后重试"),
    MESSAGE_REJECTED(422, "请求内容无效"),
    REQUEST_NOT_ACTIVE(405, "请求不存在或已结束"),
    DEPENDENCY_UNAVAILABLE(503, "依赖服务暂不可用"),
    INTERNAL_ERROR(500, "服务内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /** @return 全局业务码 */
    public int code() {
        return code;
    }

    /** @return 面向客户端的中文错误信息 */
    public String message() {
        return message;
    }
}