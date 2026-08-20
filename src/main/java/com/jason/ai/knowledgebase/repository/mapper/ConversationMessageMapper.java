package com.jason.ai.knowledgebase.repository.mapper;

import java.time.Instant;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jason.ai.knowledgebase.model.entity.ConversationMessage;

public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
    @Update("UPDATE ai_conversation_message SET content = #{content}, status = #{status}, "
            + "metadata = #{metadata}, update_time = #{now} WHERE id = #{messageId} AND role = 'ASSISTANT' AND deleted = 0")
    int finishAssistant(@Param("messageId") long messageId, @Param("content") String content,
            @Param("status") String status, @Param("metadata") String metadata, @Param("now") Instant now);

    @Update("UPDATE ai_conversation_message SET deleted = 1, update_time = #{now} "
            + "WHERE session_id = #{sessionId} AND user_id = #{userId} AND deleted = 0")
    int softDeleteBySession(@Param("sessionId") long sessionId, @Param("userId") long userId,
            @Param("now") Instant now);
}
