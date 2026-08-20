package com.jason.ai.knowledgebase.repository.mapper;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jason.ai.knowledgebase.model.entity.ChatSession;
import com.jason.ai.knowledgebase.repository.projection.ChatSessionSummary;

/**
 * 会话持久化操作。
 */
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 一次查询返回用户全部会话及各自最新消息内容。
     *
     * @param userId 用户 ID
     * @return 按更新时间和 ID 倒序排列的会话投影
     */
    @Select("""
            SELECT s.id,
                   s.title,
                   s.create_time,
                   s.update_time,
                   (
                       SELECT m.content
                       FROM ai_conversation_message m
                       WHERE m.session_id = s.id AND m.deleted = 0
                       ORDER BY m.id DESC
                       LIMIT 1
                   ) AS last_message_preview
            FROM ai_chat_session s
            WHERE s.user_id = #{userId} AND s.deleted = 0
            ORDER BY s.update_time DESC, s.id DESC
            """)
    List<ChatSessionSummary> findSummaries(@Param("userId") long userId);

    /**
     * 仅在会话尚无 User 消息时初始化标题。
     */
    @Update("UPDATE ai_chat_session s SET s.title = #{title}, s.update_time = #{now} "
            + "WHERE s.id = #{sessionId} AND s.user_id = #{userId} AND s.deleted = 0 "
            + "AND NOT EXISTS (SELECT 1 FROM ai_conversation_message m WHERE m.session_id = s.id "
            + "AND m.role = 'USER' AND m.deleted = 0)")
    int initializeTitle(@Param("sessionId") long sessionId, @Param("userId") long userId,
            @Param("title") String title, @Param("now") Instant now);

    /**
     * 软删除用户拥有的会话。
     */
    @Update("UPDATE ai_chat_session SET deleted = 1, update_time = #{now} "
            + "WHERE id = #{sessionId} AND user_id = #{userId} AND deleted = 0")
    int softDeleteOwned(@Param("sessionId") long sessionId, @Param("userId") long userId,
            @Param("now") Instant now);

    /**
     * 更新会话最后活动时间。
     */
    @Update("UPDATE ai_chat_session SET update_time = #{now} WHERE id = #{sessionId} AND deleted = 0")
    int touch(@Param("sessionId") long sessionId, @Param("now") Instant now);
}
