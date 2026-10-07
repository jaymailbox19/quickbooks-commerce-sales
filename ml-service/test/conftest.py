
import os
import pytest
from fastapi.testclient import TestClient

# Set the token before importing main.py, because the application reads it at import time.
os.environ.setdefault("ML_SERVICE_TOKEN", "test-token")

from main import app


@pytest.fixture
def client():
    return TestClient(app)


@pytest.fixture
def valid_token():
    return "test-token"


@pytest.fixture
def historical_sales():
    return [
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-05", "sales": 100},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-12", "sales": 200},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-19", "sales": 300},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-26", "sales": 400},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-02-02", "sales": 500},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-02-09", "sales": 600},
    ]
