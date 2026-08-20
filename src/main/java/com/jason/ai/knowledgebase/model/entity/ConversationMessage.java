package com.jason.ai.knowledgebase.model.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** MySQL 中的完整对话历史记录。 */
@Data
@TableName("ai_conversation_message")
public class ConversationMessage {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long sessionId;
    private Long userId;
    private Long requestId;
    private String role;
    private String content;
    private String status;
    private String metadata;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updateTime;
}
