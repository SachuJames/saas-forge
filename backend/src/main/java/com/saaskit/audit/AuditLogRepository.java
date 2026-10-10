package com.saaskit.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByActor(String actor);

    List<AuditLog> findByEntityId(String entityId);
}
