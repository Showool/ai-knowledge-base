package com.jason.ai.knowledgebase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/** Snowflake 节点标识配置。 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.snowflake")
public class SnowflakeProperties {
    @Min(0)
    @Max(31)
    private long workerId = 1;
    @Min(0)
    @Max(31)
    private long datacenterId = 1;
}