import { useEffect, useState } from 'react';
import { api } from '../api';

interface Tenant {
  id: string;
  name: string;
  slug: string;
  plan: string;
  status: string;
}

export default function AdminTenants() {
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [error, setError] = useState('');

  const load = () => api<Tenant[]>('/admin/tenants').then(setTenants).catch(e => setError(String(e)));
  useEffect(() => { load(); }, []);

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await api('/admin/tenants', {
        method: 'POST',
        body: JSON.stringify({ name, slug }),
      });
      setName(''); setSlug('');
      load();
    } catch (err) { setError(String(err)); }
  };

  return (
    <div style={{ padding: 24, maxWidth: 800 }}>
      <h1>Tenants</h1>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <form onSubmit={create} style={{ marginBottom: 24, display: 'flex', gap: 8 }}>
        <input placeholder="Name" value={name} onChange={e => setName(e.target.value)} required />
        <input placeholder="slug" value={slug} onChange={e => setSlug(e.target.value)} required />
        <button type="submit">Create tenant</button>
      </form>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead><tr><th>Name</th><th>Slug</th><th>Plan</th><th>Status</th></tr></thead>
        <tbody>
          {tenants.map(t => (
            <tr key={t.id} style={{ borderTop: '1px solid #ddd' }}>
              <td>{t.name}</td><td>{t.slug}</td><td>{t.plan}</td><td>{t.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
