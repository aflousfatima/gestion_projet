import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    PROJECT_NAME: str = "Task Predictor Microservice"
    API_V1_STR: str = "/api/v1"
    MODEL_PATH: str = os.getenv("MODEL_PATH")
    SCALER_PATH: str = os.getenv("SCALER_PATH")
    ENCODER_PATH: str = os.getenv("ENCODER_PATH")
    TFIDF_PATH: str = os.getenv("TFIDF_PATH")
    TAGS_PATH: str = os.getenv("TAGS_PATH")
    FEATURES_PATH: str = os.getenv("FEATURES_PATH")
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

settings = Settings()