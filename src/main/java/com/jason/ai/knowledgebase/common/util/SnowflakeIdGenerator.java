package com.jason.ai.knowledgebase.common.util;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.config.SnowflakeProperties;

/** 为单实例服务生成线程安全的 Snowflake BIGINT 标识。 */
@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    private static final int SEQUENCE_BITS = 12;
    private static final int WORKER_BITS = 5;
    private static final int WORKER_SHIFT = SEQUENCE_BITS;
    private static final int DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + 2 * WORKER_BITS;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;

    private final long workerBits;
    private long lastTimestamp = -1L;
    private long sequence;

    /**
     * 使用节点配置创建 ID 生成器。
     *
     * @param properties 节点标识配置
     */
    public SnowflakeIdGenerator(SnowflakeProperties properties) {
        long workerId = properties.getWorkerId();
        long datacenterId = properties.getDatacenterId();
        this.workerBits = (datacenterId << DATACENTER_SHIFT) | (workerId << WORKER_SHIFT);
    }

    /**
     * 生成正数且进程内唯一的业务 ID。
     *
     * @return Snowflake ID
     * @throws IllegalStateException 系统时钟回拨时抛出
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("系统时钟发生回拨");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | workerBits | sequence;
    }

    /**
     * 自旋等待进入下一毫秒，避免同毫秒序列溢出后产生重复 ID。
     *
     * @param previous 已耗尽序列的毫秒时间戳
     * @return 下一毫秒时间戳
     */
    private long waitNextMillis(long previous) {
        long current = System.currentTimeMillis();
        while (current <= previous) {
            Thread.onSpinWait();
            current = System.currentTimeMillis();
        }
        return current;
    }
}