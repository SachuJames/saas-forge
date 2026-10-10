package com.saaskit.gdpr;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Data-subject endpoints. Export always acts on the caller, resolved from
 * the JWT subject; there is no parameter to point it at someone else.
 * Erasure is admin-only and scoped to the current tenant by the schema
 * the request runs against.
 */
@RestController
@RequestMapping("/api/gdpr")
public class GdprController {

    private final GdprService gdpr;

    public GdprController(GdprService gdpr) {
        this.gdpr = gdpr;
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportMine(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(gdpr.exportOwnData(jwt.getSubject()));
    }

    @DeleteMapping("/erasure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> erase(@RequestParam UUID userId) {
        gdpr.eraseUser(userId);
        return ResponseEntity.noContent().build();
    }
}
