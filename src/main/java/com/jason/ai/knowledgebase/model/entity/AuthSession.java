package com.jason.ai.knowledgebase.model.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** Refresh Token 会话；Redis 指向当前唯一有效会话。 */
@Data
@TableName("auth_session")
public class AuthSession {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long userId;
    private String refreshTokenHash;
    private Instant refreshExpireTime;
    private Boolean revoked;
    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updateTime;
}
