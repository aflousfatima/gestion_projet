import os
from dotenv import load_dotenv

print(f"Current working directory: {os.getcwd()}")  # Debug
print(f".env exists: {os.path.exists('.env')}")  # Debug
load_dotenv()
print(f"TASK_API_URL from env: {os.getenv('TASK_API_URL')}")  # Debu
LOG_LEVEL: str = os.getenv("LOG_LEVEL", "DEBUG")

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
    
    INTENTS_PATH: str = os.getenv("INTENTS_PATH")
    FAISS_INDEX_PATH: str = os.getenv("FAISS_INDEX_PATH")
    METADATA_PATH: str = os.getenv("METADATA_PATH")
    CACHE_PATH: str = os.getenv("CACHE_PATH")
    PROJECT_API_URL: str = os.getenv("PROJECT_API_URL", "http://localhost:8085/api/chatbot/projects")
    TASK_API_URL: str = os.getenv("TASK_API_URL", "http://localhost:8086/api/chatbot/tasks")  # Added
settings = Settings()