package com.jason.ai.knowledgebase.model.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 在扣减额度前用于拦截的规范化短语。 */
@Data
@TableName("ai_meaningless_phrase")
public class MeaninglessPhrase {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String phrase;
    private String category;
    private Boolean enabled;
    private Integer priority;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updateTime;
}
