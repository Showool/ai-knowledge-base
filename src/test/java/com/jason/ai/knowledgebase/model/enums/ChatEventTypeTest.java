package com.jason.ai.knowledgebase.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class ChatEventTypeTest {

    @Test
    void serializesUsingStableWireValue() {
        String json = JsonMapper.builder().build().writeValueAsString(ChatEventType.QUEUED);

        assertThat(json).isEqualTo("\"Queued\"");
        assertThat(ChatEventType.GENERATED.terminal()).isTrue();
        assertThat(ChatEventType.DELTA.terminal()).isFalse();
    }
}