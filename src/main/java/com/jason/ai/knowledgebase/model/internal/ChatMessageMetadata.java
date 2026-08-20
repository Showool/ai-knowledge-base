package com.jason.ai.knowledgebase.model.internal;

import java.util.List;

/**
 * Assistant 消息的版本化 metadata。
 *
 * @param schemaVersion 结构版本
 * @param model 模型名称
 * @param finishReason 完成或终结原因
 * @param toolCalls Tool 调用摘要
 * @param errorCode 可选错误码
 */
public record ChatMessageMetadata(
        int schemaVersion,
        String model,
        String finishReason,
        List<Object> toolCalls,
        String errorCode) {

    /**
     * 创建当前版本的消息 metadata。
     *
     * @param model 模型名称
     * @param finishReason 完成或终结原因
     * @param errorCode 可选错误码
     * @return metadata
     */
    public static ChatMessageMetadata current(String model, String finishReason, String errorCode) {
        return new ChatMessageMetadata(1, model, finishReason, List.of(), errorCode);
    }
}
