package com.jason.ai.knowledgebase.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.config.OpenApiProperties;
import com.jason.ai.knowledgebase.model.enums.UserRole;
import com.jason.ai.knowledgebase.model.response.ApiResponse;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

/** dev、test 与 prod 共用的无状态 JWT 安全配置。 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final RequestMatcher CHAT_STREAM_ASYNC_DISPATCH = request ->
            request.getDispatcherType() == DispatcherType.ASYNC
                    && HttpMethod.POST.matches(request.getMethod())
                    && "/api/chat/sessions/stream".equals(request.getServletPath());

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            OpenApiProperties properties) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> writeError(response,
                                ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) -> writeError(response,
                                ErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(CHAT_STREAM_ASYNC_DISPATCH).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                                    "/actuator/health", "/actuator/info", "/error")
                            .permitAll()
                            .requestMatchers("/api/admin/**").hasRole(UserRole.ADMIN.name());
                    if (properties.isEnabled()) {
                        authorize.requestMatchers("/api/v3/api-docs/**", "/v3/api-docs/**", "/doc.html",
                                "/webjars/**").permitAll();
                    } else {
                        authorize.requestMatchers("/api/v3/api-docs/**", "/v3/api-docs/**", "/doc.html",
                                "/webjars/**").denyAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeError(HttpServletResponse response, ErrorCode error) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        ApiResponse<Void> body = ApiResponse.error(error.code(), error.message());
        response.getWriter().write("{\"code\":" + body.code() + ",\"message\":\"" + body.message()
                + "\",\"data\":null}");
    }
}
