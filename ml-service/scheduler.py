import os
import requests

from apscheduler.schedulers.blocking import BlockingScheduler
from dotenv import load_dotenv


from train import train_and_evaluate

load_dotenv()
scheduler = BlockingScheduler()
ML_SERVICE_TOKEN = os.environ["ML_SERVICE_TOKEN"]

@scheduler.scheduled_job("cron", hour=2)
def scheduled_training():
    print("Starting scheduled model training...")

    train_and_evaluate()

    print("Training completed. Reloading latest model...")

    response = requests.post(
        "http://localhost:8000/reload-model",
        headers={
            "X-ML-Service-Token": ML_SERVICE_TOKEN
        }
    )

    response.raise_for_status()

    print("Latest model loaded into ML service.")


if __name__ == "__main__":
    scheduler.start()