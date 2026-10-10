package com.saaskit.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves the tenant for every request and pins it to the TenantContext.
 *
 * The X-Tenant-ID header is a routing hint, not proof of authorization:
 * TenantConsistencyFilter runs after Spring Security validates the JWT and
 * rejects any authenticated request whose header disagrees with the token's
 * tenant_id claim. Unauthenticated requests (public endpoints, local dev)
 * may use the header on its own.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String TENANT_CLAIM = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String tenantId = resolveTenant(request);
            String path = request.getRequestURI();

            if (tenantId == null && requiresTenant(path)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Missing tenant: set the " + TENANT_HEADER + " header");
                return;
            }
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenant(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString(TENANT_CLAIM);
        }
        return null;
    }

    private boolean requiresTenant(String path) {
        return path.startsWith("/api/")
                && !path.startsWith("/api/admin/")
                && !path.startsWith("/api/public/");
    }
}
