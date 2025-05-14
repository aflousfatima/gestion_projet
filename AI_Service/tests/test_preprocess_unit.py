import pytest
import pandas as pd
import numpy as np
from unittest.mock import patch
from app.preprocessing.preprocessor import (
    calculate_temporal_features,
    calculate_num_assigned_users,
    calculate_derived_features
)

# Tâche d'exemple pour les tests
sample_task = {
    "title": "Test Task",
    "description": "Test description",
    "estimationTime": 600.0,
    "progress": 50.0,
    "projectId": 1,
    "creationDate": "2025-01-01",
    "startDate": "2025-01-02",
    "dueDate": "2025-01-08",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "itemtags": "backend,urgent",
    "assignedUserIds": "user1,user2"
}

def test_calculate_temporal_features():
    """Teste le calcul des features temporelles."""
    # Créer un DataFrame mock
    task_df = pd.DataFrame([sample_task])
    
    # Appliquer la fonction
    result_df = calculate_temporal_features(task_df)
    
    # Vérifier les colonnes ajoutées
    assert 'days_to_start' in result_df.columns
    assert 'days_to_due' in result_df.columns
    assert 'days_to_complete' in result_df.columns
    assert 'planned_duration' in result_df.columns
    
    # Vérifier les valeurs
    assert result_df['days_to_start'].iloc[0] == 1  # 2025-01-02 - 2025-01-01
    assert result_df['days_to_due'].iloc[0] == 7   # 2025-01-08 - 2025-01-01
    assert result_df['days_to_complete'].iloc[0] == -1
    assert result_df['planned_duration'].iloc[0] == 6  # 7 - 1
    print("Temporal features calculated correctly")

def test_calculate_temporal_features_missing_dates():
    """Teste les features temporelles avec des dates manquantes."""
    task = sample_task.copy()
    task['startDate'] = None
    task['dueDate'] = None
    task_df = pd.DataFrame([task])
    
    result_df = calculate_temporal_features(task_df)
    
    assert result_df['days_to_start'].iloc[0] == 0
    assert result_df['days_to_due'].iloc[0] == 30
    assert result_df['planned_duration'].iloc[0] == 30
    print("Temporal features with missing dates handled correctly")

def test_calculate_num_assigned_users():
    """Teste le calcul du nombre d'utilisateurs assignés."""
    assert calculate_num_assigned_users("user1,user2") == 2
    assert calculate_num_assigned_users("") == 0
    assert calculate_num_assigned_users(None) == 0
    assert calculate_num_assigned_users("user1") == 1
    print("Number of assigned users calculated correctly")

def test_calculate_derived_features():
    """Teste le calcul des features dérivées."""
    task_df = pd.DataFrame([sample_task])
    
    # Appliquer la fonction
    result_df = calculate_derived_features(task_df)
    
    # Vérifier les colonnes ajoutées
    expected_columns = [
        'num_assigned_users', 'time_per_user', 'complexity_score',
        'is_urgent', 'low_progress', 'is_short_task'
    ]
    assert all(col in result_df.columns for col in expected_columns)
    
    # Vérifier les valeurs
    assert result_df['num_assigned_users'].iloc[0] == 2
    assert result_df['time_per_user'].iloc[0] == 300.0  # 600.0 / 2
    assert result_df['complexity_score'].iloc[0] == 2  # backend,urgent
    assert result_df['is_urgent'].iloc[0] == 1
    assert result_df['low_progress'].iloc[0] == 0  # progress = 50
    assert result_df['is_short_task'].iloc[0] == 0  # estimationTime = 600
    print("Derived features calculated correctly")

def test_calculate_derived_features_empty_itemtags():
    """Teste les features dérivées avec itemtags vide."""
    task = sample_task.copy()
    task['itemtags'] = ""
    task_df = pd.DataFrame([task])
    
    result_df = calculate_derived_features(task_df)
    
    assert result_df['complexity_score'].iloc[0] == 0
    assert result_df['is_urgent'].iloc[0] == 0
    print("Derived features with empty itemtags handled correctly")