import pytest
import pandas as pd
import joblib
from app.preprocessing.preprocessor import preprocess_task
from app.core.config import settings
import copy

# Charger les préprocesseurs et les features attendues (comme dans app/endpoints.py)
encoder = joblib.load(settings.ENCODER_PATH)
tfidf = joblib.load(settings.TFIDF_PATH)
scaler = joblib.load(settings.SCALER_PATH)
all_tags = joblib.load("app/models/all_tags.pkl")
features = joblib.load("app/models/features.pkl")
model = joblib.load(settings.MODEL_PATH)
expected_features = model.feature_names_in_ if hasattr(model, 'feature_names_in_') else features

# Tâche valide pour le test (même que dans test_model_robustness.py)
valid_task = {
    "title": "Développer nouvelle API",
    "description": "Créer endpoint pour gestion des utilisateurs",
    "estimationTime": 600.0,
    "progress": 50.0,
    "projectId": 1,
    "creationDate": "2025-01-01",
    "startDate": "2025-01-02",
    "dueDate": "2025-01-08",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "itemtags": "backend,api",
    "assignedUserIds": "user1,user2"
}

def test_feature_consistency_valid_task():
    """Test que les colonnes de X_scaled correspondent à expected_features pour une tâche valide."""
    # Copier la tâche pour éviter de modifier l'original
    task = copy.deepcopy(valid_task)
    
    # Appeler preprocess_task
    X_scaled, is_short_task, task_type = preprocess_task(
        task, encoder, tfidf, scaler, all_tags, features, expected_features
    )
    
    # Vérifier que X_scaled est un DataFrame
    assert isinstance(X_scaled, pd.DataFrame), "X_scaled doit être un pandas DataFrame"
    
    # Vérifier que les colonnes de X_scaled correspondent exactement à expected_features
    assert list(X_scaled.columns) == list(expected_features), (
        f"Les colonnes de X_scaled ({X_scaled.columns.tolist()}) "
        f"ne correspondent pas à expected_features ({expected_features})"
    )
    
    print(f"Feature consistency verified: {len(X_scaled.columns)} features match expected_features")

def test_feature_consistency_empty_tags():
    """Test la cohérence des features avec itemtags vide."""
    task = copy.deepcopy(valid_task)
    task["itemtags"] = ""
    
    X_scaled, is_short_task, task_type = preprocess_task(
        task, encoder, tfidf, scaler, all_tags, features, expected_features
    )
    
    assert isinstance(X_scaled, pd.DataFrame), "X_scaled doit être un pandas DataFrame"
    assert list(X_scaled.columns) == list(expected_features), (
        f"Les colonnes de X_scaled ({X_scaled.columns.tolist()}) "
        f"ne correspondent pas à expected_features ({expected_features})"
    )
    
    print(f"Feature consistency verified with empty itemtags")

def test_feature_consistency_no_description():
    """Test la cohérence des features sans description."""
    task = copy.deepcopy(valid_task)
    task["description"] = ""
    
    X_scaled, is_short_task, task_type = preprocess_task(
        task, encoder, tfidf, scaler, all_tags, features, expected_features
    )
    
    assert isinstance(X_scaled, pd.DataFrame), "X_scaled doit être un pandas DataFrame"
    assert list(X_scaled.columns) == list(expected_features), (
        f"Les colonnes de X_scaled ({X_scaled.columns.tolist()}) "
        f"ne correspondent pas à expected_features ({expected_features})"
    )
    
    print(f"Feature consistency verified with empty description")