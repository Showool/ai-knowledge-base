package com.jason.ai.knowledgebase.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageBoundsTest {

    @Test
    void normalizesNullAndOutOfRangeValues() {
        assertThat(PageBounds.of(null, null, 20, 100)).isEqualTo(new PageBounds(1, 20));
        assertThat(PageBounds.of(0L, 0L, 20, 100)).isEqualTo(new PageBounds(1, 1));
        assertThat(PageBounds.of(2L, 101L, 20, 100)).isEqualTo(new PageBounds(2, 100));
    }

    @Test
    void normalizesPrimitiveValues() {
        assertThat(PageBounds.of(-1, 500, 100)).isEqualTo(new PageBounds(1, 100));
    }
}