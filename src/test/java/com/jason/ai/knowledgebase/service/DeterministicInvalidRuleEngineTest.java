package com.jason.ai.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import com.jason.ai.knowledgebase.model.enums.AdmissionRejectReason;

class DeterministicInvalidRuleEngineTest {

    private final RequestInputNormalizer normalizer = new RequestInputNormalizer();
    private final DeterministicInvalidRuleEngine engine = new DeterministicInvalidRuleEngine();

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            https://example.com | URL_ONLY
            12345               | PURE_NUMBER
            ！！！               | PURE_PUNCTUATION
            😀😀                 | PURE_EMOJI
            aaaaaaaaa           | REPEATED_CHARACTERS
            undefined           | PLACEHOLDER
            """)
    void blocksDeterministicInvalidInputs(String text, AdmissionRejectReason reason) {
        assertThat(engine.evaluate(normalizer.normalize(text))).contains(reason);
    }

    @Test
    void allowsOrdinaryQuestion() {
        assertThat(engine.evaluate(normalizer.normalize("请解释什么是向量数据库"))).isEmpty();
    }
}
