package com.jason.ai.knowledgebase.config;



import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** 配置中立的 OpenAI ChatClient 与默认 Redis 记忆 Advisor。 */
@Configuration
public class AiConfig {

    @Bean
    ChatMemory chatMemory(RedisChatMemoryRepository repository, AiProperties properties) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(repository).maxMessages(properties.getMemoryWindowSize()).build();
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, AiProperties properties) {
        return ChatClient.builder(chatModel).defaultSystem(properties.getSystemPrompt())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), SimpleLoggerAdvisor.builder().order(Ordered.LOWEST_PRECEDENCE).build())
                .build();
    }
}
