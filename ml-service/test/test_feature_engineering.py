
from feature_engineering import create_features


def test_create_features_generates_lag_features(historical_sales):
    result = create_features(historical_sales)
    row = result.iloc[-1]

    assert row["sales_lag_1"] == 500
    assert row["sales_lag_2"] == 400
    assert row["sales_lag_3"] == 300
    assert row["sales_lag_4"] == 200


def test_rolling_average_excludes_current_week(historical_sales):
    result = create_features(historical_sales)
    row = result.iloc[-1]

    # Previous four weeks: 200, 300, 400, 500
    assert row["rolling_avg_4"] == 350


def test_growth_rate(historical_sales):
    result = create_features(historical_sales)
    row = result.iloc[-1]

    # (500 - 400) / 400
    assert abs(row["growth_rate"] - 0.25) < 0.000001


def test_week_of_year_is_created(historical_sales):
    result = create_features(historical_sales)

    assert "week_of_year" in result.columns
    assert result["week_of_year"].notna().all()


def test_features_are_independent_per_tenant_and_category():
    sales = [
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-05", "sales": 100},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-12", "sales": 200},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-19", "sales": 300},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-26", "sales": 400},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-02-02", "sales": 500},

        {"tenant_id": 2, "category": "Electronics", "week": "2026-01-05", "sales": 1000},
        {"tenant_id": 2, "category": "Electronics", "week": "2026-01-12", "sales": 1100},
        {"tenant_id": 2, "category": "Electronics", "week": "2026-01-19", "sales": 1200},
        {"tenant_id": 2, "category": "Electronics", "week": "2026-01-26", "sales": 1300},
        {"tenant_id": 2, "category": "Electronics", "week": "2026-02-02", "sales": 1400},
    ]

    result = create_features(sales)

    tenant1 = result[result["tenant_id"] == 1].iloc[-1]
    tenant2 = result[result["tenant_id"] == 2].iloc[-1]

    assert tenant1["sales_lag_1"] == 400
    assert tenant2["sales_lag_1"] == 1300


def test_zero_denominator_does_not_create_infinity():
    sales = [
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-05", "sales": 100},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-12", "sales": 0},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-19", "sales": 100},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-01-26", "sales": 200},
        {"tenant_id": 1, "category": "Electronics", "week": "2026-02-02", "sales": 300},
    ]

    result = create_features(sales)

    assert not result["growth_rate"].isin([float("inf"), float("-inf")]).any()
