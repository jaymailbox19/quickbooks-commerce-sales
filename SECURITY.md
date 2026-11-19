# Security and local configuration

- Never commit real credentials, API tokens, private keys, or production data.
- Copy .env.example to .env and replace every placeholder with unique local values.
- Values in .env.example are placeholders only; they are not valid credentials.
- Treat any credential previously committed to Git as compromised. Rotate it in the service that issued it.
- Removing a secret from the latest commit does not remove it from Git history. History rewriting and force-pushing require separate, coordinated remediation.
- Do not deploy this prototype with real customer data until tenant authorization, schema constraints, ML data handling, and cross-tenant tests have been validated end to end.

## Multi-business tenancy status

The active branch now contains:
- explicit user, tenant, and membership entities;
- password-hash-based login and short-lived workspace-selection tokens;
- membership validation before issuing an active-workspace token;
- tenant context derived from the authenticated JWT in sales and prediction controllers;
- tenant-scoped sales repository queries and a tenant/workspace schema migration;
- an opt-in first-owner bootstrap that hashes the configured password with BCrypt.

The first-owner bootstrap is disabled by default. Enable it only in a controlled initial setup after the tenant schema and configured tenant exist. It creates an OWNER only when the application user table is empty; it does not reset or elevate an existing account. Supply the email and password through deployment environment variables, never source control.

**Not production-ready yet:** no application build or integration tests have been executed as part of this code review. The SQL migration is not wired to an automated migration runner, and the current checked-in XGBoost model remains shared across tenants. Role-based authorization for write operations, membership-revocation behavior, model/training isolation policy, and end-to-end cross-tenant tests remain open. Back up and test the migration against a disposable database before applying it.

See [docs/MULTI_TENANCY.md](docs/MULTI_TENANCY.md) for the target architecture and acceptance criteria.
