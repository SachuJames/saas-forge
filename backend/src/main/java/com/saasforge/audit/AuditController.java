package com.saasforge.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only view of the current tenant's audit trail. */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditLogRepository auditLogs;

    public AuditController(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @GetMapping
    public List<AuditLog> list() {
        return auditLogs.findAll();
    }
}
