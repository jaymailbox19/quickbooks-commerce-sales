import os
import json
import secrets

from dotenv import load_dotenv
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel
from datetime import date
from decimal import Decimal
import pandas as pd

from feature_engineering import create_features
from xgboost import XGBRegressor

load_dotenv()
app = FastAPI()

ML_SERVICE_TOKEN = os.environ["ML_SERVICE_TOKEN"]

MODEL_PATH = "model.json"

model = XGBRegressor()

def load_feature_schema():
    with open("feature_schema.json", "r") as f:
        return json.load(f)
    
def load_latest_model():
    global model

    new_model = XGBRegressor()
    new_model.load_model(MODEL_PATH)

    model = new_model

    print("Latest model loaded successfully.")


load_latest_model()


class WeeklySales(BaseModel):
    week: date
    sales: Decimal


class ForecastRequest(BaseModel):
    tenantId: int
    horizon: int = 1
    category: str
    historicalSales: list[WeeklySales]


@app.get("/health")
def health():
    return {"status": "UP"}

@app.post("/reload-model")
def reload_model(
    service_token: str | None = Header(
        default=None,
        alias="X-ML-Service-Token"
    )
):
    if (
        service_token is None
        or not secrets.compare_digest(service_token, ML_SERVICE_TOKEN)
    ):
        raise HTTPException(
            status_code=401,
            detail="Unauthorized"
        )

    load_latest_model()

    return {"status": "Model reloaded"}


@app.post("/forecast")
def forecast(
    request: ForecastRequest,
    service_token: str | None = Header(
        default=None,
        alias="X-ML-Service-Token"
    )
):
    if (
        service_token is None
        or not secrets.compare_digest(service_token, ML_SERVICE_TOKEN)
    ):
        raise HTTPException(
            status_code=401,
            detail="Unauthorized"
        )
    if request.category == "BadTest":
        raise HTTPException(
            status_code=400,
            detail="Invalid test category"
        )

    # Build historical sales using the tenant from the request
    historical_sales = [
        {
            "tenant_id": request.tenantId,
            "category": request.category,
            "week": item.week,
            "sales": float(item.sales)
        }
        for item in request.historicalSales
    ]

    numeric_features = [
        "sales_lag_1",
        "sales_lag_2",
        "sales_lag_3",
        "sales_lag_4",
        "rolling_avg_4",
        "growth_rate",
        "week_of_year"
    ]

    # Load the feature schema used during model training
    feature_schema = load_feature_schema()
    feature_columns = feature_schema["feature_columns"]

    # Get category columns dynamically from the saved schema
    category_columns = [
        column
        for column in feature_columns
        if column.startswith("category_")
    ]

    weekly_predictions = []

    for _ in range(request.horizon):

        # Build features using historical and previously predicted sales
        df = create_features(historical_sales)

        # Create category features dynamically
        category_features = pd.get_dummies(
            df["category"],
            prefix="category",
            dtype=int
        )

        # Align category features with the training schema
        category_features = category_features.reindex(
            columns=category_columns,
            fill_value=0
        )

        # Combine numeric and categorical features
        X = pd.concat(
            [
                df[numeric_features].reset_index(drop=True),
                category_features.reset_index(drop=True)
            ],
            axis=1
        )

        # Ensure the exact same feature order as training
        X = X.reindex(
            columns=feature_columns,
            fill_value=0
        )

        # Get the latest row for the next-week prediction
        latest_features = X.tail(1)

        prediction = float(
            model.predict(latest_features)[0]
        )

        # Determine the next week from the latest historical date
        latest_week = max(
            item["week"]
            for item in historical_sales
        )

        next_week = latest_week + pd.Timedelta(weeks=1)

        weekly_predictions.append({
            "week": next_week.isoformat(),
            "predictedSales": round(prediction, 2)
        })

        # Add the prediction to history for recursive forecasting
        historical_sales.append({
            "tenant_id": request.tenantId,
            "category": request.category,
            "week": next_week,
            "sales": prediction
        })

    return {
        "category": request.category,
        "horizon": request.horizon,
        "weeklyPredictions": weekly_predictions,
        "predictedSales": round(
            sum(
                item["predictedSales"]
                for item in weekly_predictions
            ),
            2
        )
    }