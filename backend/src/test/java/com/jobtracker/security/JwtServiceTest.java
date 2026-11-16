package com.jobtracker.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    @Test
    void generatedTokenCanBeValidatedAndReturnsUserId() {
        JwtService jwtService = new JwtService(SECRET, 60);

        String token = jwtService.generateToken(42L, "asha@example.com");

        assertThat(jwtService.extractUserId(token)).contains(42L);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 60);
        String token = jwtService.generateToken(42L, "asha@example.com");

        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        assertThat(jwtService.extractUserId(tampered)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService issuer = new JwtService("another-secret-another-secret-another-1", 60);
        JwtService validator = new JwtService(SECRET, 60);

        String token = issuer.generateToken(7L, "x@example.com");

        assertThat(validator.extractUserId(token)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiredIssuer = new JwtService(SECRET, -1);

        String token = expiredIssuer.generateToken(7L, "x@example.com");

        assertThat(expiredIssuer.extractUserId(token)).isEmpty();
    }

    @Test
    void garbageAndEmptyTokensAreRejected() {
        JwtService jwtService = new JwtService(SECRET, 60);

        assertThat(jwtService.extractUserId("not-a-jwt")).isEmpty();
        assertThat(jwtService.extractUserId("")).isEmpty();
    }

    @Test
    void reportsConfiguredLifetimeInSeconds() {
        assertThat(new JwtService(SECRET, 15).getExpirationSeconds()).isEqualTo(900L);
    }

    @Test
    void secretShorterThan32BytesIsRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtService("too-short", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    @Test
    void placeholderSecretFromEnvExampleIsRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtService("CHANGE_ME_generate_a_long_random_secret", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }
}
