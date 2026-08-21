package com.jason.ai.knowledgebase.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.controller.GlobalExceptionHandler;
import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.model.response.PageResult;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiResponseContractTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void allApplicationErrorCodesAreUniqueThreeDigitValues() {
        int[] codes = Arrays.stream(ErrorCode.values()).mapToInt(ErrorCode::code).toArray();

        assertThat(Arrays.stream(codes).allMatch(code -> code >= 100 && code <= 999)).isTrue();
        assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    void successFactoriesReturnSuccessEnvelope() {
        assertThat(ApiResponse.success("payload"))
                .returns(200, ApiResponse::code)
                .returns("OK", ApiResponse::message)
                .returns("payload", ApiResponse::data);
        assertThat(ApiResponse.success().code()).isEqualTo(200);
    }

    @Test
    void pageFactoryExtractsItemsAndTotal() {
        Page<String> page = new Page<>(2, 5, 11);
        page.setRecords(List.of("first", "second"));

        ApiResponse<PageResult<String>> response = ApiResponse.page(page);

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data()).isNotNull();
        assertThat(response.data().items()).containsExactly("first", "second");
        assertThat(response.data().total()).isEqualTo(11);
    }

    @Test
    void applicationAndFrameworkExceptionsKeepHttpStatusOk() {
        assertError(handler.handleAppException(new AppException(ErrorCode.NOT_FOUND)), ErrorCode.NOT_FOUND);
        assertError(handler.handleForbidden(new AccessDeniedException("denied")), ErrorCode.FORBIDDEN);
        assertError(handler.handleDuplicate(new DuplicateKeyException("duplicate")), ErrorCode.CONFLICT);
        assertError(handler.handleUnexpected(new IllegalStateException("unexpected")), ErrorCode.INTERNAL_ERROR);
    }

    @Test
    void validationExceptionUsesThreeDigitCodeWithoutChangingHttpStatus() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "username", "不能为空"));
        var exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("username: 不能为空");
    }

    private void assertError(ResponseEntity<ApiResponse<Void>> response, ErrorCode errorCode) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(errorCode.code());
    }
}