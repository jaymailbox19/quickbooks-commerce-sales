# Security and local configuration

- Never commit real credentials, API tokens, private keys, or production data.
- Copy .env.example to .env and replace every placeholder with unique local values.
- Values in .env.example are placeholders only; they are not valid credentials.
- Treat any credential previously committed to Git as compromised. Rotate it in the service that issued it.
- Removing a secret from the latest commit does not remove it from Git history. History rewriting and force-pushing require separate, coordinated remediation.
- Do not deploy this prototype with real customer data until tenant authorization, schema constraints, ML data handling, and cross-tenant tests have been validated end to end.

## Multi-business tenancy status

The hardening branch contains membership-based authentication, workspace selection, JWT-derived tenant context, tenant-scoped sales and forecast repository paths, Flyway schema migrations, and an opt-in BCrypt first-owner bootstrap.

Database migrations are managed by Flyway. Hibernate validates mappings and does not mutate the schema. Existing databases are baselined at version 1 before Flyway applies the tenant migration at version 2. Always back up and rehearse this migration against a disposable database.

The first-owner bootstrap is disabled by default. Enable it only for controlled initial provisioning after Flyway has created the tenant schema and the configured tenant exists. It creates an OWNER only when the user table is empty; it does not reset or elevate an existing account. Supply credentials through deployment environment variables, never source control.

Not production-ready yet: no application build or integration tests have been executed as part of this code review. Role-based authorization for write operations, membership-revocation behavior, model/training isolation policy, and end-to-end cross-tenant tests remain open. The checked-in XGBoost artifact is shared across tenants.

See docs/MULTI_TENANCY.md for architecture, migration, and validation details.
