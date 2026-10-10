package com.saaskit.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs inside the Spring Security chain, after the bearer token has been
 * validated. Makes the token's tenant_id claim authoritative: an
 * authenticated caller cannot pick a different tenant via the X-Tenant-ID
 * header. When the header is absent, the claim fills in the context.
 *
 * Admin paths are exempt so platform admins can operate across tenants;
 * their cross-tenant access is governed by the ADMIN role instead.
 */
public class TenantConsistencyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt
                && !isAdminPath(request.getRequestURI())) {
            String jwtTenant = jwt.getClaimAsString(TenantFilter.TENANT_CLAIM);
            String contextTenant = TenantContext.getTenantId();
            if (jwtTenant != null && !jwtTenant.isBlank()) {
                if (contextTenant == null || contextTenant.isBlank()) {
                    TenantContext.setTenantId(jwtTenant);
                } else if (!jwtTenant.equals(contextTenant)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Tenant mismatch: this token is not valid for the requested tenant");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/api/admin/");
    }
}
