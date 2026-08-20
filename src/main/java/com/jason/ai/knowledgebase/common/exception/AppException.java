package com.jason.ai.knowledgebase.common.exception;

/** 可预期的业务异常。 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 使用错误码默认消息创建业务异常。
     *
     * @param errorCode 业务错误码
     */
    public AppException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义消息创建业务异常。
     *
     * @param errorCode 业务错误码
     * @param message 错误信息
     */
    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 返回对应业务错误码。
     *
     * @return 业务错误码
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}