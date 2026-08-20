package com.jason.ai.knowledgebase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiKnowledgeBaseApplicationTests {

    @Test
    void applicationEntryPointIsAvailable() {
        assertThat(AiKnowledgeBaseApplication.class).isNotNull();
    }
}
