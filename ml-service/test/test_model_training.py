
import pandas as pd

from model_training import train_model


def sample_data():
    X = pd.DataFrame({
        "sales_lag_1": [100, 200, 300, 400, 500, 600],
        "sales_lag_2": [90, 100, 200, 300, 400, 500],
        "sales_lag_3": [80, 90, 100, 200, 300, 400],
        "sales_lag_4": [70, 80, 90, 100, 200, 300],
        "rolling_avg_4": [85, 117.5, 147.5, 197.5, 297.5, 397.5],
        "growth_rate": [0.10, 1.0, 0.5, 0.333, 0.25, 0.20],
        "week_of_year": [1, 2, 3, 4, 5, 6],
    })
    y = pd.Series([150, 250, 350, 450, 550, 650])
    return X, y


def test_model_training_returns_model():
    X, y = sample_data()

    model = train_model(X, y)

    assert model is not None


def test_model_generates_prediction():
    X, y = sample_data()

    model = train_model(X, y)
    prediction = model.predict(X.iloc[[0]])

    assert len(prediction) == 1
    assert isinstance(float(prediction[0]), float)
