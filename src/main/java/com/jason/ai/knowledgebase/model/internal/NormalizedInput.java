package com.jason.ai.knowledgebase.model.internal;

/** 同时保留展示原文、规范化文本和比较文本的请求形式。 */
public record NormalizedInput(String original, String normalized, String comparable) {
}
