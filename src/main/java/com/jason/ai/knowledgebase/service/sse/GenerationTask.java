package com.jason.ai.knowledgebase.service.sse;

/** 不可变的 FIFO 任务项。 */
public record GenerationTask(SseConnection connection, String message) {
}
