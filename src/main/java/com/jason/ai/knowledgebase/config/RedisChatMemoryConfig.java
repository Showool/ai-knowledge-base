package com.jason.ai.knowledgebase.config;

import java.time.Duration;

import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.model.chat.memory.redis.autoconfigure.RedisChatMemoryProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

/** 使用 Spring AI 默认仓库并复用 Spring Data Redis 连接配置。 */
@Configuration
public class RedisChatMemoryConfig {

    @Bean("redisChatMemoryJedisClient")
    RedisClient redisChatMemoryJedisClient(DataRedisProperties redisProperties, ChatProperties properties) {
        DefaultJedisClientConfig clientConfig = createJedisClientConfig(redisProperties);
        ConnectionPoolConfig pool = new ConnectionPoolConfig();
        int maxTotal = properties.getSse().getLlmConcurrency();
        pool.setMaxTotal(maxTotal);
        pool.setMaxIdle(Math.min(16, maxTotal));
        pool.setMinIdle(Math.min(4, maxTotal));
        pool.setMaxWait(Duration.ofSeconds(1));
        pool.setBlockWhenExhausted(true);
        return RedisClient.builder().hostAndPort(redisProperties.getHost(), redisProperties.getPort())
                .clientConfig(clientConfig).poolConfig(pool).build();
    }

    DefaultJedisClientConfig createJedisClientConfig(DataRedisProperties redisProperties) {
        DefaultJedisClientConfig.Builder client = DefaultJedisClientConfig.builder()
                .database(redisProperties.getDatabase())
                .ssl(redisProperties.getSsl().isEnabled())
                .connectionTimeoutMillis(Math.toIntExact(redisProperties.getConnectTimeout().toMillis()))
                .socketTimeoutMillis(Math.toIntExact(redisProperties.getTimeout().toMillis()));
        if (StringUtils.hasText(redisProperties.getPassword())) {
            client.user(StringUtils.hasText(redisProperties.getUsername()) ? redisProperties.getUsername() : null)
                    .password(redisProperties.getPassword());
        }
        return client.build();
    }

    @Bean
    RedisChatMemoryRepository redisChatMemoryRepository(
            @Qualifier("redisChatMemoryJedisClient") RedisClient redisClient,
            RedisChatMemoryProperties properties) {
        RedisChatMemoryRepository.Builder builder = RedisChatMemoryRepository.builder().jedisClient(redisClient);
        if (StringUtils.hasText(properties.getIndexName())) {
            builder.indexName(properties.getIndexName());
        }
        if (StringUtils.hasText(properties.getKeyPrefix())) {
            builder.keyPrefix(properties.getKeyPrefix());
        }
        if (properties.getTimeToLive() != null && !properties.getTimeToLive().isZero()) {
            builder.timeToLive(properties.getTimeToLive());
        }
        if (properties.getInitializeSchema() != null) {
            builder.initializeSchema(properties.getInitializeSchema());
        }
        if (properties.getMaxConversationIds() != null) {
            builder.maxConversationIds(properties.getMaxConversationIds());
        }
        if (properties.getMaxMessagesPerConversation() != null) {
            builder.maxMessagesPerConversation(properties.getMaxMessagesPerConversation());
        }
        if (properties.getMetadataFields() != null && !properties.getMetadataFields().isEmpty()) {
            builder.metadataFields(properties.getMetadataFields());
        }
        return builder.build();
    }
}


