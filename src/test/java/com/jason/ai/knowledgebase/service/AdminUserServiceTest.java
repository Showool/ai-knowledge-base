package com.jason.ai.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.model.response.PageResult;
import com.jason.ai.knowledgebase.model.response.AdminResponses.UserListItem;
import com.jason.ai.knowledgebase.model.request.AdminRequests.UserPageRequest;
import com.jason.ai.knowledgebase.model.response.AdminResponses.UserView;
import com.jason.ai.knowledgebase.model.entity.SysUser;
import com.jason.ai.knowledgebase.repository.mapper.SysUserMapper;
import com.jason.ai.knowledgebase.service.converter.AdminUserResponseConverter;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final long USER_ID = 42L;

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), SysUser.class);
    }

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private AuthService authService;
    @Mock
    private AdminUserResponseConverter responseConverter;
    @InjectMocks
    private AdminUserService service;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listUsesFuzzyUsernameFilterAndMapsPage() {
        Page<SysUser> mapperResult = new Page<>(2, 5, 11);
        mapperResult.setRecords(List.of(user("ENABLED")));
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(mapperResult);
        UserListItem item = new UserListItem(42L, "alice", "USER", "ENABLED",
                Instant.parse("2026-08-18T01:00:00Z"), Instant.parse("2026-08-18T02:00:00Z"));
        when(responseConverter.toListItem(any(SysUser.class))).thenReturn(item);

        ApiResponse<PageResult<UserListItem>> response = service.list(new UserPageRequest("  ali  ", 2L, 5L));

        assertThat(response.data()).isNotNull();
        assertThat(response.data().total()).isEqualTo(11);
        assertThat(response.data().items()).containsExactly(item);

        ArgumentCaptor<LambdaQueryWrapper<SysUser>> queryCaptor =
                ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(userMapper).selectPage(any(Page.class), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getSqlSegment()).contains("username", "LIKE");
        assertThat(queryCaptor.getValue().getParamNameValuePairs()).containsValue("%ali%");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listClampsPaginationAndIgnoresBlankUsername() {
        Page<SysUser> mapperResult = new Page<>(1, 100, 0);
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(mapperResult);

        service.list(new UserPageRequest("   ", 0L, 500L));

        ArgumentCaptor<Page<SysUser>> pageCaptor = ArgumentCaptor.forClass((Class) Page.class);
        ArgumentCaptor<LambdaQueryWrapper<SysUser>> queryCaptor =
                ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(userMapper).selectPage(pageCaptor.capture(), queryCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
        assertThat(queryCaptor.getValue().getSqlSegment()).doesNotContain("LIKE");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listUsesDefaultPaginationWhenBodyFieldsAreMissing() {
        Page<SysUser> mapperResult = new Page<>(1, 20, 0);
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(mapperResult);

        service.list(new UserPageRequest(null, null, null));

        ArgumentCaptor<Page<SysUser>> pageCaptor = ArgumentCaptor.forClass((Class) Page.class);
        verify(userMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20);
    }

    @Test
    void getReturnsOnlyAdministrativeUserFields() {
        SysUser user = user("ENABLED");
        UserView view = new UserView("alice", "USER", "ENABLED",
                Instant.parse("2026-08-18T01:00:00Z"), Instant.parse("2026-08-18T02:00:00Z"));
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(responseConverter.toView(user)).thenReturn(view);

        ApiResponse<UserView> response = service.get(USER_ID);

        assertThat(response.data()).isEqualTo(view);
    }

    @Test
    void getRejectsUnknownUser() {
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.get(USER_ID))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void updateStatusEnablesDisabledUser() {
        SysUser user = user("DISABLED");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);

        service.updateStatus(USER_ID, "ENABLED");

        assertThat(user.getStatus()).isEqualTo("ENABLED");
        verify(userMapper).updateById(user);
        verify(authService, never()).logout(USER_ID);
    }

    @Test
    void repeatedEnabledStatusIsIdempotent() {
        SysUser user = user("ENABLED");
        when(userMapper.selectById(USER_ID)).thenReturn(user);

        service.updateStatus(USER_ID, "ENABLED");

        verify(userMapper, never()).updateById(user);
        verify(authService, never()).logout(USER_ID);
    }

    @Test
    void updateStatusDisablesUserBeforeRevokingEverySession() {
        SysUser user = user("ENABLED");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);

        service.updateStatus(USER_ID, "DISABLED");

        assertThat(user.getStatus()).isEqualTo("DISABLED");
        InOrder order = inOrder(userMapper, authService);
        order.verify(userMapper).updateById(user);
        order.verify(authService).logout(USER_ID);
    }

    @Test
    void repeatedDisabledStatusStillRevokesResidualSessions() {
        SysUser user = user("DISABLED");
        when(userMapper.selectById(USER_ID)).thenReturn(user);

        service.updateStatus(USER_ID, "DISABLED");

        verify(userMapper, never()).updateById(user);
        verify(authService).logout(USER_ID);
    }

    @Test
    void updateStatusRejectsUnsupportedValueBeforeLoadingUser() {
        assertThatThrownBy(() -> service.updateStatus(USER_ID, "LOCKED"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT));

        verify(userMapper, never()).selectById(USER_ID);
        verify(userMapper, never()).updateById(any(SysUser.class));
        verify(authService, never()).logout(USER_ID);
    }

    private SysUser user(String status) {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setUsername("alice");
        user.setPasswordHash("not-returned");
        user.setRole("USER");
        user.setStatus(status);
        user.setCreateTime(Instant.parse("2026-08-18T01:00:00Z"));
        user.setUpdateTime(Instant.parse("2026-08-18T02:00:00Z"));
        return user;
    }
}






