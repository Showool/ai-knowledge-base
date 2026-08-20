package com.jason.ai.knowledgebase.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 认证与令牌生命周期配置。 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    @NotBlank
    private String issuer = "ai-knowledge-base";
    @NotBlank
    private String jwtSecret;
    @NotNull
    private Duration accessTokenTtl = Duration.ofMinutes(30);
    @NotNull
    private Duration refreshTokenTtl = Duration.ofDays(30);
}