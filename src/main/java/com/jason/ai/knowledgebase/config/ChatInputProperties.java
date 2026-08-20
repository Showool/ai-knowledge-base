package com.jason.ai.knowledgebase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/** 用户问题长度等输入边界配置，沿用 app.chat 配置前缀。 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.chat")
public class ChatInputProperties {

    @Min(1)
    private int messageMaxLength = 256;
}