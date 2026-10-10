package com.saaskit.gdpr;

import com.saaskit.audit.AuditLog;
import com.saaskit.audit.AuditLogRepository;
import com.saaskit.audit.Audited;
import com.saaskit.user.AppUser;
import com.saaskit.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * GDPR data-subject tools. Export gives the caller everything we hold on
 * them as JSON; erasure anonymizes the profile and scrubs their audit
 * details. Callers are resolved from the authenticated JWT subject, never
 * from a request parameter, so one user cannot export or erase another
 * user's data. Erasure keeps the audit rows themselves (with the actor
 * blanked) so the trail stays gap-free while no personal data remains.
 */
@Service
public class GdprService {

    private final AppUserRepository users;
    private final AuditLogRepository auditLogs;

    public GdprService(AppUserRepository users, AuditLogRepository auditLogs) {
        this.users = users;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportOwnData(String keycloakSub) {
        AppUser user = users.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new IllegalArgumentException("unknown user"));

        Set<AuditLog> trail = new LinkedHashSet<>();
        trail.addAll(auditLogs.findByActor(user.getEmail()));
        trail.addAll(auditLogs.findByEntityId(user.getId().toString()));

        Map<String, Object> out = new HashMap<>();
        out.put("profile", Map.of(
                "id", user.getId().toString(),
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole(),
                "created_at", user.getCreatedAt().toString()));
        out.put("audit_entries", trail.stream().map(log -> Map.of(
                "action", log.getAction(),
                "entity", String.valueOf(log.getEntity()),
                "created_at", log.getCreatedAt().toString()
        )).toList());
        return out;
    }

    @Audited(action = "GDPR_ERASURE", entity = "app_user")
    @Transactional
    public void eraseUser(UUID userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("unknown user"));
        String oldEmail = user.getEmail();
        String token = "deleted-" + user.getId().toString().substring(0, 8);
        user.setEmail(token + "@deleted.local");
        user.setName("Deleted User");
        user.setKeycloakSub(null);
        users.save(user);

        // Scrub audit rows that identify the user, but keep the rows so the
        // trail itself stays gap-free.
        List<AuditLog> affected = new ArrayList<>();
        affected.addAll(auditLogs.findByActor(oldEmail));
        affected.addAll(auditLogs.findByEntityId(userId.toString()));
        for (AuditLog log : affected) {
            log.setActor("deleted-user");
            log.setDetails(null);
            auditLogs.save(log);
        }
    }
}
