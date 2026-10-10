# TenantForge

[![CI](https://github.com/SachuJames/tenant-forge/actions/workflows/ci.yml/badge.svg)](https://github.com/SachuJames/tenant-forge/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Java 21](https://img.shields.io/badge/Java-21-blue.svg)
![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3-brightgreen.svg)
![React](https://img.shields.io/badge/React-18-61dafb.svg)

A production-grade multi-tenant SaaS starter kit. Most starter kits give you
auth and a database and stop there. This one handles the parts that actually
hurt in production: tenant isolation, subscription billing, audit trails, and
GDPR tooling, from day one.

## Features

- **True tenant isolation** with schema-per-tenant Postgres. Each tenant gets
  its own schema, provisioned automatically with Flyway migrations. One bad
  query can never leak another tenant's data.
- **OAuth2/OIDC login** via Keycloak. Realm roles map to Spring Security
  authorities, so `@PreAuthorize("hasRole('ADMIN')")` just works.
- **Stripe billing** end to end: checkout sessions, customer portal, and
  webhooks that keep each tenant's subscription status in sync. Feature
  gating by plan included.
- **Audit logging** with a single `@Audited` annotation on any service
  method. Every action lands in the tenant's own audit trail.
- **GDPR tools**: one-click data export and right-to-erasure for users.
- **Two portals**: an admin portal (tenants, plans, subscriptions) and a
  tenant portal (users, billing, audit log).

## Architecture

```
                        ┌──────────────────┐
                        │  React portals   │  admin + tenant
                        └────────┬─────────┘
                                 │ REST / JWT
┌──────────┐            ┌────────▼─────────┐            ┌──────────┐
│ Keycloak │◄───────────│   Spring Boot    │───────────►│  Stripe  │
│  (OIDC)  │  tokens    │                  │  webhooks  │ (billing)│
└──────────┘            └────────┬─────────┘            └──────────┘
                                 │ pgx / JPA
                        ┌────────▼─────────┐
                        │    Postgres      │
                        │ public: tenants  │
                        │ tenant_acme: ... │
                        │ tenant_globex:.. │
                        └──────────────────┘
```

The backend resolves the tenant per request (the `X-Tenant-ID` header, or the
`tenant_id` claim on the JWT) and points the JDBC connection at that tenant's
schema. Repositories never think about tenancy. The `public` schema holds only
the tenant registry.

## Tech stack

| Layer    | Choice                                                              |
| -------- | ------------------------------------------------------------------- |
| Backend  | Java 21, Spring Boot 3, Spring Security 6, Spring Data JPA, Flyway   |
| Tenancy  | Schema-per-tenant, tenant-aware DataSource, per-schema migrations   |
| Auth     | Keycloak (OIDC), JWT resource server                                |
| Billing  | Stripe (Checkout, webhooks, subscription mirroring)                 |
| Database | PostgreSQL 16                                                      |
| Frontend | React 18, TypeScript, Vite, React Router                            |
| Infra    | Docker Compose (app, Postgres, Keycloak)                            |

## Quickstart

Prerequisites: Docker and Docker Compose.

```bash
git clone https://github.com/SachuJames/tenant-forge.git
cd tenant-forge/deploy
docker compose up --build
```

Then:

- API: http://localhost:8080
- Admin portal: http://localhost:5173
- Keycloak: http://localhost:8180 (admin/admin)

The compose file seeds a demo realm and a demo tenant. Log in with
`admin@demo.local` / `changeme` to see the tenant portal, or use the admin
account for the admin portal.

### Without Docker

You need Java 21, Node 20+, Postgres 16, and Keycloak 24+.

```bash
# backend
cd backend
./mvnw spring-boot:run \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/tenantforge

# frontend
cd frontend
npm install
npm run dev
```

Copy `.env.example` to `.env` in each directory first and fill in your
Keycloak and Stripe values. See [Configuration](#configuration).

## Configuration

All settings come from the environment. Nothing secret is hardcoded.

| Variable                     | Default                                   | Purpose                              |
| ---------------------------- | ----------------------------------------- | ------------------------------------ |
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://localhost:5432/tenantforge` | Postgres connection             |
| `SPRING_DATASOURCE_USERNAME` | `tenantforge`                             | DB user                                |
| `SPRING_DATASOURCE_PASSWORD` | `tenantforge`                             | DB password                            |
| `APP_SECURITY_ISSUER_URI`    | `http://localhost:8180/realms/tenantforge` | Keycloak realm (JWT issuer)          |
| `APP_STRIPE_API_KEY`         | _(empty)_                                  | Stripe secret key (billing disabled if empty) |
| `APP_STRIPE_WEBHOOK_SECRET`  | _(empty)_                                  | Stripe webhook signing secret          |
| `APP_STRIPE_PRICE_PRO`       | _(empty)_                                  | Stripe price ID for the PRO plan       |
| `APP_STRIPE_PRICE_ENTERPRISE`| _(empty)_                                  | Stripe price ID for the ENTERPRISE plan |
| `VITE_API_URL`               | `http://localhost:8080`                    | Backend URL for the portals            |
| `VITE_KEYCLOAK_URL`          | `http://localhost:8180`                    | Keycloak URL for the portals           |

## Project structure

```
tenant-forge/
├── backend/                    # Spring Boot 3 application
│   └── src/main/java/com/tenantforge/
│       ├── tenancy/            # TenantContext, filter, provisioning, registry
│       ├── security/           # Keycloak role mapping
│       ├── billing/            # Stripe checkout, webhooks, subscriptions
│       ├── audit/              # @Audited aspect + audit log
│       ├── gdpr/               # Data export + erasure
│       ├── admin/              # Tenant management API
│       └── user/               # Tenant user management
├── frontend/                   # React portals (admin + tenant)
├── deploy/
│   ├── docker-compose.yml
│   └── keycloak/               # Realm export with demo users
├── .github/workflows/ci.yml
└── demo.sh
```

## API overview

All tenant APIs require the `X-Tenant-ID` header (or a JWT carrying the
`tenant_id` claim).

```
POST /api/admin/tenants              # provision a tenant (ADMIN)
GET  /api/admin/tenants              # list tenants (ADMIN)

GET  /api/users                     # list tenant users
POST /api/users                     # invite a user

GET  /api/billing/subscription      # current subscription
POST /api/billing/checkout          # Stripe checkout URL for a plan

GET  /api/audit-logs                # tenant audit trail

GET  /api/gdpr/export               # download my data (JSON)
DELETE /api/gdpr/erasure            # erase my data

POST /api/public/stripe/webhook     # Stripe events (signature verified)
```

## Testing

```bash
cd backend
./mvnw test        # unit + H2-backed tenancy integration tests

cd frontend
npm test            # component tests
```

The backend suite covers tenant isolation (data written under one tenant is
invisible to another), idempotent provisioning, webhook signature handling,
and the GDPR flows.

## Roadmap

- Row-level tenancy as a second supported strategy
- Usage-based billing (Stripe metered plans)
- Per-tenant feature flags
- SCIM provisioning for enterprise tenants

## License

MIT. See [LICENSE](LICENSE).
