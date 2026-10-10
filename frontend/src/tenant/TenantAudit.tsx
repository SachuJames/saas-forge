import { useEffect, useState } from 'react';
import { api, getTenant } from '../api';

interface AuditLog {
  id: string;
  actor: string;
  action: string;
  entity: string | null;
  entityId: string | null;
  createdAt: string;
}

export default function TenantAudit() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const tenant = getTenant();

  useEffect(() => {
    api<AuditLog[]>('/audit', {}, tenant).then(setLogs).catch(() => {});
  }, [tenant]);

  return (
    <div style={{ padding: 24, maxWidth: 900 }}>
      <h1>Audit log</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead><tr><th>When</th><th>Actor</th><th>Action</th><th>Entity</th></tr></thead>
        <tbody>
          {logs.map(l => (
            <tr key={l.id} style={{ borderTop: '1px solid #ddd' }}>
              <td>{l.createdAt}</td><td>{l.actor}</td><td>{l.action}</td>
              <td>{l.entity}{l.entityId ? ` (${l.entityId})` : ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
