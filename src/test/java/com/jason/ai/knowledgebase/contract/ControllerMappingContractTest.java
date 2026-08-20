package com.jason.ai.knowledgebase.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jason.ai.knowledgebase.controller.AdminUserController;
import com.jason.ai.knowledgebase.controller.ChatSessionController;
import com.jason.ai.knowledgebase.model.request.AdminRequests.UserPageRequest;
import com.jason.ai.knowledgebase.model.request.AdminRequests.UserStatusRequest;

class ControllerMappingContractTest {

    @Test
    void everyControllerEndpointDeclaresANonEmptyMethodPath() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        var controllers = scanner.findCandidateComponents("com.jason.ai.knowledgebase");
        assertThat(controllers).isNotEmpty();

        for (BeanDefinition controller : controllers) {
            Class<?> controllerType = Class.forName(Objects.requireNonNull(controller.getBeanClassName()));
            for (Method method : controllerType.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                assertThat(Stream.concat(Arrays.stream(mapping.path()), Arrays.stream(mapping.value())))
                        .as("%s#%s must declare a non-empty method-level path",
                                controllerType.getSimpleName(), method.getName())
                        .anyMatch(pathValue -> pathValue != null && !pathValue.isBlank());
            }
        }
    }

    @Test
    void adminUserListUsesPostWithAJsonBody() throws NoSuchMethodException {
        Method method = AdminUserController.class.getDeclaredMethod("list", UserPageRequest.class);
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.method()).containsExactly(RequestMethod.POST);
        assertThat(method.getParameters()[0].isAnnotationPresent(RequestBody.class)).isTrue();
    }

    @Test
    void adminUserStatusUsesPutWithAJsonBody() throws NoSuchMethodException {
        Method method = AdminUserController.class.getDeclaredMethod("updateStatus", UserStatusRequest.class);
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.method()).containsExactly(RequestMethod.PUT);
        assertThat(method.getParameters()[0].isAnnotationPresent(RequestBody.class)).isTrue();
    }

    @Test
    void chatSessionListHasNoRequestParameters() throws NoSuchMethodException {
        Method method = ChatSessionController.class.getDeclaredMethod("list");

        assertThat(method.getParameterCount()).isZero();
    }
}



