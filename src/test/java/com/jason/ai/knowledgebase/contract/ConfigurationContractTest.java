package com.jason.ai.knowledgebase.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class ConfigurationContractTest {

    @Test
    void baseConfigurationUsesEffectiveSpringAiProperties() throws IOException {
        MockEnvironment environment = load("application.yml");

        assertThat(environment.getProperty("spring.ai.retry.max-attempts")).isEqualTo("3");
        assertThat(environment.getProperty("spring.ai.openai.max-retries")).isNull();
        assertThat(environment.getProperty("spring.ai.openai.chat.timeout")).isEqualTo("150s");
        assertThat(environment.getProperty("app.chat.message-max-length")).isEqualTo("256");
        assertThat(environment.getProperty("spring.ai.chat.memory.redis.max-messages-per-conversation"))
                .isEqualTo("5");
        assertThat(environment.containsProperty("spring.data.redis.host")).isTrue();
        assertThat(environment.containsProperty("spring.data.redis.port")).isTrue();
        assertThat(environment.containsProperty("spring.data.redis.password")).isTrue();
        assertThat(environment.getProperty("spring.ai.chat.memory.redis.host")).isNull();
        assertThat(environment.getProperty("spring.ai.chat.memory.redis.port")).isNull();
        assertThat(environment.getProperty("spring.ai.chat.memory.redis.password")).isNull();
    }

    @Test
    void profileConfigurationsOnlyOverrideOpenApiAvailability() throws IOException {
        assertProfileContainsOnlyOpenApiSetting("application-dev.yml", "true");
        assertProfileContainsOnlyOpenApiSetting("application-test.yml", "true");
        assertProfileContainsOnlyOpenApiSetting("application-prod.yml", "false");
    }

    private void assertProfileContainsOnlyOpenApiSetting(String name, String expected) throws IOException {
        PropertySource<?> source = new YamlPropertySourceLoader()
                .load(name, new ClassPathResource(name))
                .getFirst();

        assertThat(source).isInstanceOf(org.springframework.core.env.EnumerablePropertySource.class);
        assertThat(((org.springframework.core.env.EnumerablePropertySource<?>) source).getPropertyNames())
                .containsExactly("app.openapi.enabled");

        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(source);
        assertThat(environment.getProperty("app.openapi.enabled")).isEqualTo(expected);
    }

    private MockEnvironment load(String name) throws IOException {
        MockEnvironment environment = new MockEnvironment();
        for (PropertySource<?> source : new YamlPropertySourceLoader().load(name, new ClassPathResource(name))) {
            environment.getPropertySources().addLast(source);
        }
        return environment;
    }
}



