package com.jason.ai.knowledgebase.controller;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

/** 向 Knife4j 提供受版本控制的静态 OpenAPI 契约。 */
@RestController
@ConditionalOnProperty(name = "app.openapi.enabled", havingValue = "true")
public class ApiDocumentationController {

    private static final String GROUP_NAME = "AI-Knowledge-Base";
    private final String openApiJson;

    /**
     * 启动时读取并校验静态 OpenAPI JSON。
     *
     * @throws Exception 资源不存在或 JSON 无法解析时抛出
     */
    public ApiDocumentationController() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        try (InputStream input = new ClassPathResource("openapi/ai-knowledge-base-openapi.json").getInputStream()) {
            this.openApiJson = mapper.writeValueAsString(mapper.readTree(input));
        }
    }

    /**
     * 返回受版本控制的 OpenAPI 契约。
     *
     * @return OpenAPI JSON
     */
    @GetMapping(value = { "/api/v3/api-docs", "/api/v3/api-docs/AI-Knowledge-Base",
            "/v3/api-docs", "/v3/api-docs/AI-Knowledge-Base" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> openApi() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(openApiJson);
    }

    /**
     * 返回 Knife4j 所需的 Swagger UI 配置。
     *
     * @param request 当前 HTTP 请求
     * @return Swagger UI 配置
     */
    @GetMapping(value = { "/api/v3/api-docs/swagger-config", "/v3/api-docs/swagger-config" },
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> swaggerConfig(HttpServletRequest request) {
        String apiDocsUrl = request.getContextPath() + "/api/v3/api-docs/" + GROUP_NAME;
        return Map.of("configUrl", request.getContextPath() + "/api/v3/api-docs/swagger-config",
                "url", apiDocsUrl, "urls", List.of(Map.of("name", GROUP_NAME, "url", apiDocsUrl)),
                "validatorUrl", "");
    }
}
