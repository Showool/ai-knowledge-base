package com.jason.ai.knowledgebase;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.jason.ai.knowledgebase.config.AuthProperties;
import com.jason.ai.knowledgebase.config.AdmissionProperties;
import com.jason.ai.knowledgebase.config.ChatInputProperties;
import com.jason.ai.knowledgebase.config.AiProperties;
import com.jason.ai.knowledgebase.config.ChatProperties;
import com.jason.ai.knowledgebase.config.SnowflakeProperties;
import com.jason.ai.knowledgebase.config.OpenApiProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@MapperScan("com.jason.ai.knowledgebase.repository.mapper")
@EnableConfigurationProperties({ AuthProperties.class, AdmissionProperties.class, ChatInputProperties.class, AiProperties.class,
        ChatProperties.class, SnowflakeProperties.class, OpenApiProperties.class })
public class AiKnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiKnowledgeBaseApplication.class, args);
        log.info("AI KnowledgeBase started successfully!");
    }

}
