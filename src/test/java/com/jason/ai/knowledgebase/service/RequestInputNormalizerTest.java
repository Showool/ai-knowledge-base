package com.jason.ai.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jason.ai.knowledgebase.model.internal.NormalizedInput;

class RequestInputNormalizerTest {

    private final RequestInputNormalizer normalizer = new RequestInputNormalizer();

    @Test
    void normalizesUnicodeWhitespaceAndRepeatedPunctuation() {
        NormalizedInput result = normalizer.normalize("  Ｈｅｌｌｏ！！！   WORLD  ");

        assertThat(result.original()).isEqualTo("  Ｈｅｌｌｏ！！！   WORLD  ");
        assertThat(result.normalized()).isEqualTo("Hello! WORLD");
        assertThat(result.comparable()).isEqualTo("hello! world");
    }

    @Test
    void preservesBothSlashesInHttpScheme() {
        assertThat(normalizer.normalize("https://example.com").normalized())
                .isEqualTo("https://example.com");
    }
}
