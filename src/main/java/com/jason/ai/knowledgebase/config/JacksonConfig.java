package com.jason.ai.knowledgebase.config;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 保持 JavaScript 安全的 Long 序列化，并沿用 Boot 的 UTC 时间配置。 */
@Configuration
public class JacksonConfig {

    private static final ValueDeserializer<Long> LONG_DESERIALIZER = new ValueDeserializer<Long>() {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) {
            String value = parser.getValueAsString();
            return value == null ? null : Long.parseLong(value);
        }
    };

    /** 仅注册项目所需的 Long 数字兼容规则。 */
    @Bean
    JsonMapperBuilderCustomizer jacksonCustomizer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addDeserializer(Long.class, LONG_DESERIALIZER);
        module.addDeserializer(Long.TYPE, LONG_DESERIALIZER);
        return builder -> builder.addModule(module);
    }
}