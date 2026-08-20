package com.jason.ai.knowledgebase.model.enums;

/**
 * 确定性规则拒绝请求的原因。
 */
public enum AdmissionRejectReason {
    EMPTY,
    CONTROL_CHARACTERS,
    URL_ONLY,
    PURE_EMOJI,
    PURE_NUMBER,
    PURE_PUNCTUATION,
    GARBLED_TEXT,
    REPEATED_CHARACTERS,
    PLACEHOLDER
}
