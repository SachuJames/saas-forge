package com.saasforge.gdpr;

import com.saasforge.audit.AuditLog;
import com.saasforge.audit.AuditLogRepository;
import com.saasforge.audit.Audited;
import com.saasforge.user.AppUser;
import com.saasforge.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GDPR data-subject tools. Export gives the user everything we hold on
 * them as JSON; erasure anonymizes the profile and scrubs their audit
 * details. Erasure keeps the audit rows themselves (with the actor
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
    public Map<String, Object> exportUserData(UUID userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("unknown user"));
        List<AuditLog> trail = auditLogs.findAll().stream()
                .filter(log -> user.getEmail().equals(log.getActor())
                        || userId.toString().equals(log.getEntityId()))
                .collect(Collectors.toList());

        Map<String, Object> out = new HashMap<>();
        out.put("profile", Map.of(
                "id", user.getId().toString(),
                "email", user.get.email(),
                "name", user.getName(),
                "role", user.getRole(),
                "created_at", user.getCreatedAt().toString()));
        out.put("audit_entries", trail.stream().map(log -> Map.of(
                "action", log.getAction(),
                "entity", String.valueOf(log.getEntity()),
                "created_at", log.getCreatedAt().toString()
        )).collect(Collectors.toList()));
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
        for (AuditLog log : auditLogs.findAll()) {
            if (userId.toString().equals(log.getEntityId()) || oldEmail.equals(log.getActor())) {
                log.setActor("deleted-user");
                log.setDetails(null);
                auditLogs.save(log);
            }
        }
    }
}
