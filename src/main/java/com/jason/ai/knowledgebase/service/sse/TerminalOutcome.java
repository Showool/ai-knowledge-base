package com.jason.ai.knowledgebase.service.sse;

import com.jason.ai.knowledgebase.model.enums.ChatCloseReason;
import com.jason.ai.knowledgebase.model.enums.ChatEventType;
import com.jason.ai.knowledgebase.model.enums.ChatFailureCode;
import com.jason.ai.knowledgebase.model.enums.ChatRequestState;
import com.jason.ai.knowledgebase.model.enums.MessageStatus;

/**
 * 对话请求终态所需的全部类型化信息。
 *
 * @param state JVM 请求终态
 * @param eventType 对外事件类型；客户端断开时为空
 * @param messageStatus MySQL 消息状态
 * @param closeReason 终结原因
 * @param errorCode 可选错误码
 * @param disposeSubscription 是否中止模型订阅
 */
record TerminalOutcome(
        ChatRequestState state,
        ChatEventType eventType,
        MessageStatus messageStatus,
        ChatCloseReason closeReason,
        ChatFailureCode errorCode,
        boolean disposeSubscription) {

    static TerminalOutcome completed() {
        return new TerminalOutcome(ChatRequestState.COMPLETED, ChatEventType.GENERATED,
                MessageStatus.COMPLETED, ChatCloseReason.COMPLETED, null, false);
    }

    static TerminalOutcome generationFailure(ChatCloseReason reason, ChatFailureCode errorCode) {
        return new TerminalOutcome(ChatRequestState.FAILED, ChatEventType.FAILED,
                MessageStatus.FAILED, reason, errorCode, true);
    }

    static TerminalOutcome admissionFailure() {
        return new TerminalOutcome(ChatRequestState.FAILED, ChatEventType.FAILED,
                MessageStatus.FAILED, ChatCloseReason.ENQUEUE_FAILED, ChatFailureCode.INTERNAL_ERROR, false);
    }

    static TerminalOutcome queueTimeout() {
        return generationFailure(ChatCloseReason.QUEUE_TIMEOUT, ChatFailureCode.QUEUE_TIMEOUT);
    }

    static TerminalOutcome cancelled() {
        return new TerminalOutcome(ChatRequestState.CANCELLED, ChatEventType.CANCELLED,
                MessageStatus.CANCELLED, ChatCloseReason.USER_CANCELLED, null, true);
    }

    static TerminalOutcome disconnected() {
        return new TerminalOutcome(ChatRequestState.CLIENT_DISCONNECTED, null,
                MessageStatus.CLIENT_DISCONNECTED, ChatCloseReason.CLIENT_DISCONNECTED, null, true);
    }

    static TerminalOutcome maxLifetime() {
        return generationFailure(ChatCloseReason.MAX_LIFETIME, ChatFailureCode.MAX_LIFETIME);
    }

    static TerminalOutcome shutdown() {
        return generationFailure(ChatCloseReason.SERVER_SHUTDOWN, ChatFailureCode.SERVER_SHUTDOWN);
    }
}
