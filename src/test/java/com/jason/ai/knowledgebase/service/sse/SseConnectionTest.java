package com.jason.ai.knowledgebase.service.sse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.jason.ai.knowledgebase.repository.cache.ChatUserLockStore.LockToken;

class SseConnectionTest {

    @Test
    void releasesPermitAndOwnedResourcesAtMostOnce() {
        SseConnection connection = new SseConnection(1L, 2L, 3L, 4L, 5L, 6,
                new LockToken("lock", "token"), 8);
        AtomicInteger releaseCount = new AtomicInteger();
        connection.setLlmPermitRelease(releaseCount::incrementAndGet);

        connection.releaseLlmPermit();
        connection.releaseLlmPermit();

        assertThat(releaseCount).hasValue(1);
        assertThat(connection.markResourcesReleased()).isTrue();
        assertThat(connection.markResourcesReleased()).isFalse();
    }
}