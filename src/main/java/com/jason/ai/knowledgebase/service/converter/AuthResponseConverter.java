package com.jason.ai.knowledgebase.service.converter;

import org.mapstruct.Mapper;

import com.jason.ai.knowledgebase.model.entity.SysUser;
import com.jason.ai.knowledgebase.model.response.AuthResponses.UserView;

/** 认证响应转换器。 */
@Mapper(config = MapStructConfiguration.class)
public interface AuthResponseConverter {

    /** 将用户实体转换为认证安全视图。 */
    UserView toUserView(SysUser user);
}
