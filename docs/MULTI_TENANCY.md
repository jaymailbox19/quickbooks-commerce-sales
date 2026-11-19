# Multi-business tenancy architecture (CSFP)

## Decision

A user identity may be a member of multiple businesses. The user authenticates once, retrieves their authorized workspaces, and selects an active workspace. Membership is explicit and role-based.

Each business owns its own:
- categories (category names are unique only within that business);
- products (product names are unique only within that business);
- sales and forecast history.

## Implemented on the hardening branch

The active branch includes:
- user, tenant, and membership JPA entities;
- email/password-hash authentication and BCrypt password verification;
- short-lived workspace-selection JWTs and active-workspace JWTs;
- workspace listing limited to the authenticated user's memberships;
- membership validation before switching to a workspace;
- controller tenant context derived from the authenticated JWT, not a caller-supplied tenant ID;
- tenant predicates in sales aggregation and product-ranking queries;
- tenant predicates passed through category/product forecasts and both backtest paths;
- a PostgreSQL schema migration defining tenant ownership and composite foreign keys;
- an opt-in first-owner bootstrap, disabled by default, which creates an OWNER only when the user table is empty.

The first-owner bootstrap requires `APP_BOOTSTRAP_ENABLED=true`, `APP_BOOTSTRAP_OWNER_EMAIL`, and `APP_BOOTSTRAP_OWNER_PASSWORD` to be configured outside source control. The configured tenant must already exist. Use it only for controlled initial provisioning, then disable it. The bootstrap does not reset or elevate existing accounts.

## Identity and workspace flow

1. Authenticate with a user identity.
2. Return workspace memberships and roles available to that identity.
3. User selects a workspace.
4. Backend validates that the authenticated user has an active membership in the selected workspace.
5. Backend issues a short-lived access token containing stable user ID, active tenant ID, and role.
6. Every protected request derives the tenant from the validated security principal. A tenant ID supplied in a query string or request body is never an authorization source.
7. Switching workspace repeats membership validation and issues a new active-workspace token.

Recommended roles: OWNER, ADMIN, MANAGER, VIEWER. Role names are currently carried in the token; authorization policy for write operations still needs to be implemented explicitly. Membership changes may not take effect for an already-issued active-workspace token until it expires.

## Data model

- `tenants`: business/workspace.
- `app_users`: login identity; email is unique.
- `tenant_memberships`: many-to-many user/business relationship and role.
- `categories`: tenant-owned category catalog.
- `products`: tenant-owned products, each referencing a category in the same tenant.
- `sales`: tenant-owned transactions, each referencing a product in the same tenant.

Composite constraints enforce catalog and transaction ownership:
- `products(tenant_id, category_id) -> categories(tenant_id, id)`
- `sales(tenant_id, product_id) -> products(tenant_id, id)`

The migration in `database/migrations/V2__tenant_workspaces.sql` is a manual schema migration at present; it is not wired to Flyway or another automated migration runner. Back up and test it against a disposable copy of the database before applying it. Reconcile the legacy SQL bootstrap script and Hibernate schema-update settings before treating deployment as repeatable.

## Forecasting and ML boundary

The Spring Boot service obtains tenant-scoped historical data and sends the selected tenant's series to FastAPI over the authenticated service channel. The checked-in XGBoost artifact is still a shared model. This migration does not make it tenant-specific and does not establish permission to pool customer data for training.

Before production or customer data:
- choose and document either per-tenant models or an explicitly consented, privacy-reviewed pooled model;
- ensure training jobs filter by tenant and have a documented data-use policy;
- include tenant and model/version metadata in audit records;
- ensure backtests use the same tenant-scoped data path as live inference.

## Validation and remaining work

No Maven build or automated integration test result is claimed for this branch. The GitHub repository currently has no workflow run associated with the latest hardening commits. The following validation remains required:

1. A user can list only workspaces for which they have a membership.
2. A user cannot switch to a workspace without membership.
3. Same category/product names can exist in different tenants.
4. A product cannot reference a category owned by another tenant.
5. A sale cannot reference a product owned by another tenant.
6. Tenant A cannot read Tenant B sales, products, categories, rankings, forecasts, exports, or backtests.
7. The browser cannot override active tenant context using a query/body parameter.
8. ML requests contain only authorized tenant history; training and backtesting cannot silently mix tenants.
9. Role checks prevent viewers from performing write operations.
10. Tests cover workspace switching, token expiry, revoked memberships, disabled users, and the bootstrap's disabled/idempotent behavior.
