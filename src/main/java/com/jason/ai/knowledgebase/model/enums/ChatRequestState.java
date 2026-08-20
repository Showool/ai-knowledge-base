package com.jason.ai.knowledgebase.model.enums;

/**
 * 仅保存在当前 JVM Registry 中的请求状态。
 */
public enum ChatRequestState {
    QUEUED(false),
    GENERATING(false),
    COMPLETED(true),
    FAILED(true),
    CANCELLED(true),
    CLIENT_DISCONNECTED(true);

    private final boolean terminal;

    ChatRequestState(boolean terminal) {
        this.terminal = terminal;
    }

    /**
     * 判断请求是否已经进入终态。
     *
     * @return 终态返回 true
     */
    public boolean terminal() {
        return terminal;
    }
}
