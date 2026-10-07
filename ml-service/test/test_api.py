
from unittest.mock import Mock

import main


def forecast_payload():
    return {
        "tenantId": 1,
        "category": "Electronics",
        "horizon": 1,
        "historicalSales": [
            {"week": "2026-01-05", "sales": 100},
            {"week": "2026-01-12", "sales": 200},
            {"week": "2026-01-19", "sales": 300},
            {"week": "2026-01-26", "sales": 400},
            {"week": "2026-02-02", "sales": 500},
            {"week": "2026-02-09", "sales": 600},
        ],
    }


def test_health(client):
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "UP"}


def test_forecast_requires_service_token(client):
    response = client.post("/forecast", json=forecast_payload())

    assert response.status_code == 401


def test_forecast_rejects_invalid_service_token(client):
    response = client.post(
        "/forecast",
        headers={"X-ML-Service-Token": "wrong-token"},
        json=forecast_payload(),
    )

    assert response.status_code == 401


def test_forecast_success(client, monkeypatch, valid_token):
    mock_model = Mock()
    mock_model.predict.return_value = [1000.0]

    monkeypatch.setattr(main, "model", mock_model)
    monkeypatch.setattr(main, "ML_SERVICE_TOKEN", valid_token)

    response = client.post(
        "/forecast",
        headers={"X-ML-Service-Token": valid_token},
        json=forecast_payload(),
    )

    assert response.status_code == 200

    body = response.json()

    assert body["category"] == "Electronics"
    assert body["horizon"] == 1
    assert len(body["weeklyPredictions"]) == 1
    assert body["weeklyPredictions"][0]["predictedSales"] == 1000.0
    assert body["predictedSales"] == 1000.0


def test_forecast_multiple_horizon(client, monkeypatch, valid_token):
    mock_model = Mock()
    mock_model.predict.return_value = [1000.0]

    monkeypatch.setattr(main, "model", mock_model)
    monkeypatch.setattr(main, "ML_SERVICE_TOKEN", valid_token)

    payload = forecast_payload()
    payload["horizon"] = 3

    response = client.post(
        "/forecast",
        headers={"X-ML-Service-Token": valid_token},
        json=payload,
    )

    assert response.status_code == 200

    body = response.json()

    assert len(body["weeklyPredictions"]) == 3
    assert body["predictedSales"] == 3000.0
    assert mock_model.predict.call_count == 3


def test_reload_model_requires_service_token(client):
    response = client.post("/reload-model")

    assert response.status_code == 401


def test_reload_model_rejects_invalid_token(client):
    response = client.post(
        "/reload-model",
        headers={"X-ML-Service-Token": "wrong-token"},
    )

    assert response.status_code == 401


def test_reload_model_success(client, monkeypatch, valid_token):
    load_model = Mock()

    monkeypatch.setattr(main, "load_latest_model", load_model)
    monkeypatch.setattr(main, "ML_SERVICE_TOKEN", valid_token)

    response = client.post(
        "/reload-model",
        headers={"X-ML-Service-Token": valid_token},
    )

    assert response.status_code == 200
    assert response.json() == {"status": "Model reloaded"}
    load_model.assert_called_once()
