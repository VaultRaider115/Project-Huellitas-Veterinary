package com.Proyect_DevOps.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigJwtAuthoritiesTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void accessTokenKeepsRolesAndAccessAuthority() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("admin@example.com")
            .claim("roles", "Admin")
            .claim("purpose", "access")
            .build();

        AbstractAuthenticationToken auth = config.configurarAutenticacion().convert(jwt);

        assertTrue(hasAuthority(auth, "Admin"));
        assertTrue(hasAuthority(auth, "TOKEN_access"));
        assertFalse(hasAuthority(auth, "TOKEN_mfa"));
    }

    @Test
    void temporaryMfaTokenHasNoPrivilegedRole() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("admin@example.com")
            .claim("roles", "Admin")
            .claim("purpose", "mfa")
            .build();

        AbstractAuthenticationToken auth = config.configurarAutenticacion().convert(jwt);

        assertFalse(hasAuthority(auth, "Admin"));
        assertFalse(hasAuthority(auth, "Administrador"));
        assertFalse(hasAuthority(auth, "TOKEN_access"));
        assertTrue(hasAuthority(auth, "TOKEN_mfa"));
    }

    private boolean hasAuthority(AbstractAuthenticationToken auth, String authority) {
        return auth.getAuthorities().stream()
            .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
