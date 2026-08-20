package com.jason.ai.knowledgebase.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jason.ai.knowledgebase.repository.cache.AuthSessionStore;
import com.jason.ai.knowledgebase.model.enums.UserRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 同时验证 JWT 和 Redis 当前登录会话指针。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthSessionStore sessionStore;

    /**
     * 验证请求中的 Bearer Token，并在认证成功时写入安全上下文。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 后续过滤器链
     * @throws ServletException 过滤器链执行失败时抛出
     * @throws IOException HTTP 读写失败时抛出
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            try {
                AuthenticatedUser user = jwtService.decode(authorization.substring(BEARER_PREFIX.length()));
                String currentSession = sessionStore.get(user.userId());
                if (String.valueOf(user.authSessionId()).equals(currentSession)) {
                    UserRole role = UserRole.valueOf(user.role());
                    var authentication = new UsernamePasswordAuthenticationToken(user, null,
                            List.of(new SimpleGrantedAuthority(role.authority())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
