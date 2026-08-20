package com.jason.ai.knowledgebase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import lombok.Getter;
import lombok.Setter;

/** 请求准入与缓存降级策略配置。 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.request-admission")
public class AdmissionProperties {
    private boolean enabled = true;
    private boolean failOpen;
}