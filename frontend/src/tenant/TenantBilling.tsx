import { useEffect, useState } from 'react';
import { api, getTenant } from '../api';

interface Subscription {
  plan: string;
  status: string;
  currentPeriodEnd: string | null;
}

export default function TenantBilling() {
  const [sub, setSub] = useState<Subscription | null>(null);
  const [error, setError] = useState('');
  const tenant = getTenant();

  useEffect(() => {
    api<Subscription>('/billing/subscription', {}, tenant)
      .then(setSub).catch(e => setError(String(e)));
  }, [tenant]);

  const checkout = async (plan: string) => {
    setError('');
    try {
      const { url } = await api<{ url: string }>('/billing/checkout', {
        method: 'POST',
        body: JSON.stringify({ plan }),
      }, tenant);
      window.location.href = url;
    } catch (e) { setError(String(e)); }
  };

  return (
    <div style={{ padding: 24, maxWidth: 700 }}>
      <h1>Billing</h1>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {sub ? (
        <div>
          <p>Plan: <strong>{sub.plan}</strong></p>
          <p>Status: {sub.status}</p>
          {sub.currentPeriodEnd && <p>Renews: {sub.currentPeriodEnd}</p>}
        </div>
      ) : <p>Loading…</p>}
      <h2>Upgrade</h2>
      <div style={{ display: 'flex', gap: 12 }}>
        <button onClick={() => checkout('PRO')}>Go Pro</button>
        <button onClick={() => checkout('ENTERPRISE')}>Go Enterprise</button>
      </div>
    </div>
  );
}
