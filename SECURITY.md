# Security and local configuration

- Never commit real credentials, API tokens, private keys, or production data.
- Copy .env.example to .env and replace every placeholder with unique local values.
- Values in .env.example are placeholders only; they are not valid credentials.
- Treat any credential previously committed to Git as compromised. Rotate it in the service that issued it.
- Removing a secret from the latest commit does not remove it from Git history. History rewriting and force-pushing require separate, coordinated remediation.
- Do not deploy this prototype with real customer data until tenant authorization is enforced consistently in authentication, API handlers, database queries, and model-training data access.

## Multi-business tenancy status

The selected target design supports one user belonging to multiple businesses through explicit tenant memberships and an active workspace. See [docs/MULTI_TENANCY.md](docs/MULTI_TENANCY.md) and the database migration in `database/migrations/V2__tenant_workspaces.sql`.

**Important:** the current application is not yet tenant-isolated end to end. Existing repository queries and demo authentication still require application changes and cross-tenant tests. The SQL migration is a schema foundation only; it does not make the application safe for customer data. Back up and test the migration against a disposable database before applying it.
