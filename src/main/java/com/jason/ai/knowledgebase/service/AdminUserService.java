package com.jason.ai.knowledgebase.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.model.response.PageResult;
import com.jason.ai.knowledgebase.model.response.AdminResponses.UserListItem;
import com.jason.ai.knowledgebase.model.request.AdminRequests.UserPageRequest;
import com.jason.ai.knowledgebase.model.response.AdminResponses.UserView;
import com.jason.ai.knowledgebase.model.entity.SysUser;
import com.jason.ai.knowledgebase.model.enums.UserStatus;
import com.jason.ai.knowledgebase.repository.mapper.SysUserMapper;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.common.util.PageBounds;
import com.jason.ai.knowledgebase.service.converter.AdminUserResponseConverter;

import lombok.RequiredArgsConstructor;

/**
 * 管理员用户查询和账号状态管理服务。
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final long DEFAULT_PAGE_SIZE = 20;
    private static final long MAXIMUM_PAGE_SIZE = 100;

    private final SysUserMapper userMapper;
    private final AuthService authService;
    private final AdminUserResponseConverter responseConverter;

    /**
     * 按可选用户名查询用户分页。
     *
     * @param request 分页与用户名筛选条件
     * @return 用户分页成功响应
     */
    public ApiResponse<PageResult<UserListItem>> list(UserPageRequest request) {
        String keyword = request.username() == null ? null : request.username().trim();
        PageBounds bounds = PageBounds.of(request.page(), request.size(), DEFAULT_PAGE_SIZE, MAXIMUM_PAGE_SIZE);
        Page<SysUser> result = userMapper.selectPage(
                new Page<>(bounds.page(), bounds.size()),
                Wrappers.<SysUser>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), SysUser::getUsername, keyword)
                        .orderByDesc(SysUser::getCreateTime)
                        .orderByDesc(SysUser::getId));
        return ApiResponse.page(result.convert(responseConverter::toListItem));
    }

    /**
     * 查询指定用户的管理员视图。
     *
     * @param id 用户 ID
     * @return 包含非敏感用户视图的成功响应
     * @throws AppException 用户不存在时抛出
     */
    public ApiResponse<UserView> get(long id) {
        return ApiResponse.success(responseConverter.toView(require(id)));
    }

    /**
     * 幂等更新账号状态；停用账号时注销全部认证会话。
     *
     * @param id 用户 ID
     * @param status 目标状态
     * @throws AppException 状态不受支持或用户不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(long id, String status) {
        UserStatus target = UserStatus.parse(status);
        applyStatus(require(id), target);
        if (target == UserStatus.DISABLED) {
            authService.logout(id);
        }
    }

    private void applyStatus(SysUser user, UserStatus target) {
        if (target.name().equals(user.getStatus())) {
            return;
        }
        user.setStatus(target.name());
        if (userMapper.updateById(user) != 1) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
    }

    private SysUser require(long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        return user;
    }

}
