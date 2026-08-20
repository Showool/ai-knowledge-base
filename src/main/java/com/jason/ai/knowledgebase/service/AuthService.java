package com.jason.ai.knowledgebase.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jason.ai.knowledgebase.repository.cache.AuthSessionStore;
import com.jason.ai.knowledgebase.model.response.AuthResponses.TokenResponse;
import com.jason.ai.knowledgebase.model.response.AuthResponses.UserView;
import com.jason.ai.knowledgebase.model.entity.AuthSession;
import com.jason.ai.knowledgebase.model.entity.SysUser;
import com.jason.ai.knowledgebase.model.entity.UserQuota;
import com.jason.ai.knowledgebase.model.enums.UserRole;
import com.jason.ai.knowledgebase.model.enums.UserStatus;
import com.jason.ai.knowledgebase.repository.mapper.AuthSessionMapper;
import com.jason.ai.knowledgebase.repository.mapper.SysUserMapper;
import com.jason.ai.knowledgebase.repository.mapper.UserQuotaMapper;
import com.jason.ai.knowledgebase.security.JwtService;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.config.AuthProperties;
import com.jason.ai.knowledgebase.common.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;

/**
 * 账号注册、密码验证、Token 轮换和全局注销服务。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{4,32}");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN_TYPE = "Bearer";
    private static final String TOKEN_DIGEST_ALGORITHM = "SHA-256";
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final SysUserMapper userMapper;
    private final AuthSessionMapper sessionMapper;
    private final UserQuotaMapper quotaMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthSessionStore sessionStore;
    private final SnowflakeIdGenerator idGenerator;
    private final AuthProperties properties;

    /**
     * 注册账号并创建初始额度记录。
     *
     * @param rawUsername 未规范化的用户名
     * @param password 原始密码
     * @return 新账号的安全视图
     * @throws AppException 用户名或密码不合法、用户名已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public UserView register(String rawUsername, String password) {
        String username = normalizeUsername(rawUsername);
        validatePassword(password);
        long userId = idGenerator.nextId();

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.USER.name());
        user.setStatus(UserStatus.ENABLED.name());
        user.setDeleted(0);

        UserQuota quota = new UserQuota();
        quota.setUserId(userId);
        quota.setAvailableTimes(0);
        try {
            userMapper.insert(user);
            quotaMapper.insert(quota);
        } catch (DuplicateKeyException exception) {
            throw new AppException(ErrorCode.USERNAME_EXISTS);
        }
        return toView(user);
    }

    /**
     * 验证账号密码并替换该用户之前的全部登录会话。
     *
     * @param rawUsername 未规范化的用户名
     * @param password 原始密码
     * @return 新的 Access Token 和 Refresh Token
     * @throws AppException 凭据错误或账号不可用时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenResponse login(String rawUsername, String password) {
        String username = normalizeUsername(rawUsername);
        SysUser user = userMapper.findByUsername(username);
        if (user == null || !UserStatus.ENABLED.name().equals(user.getStatus())
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        Instant now = Instant.now();
        sessionMapper.revokeAll(user.getId(), now);
        long authSessionId = idGenerator.nextId();
        String refreshToken = newRefreshToken();

        AuthSession session = new AuthSession();
        session.setId(authSessionId);
        session.setUserId(user.getId());
        session.setRefreshTokenHash(hash(refreshToken));
        session.setRefreshExpireTime(now.plus(properties.getRefreshTokenTtl()));
        session.setRevoked(false);
        sessionMapper.insert(session);
        sessionStore.set(user.getId(), authSessionId);
        return tokens(user, authSessionId, refreshToken);
    }

    /**
     * 原子轮换 Refresh Token，并签发新的 Access Token。
     *
     * @param refreshToken 当前 Refresh Token
     * @return 轮换后的 Token
     * @throws AppException Token 失效、过期或账号不可用时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenResponse refresh(String refreshToken) {
        String oldHash = hash(refreshToken);
        AuthSession session = sessionMapper.findActiveByRefreshHash(oldHash);
        Instant now = Instant.now();
        if (session == null || !session.getRefreshExpireTime().isAfter(now)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String currentSession = sessionStore.get(session.getUserId());
        if (!String.valueOf(session.getId()).equals(currentSession)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        SysUser user = userMapper.selectById(session.getUserId());
        if (user == null || !UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String newRefresh = newRefreshToken();
        Instant newExpiry = now.plus(properties.getRefreshTokenTtl());
        if (sessionMapper.rotate(session.getId(), oldHash, hash(newRefresh), newExpiry, now) != 1) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        sessionStore.set(user.getId(), session.getId());
        return tokens(user, session.getId(), newRefresh);
    }

    /**
     * 注销用户的全部认证会话，并删除 Redis 当前会话指针。
     *
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void logout(long userId) {
        sessionMapper.revokeAll(userId, Instant.now());
        sessionStore.delete(userId);
    }

    private TokenResponse tokens(SysUser user, long sessionId, String refreshToken) {
        return new TokenResponse(TOKEN_TYPE,
                jwtService.issue(user.getId(), user.getUsername(), user.getRole(), sessionId),
                properties.getAccessTokenTtl().toSeconds(),
                refreshToken,
                properties.getRefreshTokenTtl().toSeconds(),
                toView(user));
    }

    private UserView toView(SysUser user) {
        return new UserView(user.getId(), user.getUsername(), user.getRole());
    }

    private String normalizeUsername(String username) {
        String normalized = Normalizer.normalize(username, Normalizer.Form.NFKC).trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "用户名必须为 4-32 位字母、数字或下划线");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password.length() < 8 || password.length() > 72) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "密码长度必须为 8-72 个字符");
        }
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(TOKEN_DIGEST_ALGORITHM)
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }
}
