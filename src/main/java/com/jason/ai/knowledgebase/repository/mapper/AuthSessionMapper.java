package com.jason.ai.knowledgebase.repository.mapper;

import java.time.Instant;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jason.ai.knowledgebase.model.entity.AuthSession;

public interface AuthSessionMapper extends BaseMapper<AuthSession> {
    @Update("UPDATE auth_session SET revoked = 1, update_time = #{now} WHERE user_id = #{userId} AND revoked = 0")
    int revokeAll(@Param("userId") long userId, @Param("now") Instant now);

    @Select("SELECT * FROM auth_session WHERE refresh_token_hash = #{hash} AND revoked = 0 LIMIT 1")
    AuthSession findActiveByRefreshHash(@Param("hash") String hash);

    @Update("UPDATE auth_session SET refresh_token_hash = #{newHash}, refresh_expire_time = #{expireTime}, "
            + "update_time = #{now} WHERE id = #{id} AND refresh_token_hash = #{oldHash} AND revoked = 0")
    int rotate(@Param("id") long id, @Param("oldHash") String oldHash, @Param("newHash") String newHash,
            @Param("expireTime") Instant expireTime, @Param("now") Instant now);
}
