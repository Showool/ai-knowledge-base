package com.jason.ai.knowledgebase.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 对外 SSE 事件类型。
 */
public enum ChatEventType {
    QUEUED("Queued", false),
    GENERATING("Generating", false),
    DELTA("Delta", false),
    GENERATED("Generated", true),
    FAILED("Failed", true),
    CANCELLED("Cancelled", true);

    private final String wireValue;
    private final boolean terminal;

    ChatEventType(String wireValue, boolean terminal) {
        this.wireValue = wireValue;
        this.terminal = terminal;
    }

    /**
     * 返回保持兼容的 SSE 和 JSON 线值。
     *
     * @return 对外事件名称
     */
    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /**
     * 判断事件是否为协议终态。
     *
     * @return 终态事件返回 true
     */
    public boolean terminal() {
        return terminal;
    }
}