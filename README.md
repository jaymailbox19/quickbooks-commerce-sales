# Commerce Sales Forecasting Platform

An independent commerce analytics and category-level sales forecasting prototype.

## Overview

The platform combines historical sales analytics with category-level forecasts. It is intended as a research and engineering prototype for evaluating a modular forecasting workflow.

## Features

- Sales aggregation by category and selectable date ranges
- Daily sales drill-down
- Historical top-product rankings
- Category-level forecasts for weekly, monthly, and yearly horizons
- XGBoost forecasting with a four-week moving-average baseline and service-unavailability fallback
- Model comparison and historical backtesting endpoints
- Angular dashboard and REST API
- Separate Python inference and model-training service

## Architecture

- **Frontend:** Angular
- **Application API:** Java and Spring Boot
- **Database:** PostgreSQL
- **ML service:** Python and FastAPI
- **Forecast model:** XGBoost
- **API documentation:** Swagger / OpenAPI

The browser calls the Spring Boot API. The backend aggregates historical sales and calls the Python ML service for inference. The Python service also contains the model-training and refresh workflow.

## Research status and limitations

This repository is an independent prototype. It is not a product of, affiliated with, sponsored by, or endorsed by Intuit or any other commerce software vendor.

The checked-in sample data is demonstration data and is not sufficient to support generalizable claims about forecasting accuracy. Model performance should be reported only after reproducible, chronological evaluation on a sufficiently large and documented dataset.

Multi-tenant authorization, credential management, and rolling-origin evaluation are under active review. Do not deploy this prototype with real customer data until these controls have been implemented and independently tested.

## Local development

See the backend, frontend, and ML-service directories for component-specific setup. Store secrets in environment variables or a secrets manager. Do not commit environment files, credentials, access tokens, or production data.

## License

No license is currently granted by this repository. All rights reserved unless and until an explicit license is added.
