package com.jason.ai.knowledgebase.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class JacksonConfigTest {

    @Test
    void serializesAndDeserializesLongAsString() {
        JsonMapper.Builder builder = JsonMapper.builder();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        JsonMapper mapper = builder.build();

        LongPayload payload = new LongPayload(9_223_372_036_854_775_807L);

        assertThat(mapper.writeValueAsString(payload))
                .isEqualTo("{\"id\":\"9223372036854775807\"}");
        assertThat(mapper.readValue("{\"id\":\"9223372036854775807\"}", LongPayload.class).id())
                .isEqualTo(Long.MAX_VALUE);
        assertThat(mapper.readValue("{\"id\":9223372036854775807}", LongPayload.class).id())
                .isEqualTo(Long.MAX_VALUE);
    }

    private record LongPayload(Long id) {
    }
}