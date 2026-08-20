package com.jason.ai.knowledgebase.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.config.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** 签发并校验 HS256 Access Token。 */
@Component
public class JwtService {

    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final String CLAIM_ISSUER = "iss";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_AUTH_SESSION_ID = "authSessionId";

    private final AuthProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    /**
     * 使用认证配置初始化签名器和校验器。
     *
     * @param properties 认证配置
     * @throws IllegalStateException JWT 密钥不足 32 字节时抛出
     */
    public JwtService(AuthProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT 密钥至少需要 32 字节");
        }
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        this.decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /**
     * 为认证会话签发 Access Token。
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @param role 角色
     * @param authSessionId 认证会话 ID
     * @return JWT 字符串
     */
    public String issue(long userId, String username, String role, long authSessionId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plus(properties.getAccessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_AUTH_SESSION_ID, authSessionId)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * 校验并解析 Access Token。
     *
     * @param token JWT 字符串
     * @return 已认证用户
     * @throws IllegalArgumentException 签发方不符合当前服务配置时抛出
     */
    public AuthenticatedUser decode(String token) {
        Jwt jwt = decoder.decode(token);
        if (!properties.getIssuer().equals(jwt.getClaimAsString(CLAIM_ISSUER))) {
            throw new IllegalArgumentException("JWT 签发方不符合预期");
        }
        return new AuthenticatedUser(
                jwt.getClaim(CLAIM_USER_ID),
                jwt.getClaimAsString(CLAIM_USERNAME),
                jwt.getClaimAsString(CLAIM_ROLE),
                jwt.getClaim(CLAIM_AUTH_SESSION_ID));
    }
}