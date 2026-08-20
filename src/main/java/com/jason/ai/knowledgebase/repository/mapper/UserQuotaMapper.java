package com.jason.ai.knowledgebase.repository.mapper;

import java.time.Instant;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jason.ai.knowledgebase.model.entity.UserQuota;

public interface UserQuotaMapper extends BaseMapper<UserQuota> {
    @Update("UPDATE ai_user_quota SET available_times = available_times - 1, update_time = #{now} "
            + "WHERE user_id = #{userId} AND available_times > 0")
    int consume(@Param("userId") long userId, @Param("now") Instant now);

    @Select("SELECT available_times FROM ai_user_quota WHERE user_id = #{userId}")
    Integer available(@Param("userId") long userId);
}
