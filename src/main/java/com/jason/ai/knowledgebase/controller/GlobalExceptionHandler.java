package com.jason.ai.knowledgebase.controller;

import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.model.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/** 将框架与业务异常统一映射为 HTTP 200 和业务错误码。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理可预期业务异常。
     *
     * @param exception 业务异常
     * @return 业务错误响应
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
        ErrorCode error = exception.errorCode();
        return ResponseEntity.ok(ApiResponse.error(error.code(), exception.getMessage()));
    }

    /**
     * 聚合请求体字段校验错误。
     *
     * @param exception 参数校验异常
     * @return 参数错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(ApiResponse.error(ErrorCode.INVALID_ARGUMENT.code(), message));
    }

    /**
     * 处理授权拒绝异常。
     *
     * @param exception 授权拒绝异常
     * @return 无权限响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException exception) {
        return ResponseEntity.ok(ApiResponse.error(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message()));
    }

    /**
     * 将数据库唯一键冲突转换为统一业务错误。
     *
     * @param exception 唯一键冲突异常
     * @return 资源冲突响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateKeyException exception) {
        return ResponseEntity.ok(ApiResponse.error(ErrorCode.CONFLICT.code(), ErrorCode.CONFLICT.message()));
    }

    /**
     * 兜底处理未识别异常，并仅记录异常类型和堆栈。
     *
     * @param exception 未识别异常
     * @return 内部错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("unexpected_request_failure type={}", exception.getClass().getSimpleName(), exception);
        return ResponseEntity.ok(ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message()));
    }
}