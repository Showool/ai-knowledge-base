package com.jason.ai.knowledgebase.service.converter;

import org.mapstruct.Mapper;

import com.jason.ai.knowledgebase.model.entity.SysUser;
import com.jason.ai.knowledgebase.model.response.AdminResponses.UserListItem;
import com.jason.ai.knowledgebase.model.response.AdminResponses.UserView;

/** 管理员用户响应转换器。 */
@Mapper(config = MapStructConfiguration.class)
public interface AdminUserResponseConverter {

    /** 将用户实体转换为管理员分页项。 */
    UserListItem toListItem(SysUser user);

    /** 将用户实体转换为管理员详情。 */
    UserView toView(SysUser user);
}