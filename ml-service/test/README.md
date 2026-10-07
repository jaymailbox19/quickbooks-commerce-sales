
# Python ML Service Tests

Pytest test suite for the QuickBooks Commerce sales forecasting Python service.

## Test coverage

### Feature engineering
- Lag 1-4 calculation
- Four-week rolling average
- Target-leakage protection
- Growth-rate calculation
- Week-of-year feature
- Tenant/category isolation
- Zero-denominator handling

### Model
- XGBoost model training
- Prediction generation

### FastAPI service
- Health endpoint
- Forecast authentication
- Invalid service token
- Successful forecast
- Multi-horizon recursive forecasting
- Model reload authentication
- Successful model reload

## Install

```bash
pip install pytest httpx
```

Make sure the Python ML service dependencies are already installed:

```bash
pip install pandas numpy scikit-learn xgboost fastapi uvicorn python-dotenv psycopg2-binary
```

## Run

From the Python ML service directory:

```bash
pytest -v
```

For coverage:

```bash
pip install pytest-cov
pytest --cov=. --cov-report=term-missing
```

## Interview explanation

"The Python service has automated unit tests for feature engineering and model behavior, plus API tests for the FastAPI endpoints and service-to-service authentication. I mock the XGBoost model in API tests so the endpoint tests are deterministic. Model accuracy itself is evaluated separately using the backtesting/comparison flow rather than treating code coverage as proof of model quality."
