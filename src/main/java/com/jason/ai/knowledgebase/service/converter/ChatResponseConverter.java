package com.jason.ai.knowledgebase.service.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.jason.ai.knowledgebase.common.util.UnicodeText;
import com.jason.ai.knowledgebase.model.entity.ChatSession;
import com.jason.ai.knowledgebase.model.entity.ConversationMessage;
import com.jason.ai.knowledgebase.model.response.ChatResponses.CreateSessionResponse;
import com.jason.ai.knowledgebase.model.response.ChatResponses.MessageView;
import com.jason.ai.knowledgebase.model.response.ChatResponses.SessionView;
import com.jason.ai.knowledgebase.repository.projection.ChatSessionSummary;

/** 会话与消息响应转换器。 */
@Mapper(config = MapStructConfiguration.class, uses = DatabaseJsonConverter.class)
public interface ChatResponseConverter {

    int MESSAGE_PREVIEW_LENGTH = 80;

    /** 将会话实体转换为创建会话响应。 */
    @Mapping(target = "sessionId", source = "id")
    CreateSessionResponse toCreateResponse(ChatSession session);

    /** 将会话查询投影转换为列表视图，并截断消息预览。 */
    @Mapping(target = "lastMessagePreview", source = "lastMessagePreview",
            qualifiedByName = "truncateMessagePreview")
    SessionView toSessionView(ChatSessionSummary session);

    /** 将消息实体转换为历史消息视图，并解析 metadata JSON。 */
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "readMetadata")
    MessageView toMessageView(ConversationMessage message);

    /** 按对外契约的 Unicode code point 上限截断消息预览。 */
    @Named("truncateMessagePreview")
    default String truncateMessagePreview(String preview) {
        return UnicodeText.truncate(preview, MESSAGE_PREVIEW_LENGTH);
    }
}
