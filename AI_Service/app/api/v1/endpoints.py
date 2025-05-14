from fastapi import APIRouter, HTTPException
from app.api.v1.schemas import TaskInput, PredictionResponse
from app.models.predictor import TaskPredictor
from app.preprocessing.preprocessor import preprocess_task
from app.core.config import settings
import joblib

from pathlib import Path

router = APIRouter()

# Définir le répertoire de base (racine du projet)
BASE_DIR = Path(__file__).resolve().parent.parent.parent  # Remonte à C:\Gestion_Projet\AI_Service

# Charger les préprocesseurs et le modèle au démarrage
predictor = TaskPredictor(settings.MODEL_PATH)
encoder = joblib.load(settings.ENCODER_PATH)
tfidf = joblib.load(settings.TFIDF_PATH)
scaler = joblib.load(settings.SCALER_PATH)
all_tags = joblib.load("app/models/all_tags.pkl")  # Chemin explicite
#all_tags = joblib.load(settings.TAGS_PATH)
#features = joblib.load(settings.FEATURES_PATH)
features = joblib.load("app/models/features.pkl")  # Chemin explicite


# Charger les features attendues
model = joblib.load(settings.MODEL_PATH)
expected_features = model.feature_names_in_ if hasattr(model, 'feature_names_in_') else features

@router.post("/predict", response_model=PredictionResponse)
async def predict_task_duration(task: TaskInput):
    try:
        # Prétraiter la tâche
        X_task, is_short_task, task_type = preprocess_task(
            task.dict(), encoder, tfidf, scaler, all_tags, features, expected_features
        )
        
        # Faire la prédiction
        predicted_duration = predictor.predict(
            X_task, is_short_task, task_type, task.estimationTime
        )
        
        return PredictionResponse(
            task_title=task.title,
            predicted_duration=predicted_duration,
            expected_duration=task.estimationTime,
            model_name=predictor.model_name
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erreur lors de la prédiction : {str(e)}")