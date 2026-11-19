# Commerce Sales Forecasting Platform

An independent commerce analytics and category-level sales forecasting prototype.

## Overview

The platform combines historical sales analytics with category-level forecasts. It is intended as a research and engineering prototype for evaluating a modular forecasting workflow.

## Features

- Sales aggregation by category and selectable date ranges
- Daily sales drill-down and historical top-product rankings
- Category-level forecasts for weekly, monthly, and yearly horizons
- XGBoost forecasting with a four-week moving-average baseline and service-unavailability fallback
- Model comparison and historical backtesting endpoints
- Angular dashboard, Spring Boot REST API, PostgreSQL, and Python FastAPI ML service
- Multi-business workspace foundation with membership-based access

## Database setup

The backend uses Flyway for schema changes and Hibernate in validation-only mode.

- On an empty database, Flyway applies V1 (base schema) and V2 (tenant/workspace schema) in order.
- On an existing non-empty prototype database without Flyway history, baseline-on-migrate marks the existing schema as version 1 and then applies V2.
- Existing categories, products, and sales are assigned to the Demo Tenant by V2. Back up the database and rehearse the migration against a disposable copy first.
- Automatic Spring data.sql seeding is disabled. Optional demonstration rows are in database/demo_seed.sql and must be run manually after migrations.

Configure DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, and ML_SERVICE_TOKEN as environment variables. Use .env.example for placeholders.

To provision the first owner account, temporarily set APP_BOOTSTRAP_ENABLED=true, APP_BOOTSTRAP_OWNER_EMAIL, and APP_BOOTSTRAP_OWNER_PASSWORD in the deployment environment. The tenant name defaults to Demo Tenant. The password is stored as a BCrypt hash. Disable bootstrap after the first owner is created; it does not create or elevate users once any user exists.

## Architecture

- Frontend: Angular
- Application API: Java and Spring Boot
- Database: PostgreSQL, managed by Flyway migrations
- ML service: Python and FastAPI
- Forecast model: XGBoost
- API documentation: Swagger / OpenAPI

The browser calls the Spring Boot API. The backend aggregates historical sales and calls the Python ML service for inference. The Python service also contains the model-training and refresh workflow.

## Research status and limitations

This repository is an independent prototype. It is not a product of, affiliated with, sponsored by, or endorsed by Intuit or any other commerce software vendor.

The checked-in sample data is demonstration data and is not sufficient to support generalizable claims about forecasting accuracy. Model performance should be reported only after reproducible, chronological evaluation on a sufficiently large and documented dataset.

The active hardening branch implements tenant-aware API and repository paths, but this is not yet a production-validated multi-tenant system. Cross-tenant integration tests, role-based write authorization, membership revocation behavior, and an explicit model-training data policy remain required. The checked-in XGBoost model is shared across tenants. Do not use real customer data until these controls have been validated end to end.

## License

No license is currently granted by this repository. All rights reserved unless and until an explicit license is added.
