package com.docint.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for JWT generation/validation using REAL crypto (no mocks needed —
 * it's self-contained). We set the @Value fields then call init() ourselves,
 * exactly as Spring's @PostConstruct would.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "test-secret-key-at-least-32-characters-long");
        ReflectionTestUtils.setField(provider, "accessExpiryMs", 900_000L);
        ReflectionTestUtils.setField(provider, "refreshExpiryMs", 604_800_000L);
        provider.init();   // builds the signing key
    }

    @Test
    @DisplayName("a generated access token is valid and round-trips its claims")
    void accessTokenRoundTrips() {
        String token = provider.generateAccessToken("alice@docint.com", 3);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.extractEmail(token)).isEqualTo("alice@docint.com");
        assertThat(provider.extractType(token)).isEqualTo("access");
        assertThat(provider.extractVersion(token)).isEqualTo(3);
    }

    @Test
    @DisplayName("refresh tokens are tagged with type=refresh")
    void refreshTokenType() {
        String token = provider.generateRefreshToken("bob@docint.com", 0);
        assertThat(provider.extractType(token)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("a garbage / tampered token fails validation")
    void invalidTokenRejected() {
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void tokenFromAnotherSecretRejected() {
        // Simulate a token minted by a different server (different key)
        JwtTokenProvider other = new JwtTokenProvider();
        ReflectionTestUtils.setField(other, "jwtSecret", "a-totally-different-secret-key-32-chars!!");
        ReflectionTestUtils.setField(other, "accessExpiryMs", 900_000L);
        ReflectionTestUtils.setField(other, "refreshExpiryMs", 604_800_000L);
        other.init();

        String foreignToken = other.generateAccessToken("eve@evil.com", 0);

        assertThat(provider.validateToken(foreignToken)).isFalse();
    }
}
