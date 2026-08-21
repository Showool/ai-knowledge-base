package com.jason.ai.knowledgebase.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.jason.ai.knowledgebase.controller.AdminUserController;
import com.jason.ai.knowledgebase.controller.ApiDocumentationController;
import com.jason.ai.knowledgebase.controller.AuthController;
import com.jason.ai.knowledgebase.controller.ChatSessionController;
import com.jason.ai.knowledgebase.controller.ChatStreamController;
import com.jason.ai.knowledgebase.controller.MeaninglessPhraseAdminController;
import com.jason.ai.knowledgebase.controller.QuotaController;
import com.jason.ai.knowledgebase.model.request.AdminRequests;
import com.jason.ai.knowledgebase.model.request.AuthRequests;
import com.jason.ai.knowledgebase.model.request.ChatRequests;
import com.jason.ai.knowledgebase.model.request.PhraseRequests;
import com.jason.ai.knowledgebase.model.response.AdminResponses;
import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.model.response.AuthResponses;
import com.jason.ai.knowledgebase.model.response.ChatResponses;
import com.jason.ai.knowledgebase.model.response.PhraseResponses;
import com.jason.ai.knowledgebase.model.response.QuotaResponses;
import com.jason.ai.knowledgebase.model.response.ChatSseEvent;
import com.jason.ai.knowledgebase.model.response.PageResult;

class ApiLongTypeContractTest {

    @Test
    void apiContractsUseBoxedLongInsteadOfPrimitiveLong() {
        List<Class<?>> containerTypes = List.of(AdminRequests.class, AuthRequests.class, ChatRequests.class,
                PhraseRequests.class, AdminResponses.class, AuthResponses.class, ChatResponses.class,
                PhraseResponses.class, QuotaResponses.class);
        List<Class<?>> standaloneRecords = List.of(ApiResponse.class, PageResult.class, ChatSseEvent.class);

        List<String> primitiveRecordComponents = Stream.concat(
                containerTypes.stream().flatMap(type -> Arrays.stream(type.getDeclaredClasses())),
                standaloneRecords.stream())
                .filter(Class::isRecord)
                .flatMap(type -> Arrays.stream(type.getRecordComponents())
                        .map(component -> type.getSimpleName() + "." + component.getName()
                                + ":" + component.getType().getName()))
                .filter(component -> component.endsWith(":" + long.class.getName()))
                .toList();

        assertThat(primitiveRecordComponents).isEmpty();
    }

    @Test
    void pageResultOnlyExposesItemsAndTotal() {
        assertThat(Arrays.stream(PageResult.class.getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly("items", "total");
    }

    @Test
    void controllerMethodParametersUseBoxedLongInsteadOfPrimitiveLong() {
        List<Class<?>> controllerTypes = List.of(AdminUserController.class, ApiDocumentationController.class,
                AuthController.class, ChatSessionController.class, ChatStreamController.class,
                MeaninglessPhraseAdminController.class, QuotaController.class);

        List<String> primitiveParameters = controllerTypes.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods())
                        .flatMap(method -> Arrays.stream(method.getParameterTypes())
                                .map(parameterType -> type.getSimpleName() + "." + method.getName()
                                        + ":" + parameterType.getName())))
                .filter(parameter -> parameter.endsWith(":" + long.class.getName()))
                .toList();

        assertThat(primitiveParameters).isEmpty();
    }
}