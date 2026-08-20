package com.jason.ai.knowledgebase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/** 静态 OpenAPI 文档开放配置。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.openapi")
public class OpenApiProperties {
    private boolean enabled = true;
}