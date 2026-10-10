package com.saaskit.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The token's tenant_id claim is authoritative. A forged X-Tenant-ID
 * header must not let an authenticated caller reach another tenant.
 */
class TenantConsistencyFilterTest {

    private final TenantConsistencyFilter filter = new TenantConsistencyFilter();

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Jwt jwt(String tenantId) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("sub", "user-1", "tenant_id", tenantId));
    }

    private void authenticate(String tenantId) {
        var auth = new TestingAuthenticationToken(jwt(tenantId), null, "ROLE_USER");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void matchingHeaderAndClaimPasses() throws Exception {
        authenticate("acme");
        TenantContext.setTenantId("acme");
        var req = new MockHttpServletRequest("GET", "/api/users");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertEquals(200, res.getStatus());
        assertEquals("acme", TenantContext.getTenantId());
    }

    @Test
    void mismatchedHeaderIsRejected() throws Exception {
        authenticate("acme");
        TenantContext.setTenantId("evil-corp");
        var req = new MockHttpServletRequest("GET", "/api/users");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertEquals(403, res.getStatus());
    }

    @Test
    void missingHeaderFallsBackToClaim() throws Exception {
        authenticate("acme");
        var req = new MockHttpServletRequest("GET", "/api/users");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertEquals(200, res.getStatus());
        assertEquals("acme", TenantContext.getTenantId());
    }

    @Test
    void adminPathsAreExempt() throws Exception {
        authenticate("acme");
        TenantContext.setTenantId("other-tenant");
        var req = new MockHttpServletRequest("GET", "/api/admin/tenants");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertEquals(200, res.getStatus());
    }

    @Test
    void unauthenticatedRequestsPassThrough() throws Exception {
        TenantContext.setTenantId("acme");
        var req = new MockHttpServletRequest("GET", "/api/public/stripe/webhook");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertEquals(200, res.getStatus());
    }
}
