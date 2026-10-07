import pandas as pd


def create_features(historical_sales):
    df = pd.DataFrame(historical_sales)

    df["week"] = pd.to_datetime(df["week"])

    # Sort each tenant/category time series chronologically.
    df = (
        df.sort_values(
            ["tenant_id", "category", "week"]
        )
        .reset_index(drop=True)
    )

    # Calculate features independently for each tenant + category.
    group = df.groupby(["tenant_id", "category"])

    df["sales_lag_1"] = group["sales"].shift(1)
    df["sales_lag_2"] = group["sales"].shift(2)
    df["sales_lag_3"] = group["sales"].shift(3)
    df["sales_lag_4"] = group["sales"].shift(4)

    # Shift first so the current week's sales are not included
    # in the rolling average. This prevents target leakage.
    df["rolling_avg_4"] = group["sales"].transform(
        lambda x: x.shift(1).rolling(window=4).mean()
    )

    # Calculate week-over-week growth using the previous two weeks.
    # Replace zero denominator with NaN to avoid division by zero.
    df["growth_rate"] = (
        (df["sales_lag_1"] - df["sales_lag_2"])
        / df["sales_lag_2"].replace(0, float("nan"))
    )

    df["week_of_year"] = (
        df["week"].dt.isocalendar().week.astype(int)
    )

    # Remove rows without enough historical data to calculate
    # all required features.
    df = df.dropna().reset_index(drop=True)

    return df
