package com.jason.ai.knowledgebase.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.jason.ai.knowledgebase.config.AuthProperties;

class JwtServiceTest {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";
    private static final long USER_ID = 348019002964054016L;
    private static final long AUTH_SESSION_ID = 348044607776493568L;

    @Test
    void decodesStableStringIssuerAndLongClaims() {
        JwtService service = new JwtService(properties("ai-knowledge-base"));

        String token = service.issue(USER_ID, "admin", "ADMIN", AUTH_SESSION_ID);
        AuthenticatedUser user = service.decode(token);

        assertThat(user.userId()).isEqualTo(USER_ID);
        assertThat(user.username()).isEqualTo("admin");
        assertThat(user.role()).isEqualTo("ADMIN");
        assertThat(user.authSessionId()).isEqualTo(AUTH_SESSION_ID);
    }

    @Test
    void rejectsUnexpectedStringIssuer() {
        JwtService trustedService = new JwtService(properties("ai-knowledge-base"));
        JwtService otherIssuerService = new JwtService(properties("other-service"));
        String token = otherIssuerService.issue(USER_ID, "admin", "ADMIN", AUTH_SESSION_ID);

        assertThatThrownBy(() -> trustedService.decode(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT 签发方不符合预期");
    }

    private AuthProperties properties(String issuer) {
        AuthProperties properties = new AuthProperties();
        properties.setIssuer(issuer);
        properties.setJwtSecret(JWT_SECRET);
        return properties;
    }
}