package com.jason.ai.knowledgebase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 模型调用与对话记忆配置。 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    @NotBlank
    private String systemPrompt;
    @Min(1)
    private int memoryWindowSize = 5;
    @NotBlank
    private String model;
}