package com.jason.ai.knowledgebase.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jason.ai.knowledgebase.model.request.AdminRequests.UserPageRequest;
import com.jason.ai.knowledgebase.model.request.PhraseRequests.SaveRequest;
import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.service.AdminUserService;
import com.jason.ai.knowledgebase.service.AuthService;
import com.jason.ai.knowledgebase.service.ChatSessionService;
import com.jason.ai.knowledgebase.service.MeaninglessPhraseService;
import com.jason.ai.knowledgebase.service.QuotaService;

class ServiceResponseContractTest {

    @Test
    void nonSseEndpointServicesReturnApiResponse() throws NoSuchMethodException {
        assertApiResponse(AuthService.class, "register", String.class, String.class);
        assertApiResponse(AuthService.class, "login", String.class, String.class);
        assertApiResponse(AuthService.class, "refresh", String.class);
        assertApiResponse(QuotaService.class, "available", long.class);
        assertApiResponse(AdminUserService.class, "list", UserPageRequest.class);
        assertApiResponse(AdminUserService.class, "get", long.class);
        assertApiResponse(ChatSessionService.class, "create", long.class);
        assertApiResponse(ChatSessionService.class, "list", long.class);
        assertApiResponse(ChatSessionService.class, "messages", long.class, long.class, long.class, long.class);
        assertApiResponse(MeaninglessPhraseService.class, "create", SaveRequest.class);
        assertApiResponse(MeaninglessPhraseService.class, "get", long.class);
        assertApiResponse(MeaninglessPhraseService.class, "list",
                String.class, String.class, Boolean.class, long.class, long.class);
    }

    private void assertApiResponse(Class<?> serviceType, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        assertThat(serviceType.getDeclaredMethod(methodName, parameterTypes).getReturnType())
                .as(serviceType.getSimpleName() + "." + methodName)
                .isEqualTo(ApiResponse.class);
    }
}