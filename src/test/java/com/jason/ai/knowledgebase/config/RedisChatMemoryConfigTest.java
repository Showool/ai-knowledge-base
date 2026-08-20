package com.jason.ai.knowledgebase.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;

class RedisChatMemoryConfigTest {

    @Test
    void chatMemoryClientUsesSpringDataRedisConnectionSettings() {
        DataRedisProperties redisProperties = new DataRedisProperties();
        redisProperties.setDatabase(3);
        redisProperties.setUsername("chat-user");
        redisProperties.setPassword("chat-password");
        redisProperties.setConnectTimeout(Duration.ofSeconds(2));
        redisProperties.setTimeout(Duration.ofSeconds(4));
        redisProperties.getSsl().setEnabled(true);

        var clientConfig = new RedisChatMemoryConfig().createJedisClientConfig(redisProperties);

        assertThat(clientConfig.getDatabase()).isEqualTo(3);
        assertThat(clientConfig.getUser()).isEqualTo("chat-user");
        assertThat(clientConfig.getPassword()).isEqualTo("chat-password");
        assertThat(clientConfig.getConnectionTimeoutMillis()).isEqualTo(2_000);
        assertThat(clientConfig.getSocketTimeoutMillis()).isEqualTo(4_000);
        assertThat(clientConfig.isSsl()).isTrue();
    }
}
