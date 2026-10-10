package com.saasforge.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Writes audit rows for @Audited service methods. The actor comes from the
 * JWT subject; the entity id is taken from the method's first argument when
 * it looks like an id, otherwise left blank. Kept deliberately simple:
 * a starter kit should make auditing the default, not a framework.
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogs;

    public AuditAspect(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @AfterReturning("@annotation(com.saasforge.audit.Audited)")
    public void audit(JoinPoint joinPoint) {
        Audited audited = ((MethodSignature) joinPoint.getSignature())
                .getMethod().getAnnotation(Audited.class);

        AuditLog log = new AuditLog();
        log.setActor(currentActor());
        log.setAction(audited.action());
        log.setEntity(audited.entity().isEmpty() ? null : audited.entity());
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] != null) {
            log.setEntityId(args[0].toString());
        }
        auditLogs.save(log);
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }
}
