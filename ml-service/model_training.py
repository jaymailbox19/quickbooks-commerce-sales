from xgboost import XGBRegressor


def train_model(X, y):
    model = XGBRegressor(
        n_estimators=100,
        max_depth=3,
        learning_rate=0.1,
        objective="reg:squarederror",
        random_state=42
    )

    model.fit(X, y)

    return model


def save_model(model, path):
    model.save_model(path)