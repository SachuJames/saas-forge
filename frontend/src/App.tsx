import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import AdminTenants from './admin/AdminTenants';
import TenantBilling from './tenant/TenantBilling';
import TenantAudit from './tenant/TenantAudit';

function Login() {
  const login = () => {
    // Keycloak direct login; backend validates the JWT
    const kc = 'http://localhost:8180/realms/tenantkit';
    const redirect = encodeURIComponent(window.location.origin + '/app');
    window.location.href =
      `${kc}/protocol/openid-connect/auth?client_id=tenantkit-app&response_type=token&redirect_uri=${redirect}`;
  };
  return (
    <div style={{ padding: 48 }}>
      <h1>TenantKit</h1>
      <p>Multi-tenant SaaS starter kit.</p>
      <button onClick={login}>Log in with Keycloak</button>
      <p><Link to="/admin">Admin portal</Link></p>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/admin" element={<AdminTenants />} />
        <Route path="/app/billing" element={<TenantBilling />} />
        <Route path="/app/audit" element={<TenantAudit />} />
      </Routes>
    </BrowserRouter>
  );
}
