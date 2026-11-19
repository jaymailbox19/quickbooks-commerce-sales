# Multi-business tenancy architecture (CSFP)

## Decision

A user identity may be a member of multiple businesses. The user authenticates once, retrieves their authorized workspaces, and selects an active workspace. Membership is explicit and role-based.

Each business owns its own:
- categories (category names are unique only within that business);
- products (product names are unique only within that business);
- sales and forecast history.

## Identity and workspace flow

1. Authenticate with a user identity.
2. Return workspace memberships and roles available to that identity.
3. User selects a workspace.
4. Backend validates that the authenticated user has an active membership in the selected workspace.
5. Backend issues a short-lived access token containing stable user ID, active tenant ID, and role (or creates an equivalent server-side workspace context).
6. Every protected request derives the tenant from the validated security principal. A tenant ID supplied in a query string or request body is never an authorization source.
7. Switching workspace repeats membership validation and issues a new active-workspace token.

Recommended roles: OWNER, ADMIN, MANAGER, VIEWER. Authorization policy should be explicit per operation; role names alone do not enforce permissions.

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

Every repository query must include the active tenant predicate. Apply this to sales, categories, products, rankings, forecasts, exports, and backtests. Add database row-level security as defense in depth only after connection-pool/session-context behavior is designed and tested.

## Forecasting and ML boundary

The Spring Boot service obtains tenant-scoped historical data and sends only the authorized tenant's series to FastAPI over the authenticated service channel. FastAPI must validate the service token and must not independently trust a browser-provided tenant identifier.

The current checked-in XGBoost artifact is a shared model. This migration does not make it tenant-specific and does not establish permission to pool customer data for training. Before production or customer data:
- choose and document either per-tenant models or an explicitly consented, privacy-reviewed pooled model;
- ensure training jobs filter by tenant and have a documented data-use policy;
- include tenant and model/version metadata in audit records;
- ensure backtests use the same tenant-scoped data path as live inference.

## API contract changes required

- Remove `tenantId` from public forecast query parameters.
- Add an authenticated workspace listing endpoint.
- Add a workspace-switch endpoint that validates membership before issuing an active-workspace token.
- Resolve tenant ID and role from the authenticated principal in controllers/services.
- Reject missing or invalid active-workspace context; never fall back to a global query.

## Migration and rollout

The companion SQL migration assigns existing prototype categories, products, and sales to the existing Demo Tenant. Take a database backup and test against a disposable copy before running it. It creates identity/membership tables but does not create production users or convert the demo login into a production identity provider.

The migration is not proof that the running application is tenant-safe. The application currently contains unscoped repository queries and demo-only authentication. Do not deploy with real customer data until the API, repositories, authentication, ML training/inference, and cross-tenant integration tests are completed.

## Required acceptance tests

1. A user can list only workspaces for which they have a membership.
2. A user cannot switch to a workspace without membership.
3. Same category/product names can exist in different tenants.
4. A product cannot reference a category owned by another tenant.
5. A sale cannot reference a product owned by another tenant.
6. Tenant A cannot read Tenant B sales, products, categories, rankings, forecasts, exports, or backtests.
7. The browser cannot override active tenant context using a query/body parameter.
8. ML requests contain only authorized tenant history; training and backtesting cannot silently mix tenants.
9. Role checks prevent viewers from performing write operations.
10. Tests cover workspace switching, token expiry, revoked memberships, and disabled users.
