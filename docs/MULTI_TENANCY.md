# Multi-business tenancy architecture (CSFP)

## Decision

A user identity may be a member of multiple businesses. The user authenticates once, retrieves their authorized workspaces, and selects an active workspace. Membership is explicit and role-based.

Each business owns its own categories, products, sales, and forecast history. Category and product names are unique within a business rather than globally.

## Implemented on the hardening branch

The active branch includes user, tenant, and membership entities; email/password-hash authentication with BCrypt; short-lived workspace-selection and active-workspace JWTs; membership validation before workspace switching; JWT-derived tenant context in sales and prediction controllers; tenant predicates in sales, product-ranking, forecast, and backtest data paths; Flyway migrations; Hibernate schema validation; and an opt-in first-owner bootstrap disabled by default.

The bootstrap requires APP_BOOTSTRAP_ENABLED=true, APP_BOOTSTRAP_OWNER_EMAIL, and APP_BOOTSTRAP_OWNER_PASSWORD outside source control. The configured tenant must already exist. Use only for controlled initial provisioning, then disable it. It does not reset or elevate existing accounts.

## Identity and workspace flow

1. Authenticate with a user identity.
2. Return only workspaces for which that identity has a membership.
3. The user selects a workspace.
4. The backend validates the user's membership in that workspace.
5. The backend issues a short-lived token containing user ID, active tenant ID, and role.
6. Protected requests derive tenant identity from the validated security principal, never from a query string or request body.
7. Switching workspace repeats membership validation and issues a new active-workspace token.

Roles are OWNER, ADMIN, MANAGER, and VIEWER. Role names are currently carried in the token; explicit authorization policy for write operations remains to be implemented. Membership changes may not take effect for an already-issued active token until it expires.

## Data model

- tenants: business/workspace.
- app_users: login identity; email is unique.
- tenant_memberships: many-to-many user/business relationship and role.
- categories: tenant-owned category catalog.
- products: tenant-owned products, each referencing a category in the same tenant.
- sales: tenant-owned transactions, each referencing a product in the same tenant.

Composite constraints enforce products(tenant_id, category_id) -> categories(tenant_id, id) and sales(tenant_id, product_id) -> products(tenant_id, id).

## Migration and rollout

Flyway migrations live in backend/src/main/resources/db/migration:

- V1__initial_schema.sql creates the original categories/products/sales schema for a clean database.
- V2__tenant_workspaces.sql creates tenants, users, memberships, tenant columns, tenant-scoped uniqueness, and composite foreign keys.

For a clean database, Flyway applies V1 and V2 in order. For an existing non-empty prototype database without Flyway history, spring.flyway.baseline-on-migrate=true baselines the existing schema at version 1 and then applies V2. Existing catalog and sales rows are assigned to Demo Tenant. Back up and rehearse the migration against a disposable database before applying it to valuable data.

Hibernate uses ddl-auto=validate; it must not create or alter the schema. Spring automatic data.sql execution is disabled. Optional demonstration rows are in database/demo_seed.sql and must be run manually after migrations.

## Forecasting and ML boundary

Spring Boot obtains tenant-scoped historical data and sends the selected tenant's series to FastAPI over the authenticated service channel. The checked-in XGBoost artifact is still a shared model. This migration does not make it tenant-specific and does not establish permission to pool customer data for training.

Before production or customer data, choose either per-tenant models or an explicitly consented, privacy-reviewed pooled model; filter training jobs by tenant; document data use; record tenant and model/version metadata; and ensure backtests use the same tenant-scoped data path as live inference.

## Validation and remaining work

No Maven build or automated integration test result is claimed for this branch. Required validation includes:

1. A user can list only workspaces for which they have a membership.
2. A user cannot switch to a workspace without membership.
3. Same category/product names can exist in different tenants.
4. A product cannot reference a category owned by another tenant.
5. A sale cannot reference a product owned by another tenant.
6. Tenant A cannot read Tenant B sales, products, categories, rankings, forecasts, exports, or backtests.
7. The browser cannot override active tenant context using a query/body parameter.
8. ML requests and training/backtesting cannot silently mix tenants.
9. Role checks prevent viewers from performing write operations.
10. Tests cover clean-database migration, existing-database baseline migration, workspace switching, token expiry, revoked memberships, disabled users, and bootstrap disabled/idempotent behavior.
