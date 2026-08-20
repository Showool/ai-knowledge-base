package com.jason.ai.knowledgebase.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;

import com.jason.ai.knowledgebase.common.exception.ErrorCode;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

class SecurityConfigTest {

    @Test
    void securityExceptionUsesApplicationCodeAndHttpOk() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        ReflectionTestUtils.invokeMethod(new SecurityConfig(), "writeError", response, ErrorCode.UNAUTHORIZED);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertThat(responseBody.toString()).contains("\"code\":401");
    }

    @Test
    void onlyChatStreamAsyncDispatchBypassesSecondAuthorization() {
        RequestMatcher matcher = (RequestMatcher) ReflectionTestUtils.getField(SecurityConfig.class,
                "CHAT_STREAM_ASYNC_DISPATCH");

        assertThat(matcher).isNotNull();
        assertThat(matcher.matches(request("POST", "/api/chat/sessions/stream", DispatcherType.ASYNC))).isTrue();
        assertThat(matcher.matches(request("POST", "/api/chat/sessions/stream", DispatcherType.REQUEST))).isFalse();
        assertThat(matcher.matches(request("POST", "/api/chat/requests/1/cancel", DispatcherType.ASYNC))).isFalse();
        assertThat(matcher.matches(request("GET", "/api/chat/sessions/stream", DispatcherType.ASYNC))).isFalse();
    }

    private MockHttpServletRequest request(String method, String path, DispatcherType dispatcherType) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setDispatcherType(dispatcherType);
        return request;
    }
}
