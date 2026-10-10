const API = '/api';

function authHeaders(tenantId?: string): HeadersInit {
  const token = localStorage.getItem('tk_token');
  const h: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) h['Authorization'] = `Bearer ${token}`;
  if (tenantId) h['X-Tenant-ID'] = tenantId;
  return h;
}

export async function api<T>(path: string, opts: RequestInit = {}, tenantId?: string): Promise<T> {
  const res = await fetch(API + path, { ...opts, headers: { ...authHeaders(tenantId), ...opts.headers } });
  if (!res.ok) throw new Error(`${res.status} ${await res.text()}`);
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export function setToken(t: string) { localStorage.setItem('tk_token', t); }
export function getTenant(): string { return localStorage.getItem('tk_tenant') || ''; }
export function setTenant(t: string) { localStorage.setItem('tk_tenant', t); }
