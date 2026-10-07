import pandas as pd
import json
from sklearn.metrics import mean_absolute_error

from db import get_sales
from feature_engineering import create_features
from model_training import train_model, save_model



def train_and_evaluate():

    rows = get_sales()
    historical_sales = [
        {
            "tenant_id": tenant_id,
            "category": category,
            "week": week,
            "sales": float(sales)
        }
        for tenant_id,category, week, sales in rows
    ]

    df = create_features(historical_sales)
    categories = sorted(df["category"].unique())

    print(df.head())
    print("Feature rows:", len(df))


    cutoff_date = df["week"].max() - pd.Timedelta(weeks=1)

    train_df = df[df["week"] < cutoff_date]
    test_df = df[df["week"] >= cutoff_date]
    

    print("Train rows:", len(train_df))
    print("Test rows:", len(test_df))
    print("\nTest data:")
    print(
    test_df[
        [
            "tenant_id",
            "category",
            "week",
            "sales",
            "sales_lag_1",
            "sales_lag_2",
            "sales_lag_3",
            "sales_lag_4",
            "rolling_avg_4",
        ]
    ].to_string(index=False)
    )
    print(
        "Training period:",
        train_df["week"].min(),
        "to",
        train_df["week"].max()
    )

    print(
        "Test period:",
        test_df["week"].min(),
        "to",
        test_df["week"].max()
    )


    # XGBoost training and evaluation

    # Numeric features
    numeric_features = [
        "sales_lag_1",
        "sales_lag_2",
        "sales_lag_3",
        "sales_lag_4",
        "rolling_avg_4",
        "growth_rate",
        "week_of_year"
    ]

    # Convert category names into numeric columns
    category_features = pd.get_dummies(
        df["category"],
        prefix="category",
        dtype=int
    )

    # Combine numeric and category features
    X = pd.concat(
        [df[numeric_features], category_features],
        axis=1
    )
    feature_columns = list(X.columns)
    with open("feature_schema.json", "w") as f:
        json.dump(
        {
            "feature_columns": feature_columns
        },
        f,
        indent=2
    )

    # Keep the same chronological train/test split
    X_train = X.loc[train_df.index]
    y_train = train_df["sales"]

    X_test = X.loc[test_df.index]
    y_test = test_df["sales"]

    model = train_model(X_train, y_train)

    predictions = model.predict(X_test)

    print("Predictions:", predictions)
    print("Actual:", y_test.values)

    mae = mean_absolute_error(y_test, predictions)

    print("XGBoost MAE:", mae)
    print("Training features:", X_train.columns.tolist())
    print("Training shape:", X_train.shape)


    baseline_predictions = test_df["rolling_avg_4"]

    baseline_mae = mean_absolute_error(
        y_test,
        baseline_predictions
    )

    print("Moving average baseline MAE:", baseline_mae)
    print("\nRows per tenant/category:")
    print(
    df.groupby(["tenant_id", "category"]).size()
    )   

    save_model(model, "model.json")
    print("Model saved to model.json")

if __name__ == "__main__":
    train_and_evaluate()