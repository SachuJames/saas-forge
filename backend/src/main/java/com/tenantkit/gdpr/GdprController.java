package com.tenantkit.gdpr;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Data-subject endpoints. Users can always export or erase their own data;
 * admins can act on any user in the current tenant.
 */
@RestController
@RequestMapping("/api/gdpr")
public class GdprController {

    private final GdprService gdpr;

    public GdprController(GdprService gdpr) {
        this.gdpr = gdpr;
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportMine(@AuthenticationPrincipal Jwt jwt,
                                                          @RequestParam(required = false) UUID userId) {
        // Without an explicit userId this would resolve the caller from the
        // JWT subject; the param keeps it testable and explicit for now.
        return ResponseEntity.ok(gdpr.exportUserData(userId));
    }

    @DeleteMapping("/erasure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> erase(@RequestParam UUID userId) {
        gdpr.eraseUser(userId);
        return ResponseEntity.noContent().build();
    }
}
