package com.jason.ai.knowledgebase.service;

/** 用于防止删除仍有本地活动请求会话的只读视图。 */
public interface ActiveRequestLookup {
    boolean hasActiveSession(long userId, long sessionId);
}
