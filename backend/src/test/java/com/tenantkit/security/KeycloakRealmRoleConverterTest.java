package com.tenantkit.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    private Jwt jwtWithRoles(List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-1")
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    @Test
    void mapsRealmRolesToAuthorities() {
        AbstractAuthenticationToken token =
                converter.convert(jwtWithRoles(List.of("admin", "user")));
        var authorities = token.getAuthorities().stream()
                .map(Object::toString).toList();
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_USER"));
    }

    @Test
    void emptyWhenNoRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        AbstractAuthenticationToken token = converter.convert(jwt);
        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    void usesSubjectAsPrincipalName() {
        AbstractAuthenticationToken token =
                converter.convert(jwtWithRoles(List.of("user")));
        assertEquals("user-1", token.getName());
    }
}
