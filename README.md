# QuickBooks Commerce Sales & Forecast

## Project Overview

A sales analytics and forecasting demo that helps users explore historical sales performance, identify top-selling products, and forecast future sales by category.

## Key Features

* Sales by category with selectable date ranges
* Daily sales drilldown by category
* Top-selling products ranked by historical sales amount
* Category-level sales forecasting for the next week, month, or year
* Forecast model comparison and backtesting using Mean Absolute Error (MAE)
* Interactive Angular dashboard

## Technology Stack

* **Frontend:** Angular, AG Grid, Chart.js
* **Backend:** Java, Spring Boot
* **Database:** PostgreSQL
* **Forecasting:** Python, FastAPI, XGBoost
* **API Documentation:** Swagger / OpenAPI

## Service Quality

### High Availability

The demo separates the frontend, backend API, database, and forecasting service into distinct components.

For a production deployment, availability could be improved through multiple backend instances, load balancing, health checks, service monitoring, and database backup and recovery.

These production capabilities are design considerations and are not claimed as implemented in the local demo.

### Forecast Accuracy

The application supports model comparison and backtesting using Mean Absolute Error (MAE).

MAE measures the average absolute difference between predicted and actual values. Comparing models on historical data helps evaluate their forecasting performance. Results should be interpreted in the context of the data and evaluation period; they do not guarantee future accuracy.

### Data Consistency

The dashboard uses the selected date range and category when retrieving sales data.

* Sales by Category and Top-Selling Products refresh based on the selected filters.
* Top-Selling Products ranks historical sales by total sales amount.
* Forecast and model comparison results can be cleared or refreshed when the date range changes, avoiding the display of results calculated for an earlier selection.

## Forecasting Scope

The current forecasting feature predicts future sales at the **category level**.

Top-Selling Products shows **historical product rankings** for the selected date range. It does not currently forecast future sales for individual products.

## Running the Demo

The application is designed to run locally with separate frontend, backend, database, and ML services.

Refer to the project configuration and service-specific setup instructions for local startup details.
