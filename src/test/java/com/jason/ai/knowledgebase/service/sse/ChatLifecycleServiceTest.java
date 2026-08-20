package com.jason.ai.knowledgebase.service.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.jason.ai.knowledgebase.repository.cache.ChatUserLockStore;
import com.jason.ai.knowledgebase.repository.cache.ChatUserLockStore.LockToken;
import com.jason.ai.knowledgebase.model.enums.ChatEventType;
import com.jason.ai.knowledgebase.service.ChatPersistenceService;
import com.jason.ai.knowledgebase.config.AiProperties;
import com.jason.ai.knowledgebase.config.ChatProperties;
import com.jason.ai.knowledgebase.common.util.JsonCodec;
import com.openai.errors.OpenAIIoException;

import tools.jackson.databind.json.JsonMapper;

class ChatLifecycleServiceTest {

    @Test
    void nestedOpenAiIoTimeoutIsGenerationTimeout() {
        InterruptedIOException timeout = new InterruptedIOException("timeout");
        timeout.initCause(new RuntimeException("stream was reset: CANCEL"));
        OpenAIIoException error = new OpenAIIoException("Stream failed", timeout);

        assertThat(ChatLifecycleService.isGenerationTimeout(error)).isTrue();
    }

    @Test
    void generationFailureEmitsGenerationTimeoutTerminalEvent() {
        SseConnectionRegistry registry = mock(SseConnectionRegistry.class);
        ChatGenerationQueue queue = mock(ChatGenerationQueue.class);
        ChatPersistenceService persistence = mock(ChatPersistenceService.class);
        ChatUserLockStore userLockStore = mock(ChatUserLockStore.class);
        JsonCodec jsonCodec = new JsonCodec(JsonMapper.builder().build());
        AiProperties aiProperties = new AiProperties();
        ChatProperties chatProperties = new ChatProperties();
        aiProperties.setModel("test-model");
        chatProperties.getSse().setTerminalRetention(Duration.ofMinutes(1));
        ChatLifecycleService lifecycle = new ChatLifecycleService(registry, queue, persistence, userLockStore,
                aiProperties, chatProperties, jsonCodec);
        SseConnection connection = new SseConnection(1L, 2L, 3L, 4L, 5L, 6,
                new LockToken("lock", "token"), 8);
        OpenAIIoException error = new OpenAIIoException("Stream failed", new InterruptedIOException("timeout"));

        try {
            lifecycle.generationFailed(connection, error);

            var serverEvent = connection.eventFlux().blockFirst(Duration.ofSeconds(1));
            assertThat(serverEvent).isNotNull();
            assertThat(serverEvent.data().event()).isEqualTo(ChatEventType.FAILED);
            assertThat(serverEvent.data().closeReason()).isEqualTo("GENERATION_TIMEOUT");
            assertThat(serverEvent.data().errorCode()).isEqualTo("GENERATION_TIMEOUT");
        } finally {
            lifecycle.closeRetentionExecutor();
        }
    }

    @Test
    void standardTimeoutTypesAreGenerationTimeouts() {
        assertThat(ChatLifecycleService.isGenerationTimeout(new TimeoutException("No signal"))).isTrue();
        assertThat(ChatLifecycleService.isGenerationTimeout(new SocketTimeoutException("Read timed out"))).isTrue();
    }

    @Test
    void nonTimeoutIoFailureRemainsLlmFailure() {
        OpenAIIoException error = new OpenAIIoException("Stream failed", new InterruptedIOException("cancelled"));

        assertThat(ChatLifecycleService.isGenerationTimeout(error)).isFalse();
    }
}
