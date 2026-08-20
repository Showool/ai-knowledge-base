package com.jason.ai.knowledgebase.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.jason.ai.knowledgebase.config.SnowflakeProperties;



class SnowflakeIdGeneratorTest {

    @Test
    void createsPositiveUniqueIds() {
        SnowflakeProperties properties = new SnowflakeProperties();
        properties.setDatacenterId(1);
        properties.setWorkerId(1);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(properties);

        Set<Long> ids = new HashSet<>();
        for (int index = 0; index < 10_000; index++) {
            ids.add(generator.nextId());
        }

        assertThat(ids).hasSize(10_000).allMatch(id -> id > 0);
    }
}
