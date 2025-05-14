import pandas as pd
import numpy as np
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.feature_extraction.text import TfidfVectorizer
from typing import Tuple
import logging

# Configurer le logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def preprocess_task(task: dict, encoder: OneHotEncoder, tfidf: TfidfVectorizer, 
                   scaler: StandardScaler, all_tags: set, features: list, 
                   expected_features: list) -> Tuple[pd.DataFrame, int, str]:
    logger.info("Début du prétraitement de la tâche : %s", task['title'])
    
    task_df = pd.DataFrame([task])
    logger.info("Features initiales dans task_df : %s", task_df.columns.tolist())
    
    # Gérer les valeurs manquantes
    task_df['description'] = task_df['description'].fillna('')
    task_df['itemtags'] = task_df['itemtags'].fillna('')
    task_df['assignedUserIds'] = task_df['assignedUserIds'].fillna('')
    
    # Features temporelles
    task_df['creationDate'] = pd.to_datetime(task_df['creationDate'])
    task_df['startDate'] = pd.to_datetime(task_df['startDate'], errors='coerce')
    task_df['dueDate'] = pd.to_datetime(task_df['dueDate'], errors='coerce')
    task_df['days_to_start'] = (task_df['startDate'] - task_df['creationDate']).dt.days.fillna(0)
    task_df['days_to_due'] = (task_df['dueDate'] - task_df['creationDate']).dt.days.fillna(30)
    task_df['days_to_complete'] = -1
    task_df['planned_duration'] = (task_df['days_to_due'] - task_df['days_to_start']).clip(lower=1)
    task_df['description_length'] = task_df['description'].apply(len)
    
    # Nombre d'utilisateurs assignés
    task_df['num_assigned_users'] = task_df['assignedUserIds'].apply(
        lambda x: len(x.split(',')) if x and isinstance(x, str) else 0)
    
    # Nouvelles features
    task_df['time_per_user'] = task_df['estimationTime'] / task_df['num_assigned_users'].clip(lower=1)
    task_df['complexity_score'] = task_df['itemtags'].apply(
        lambda x: len(x.split(',')) if x and isinstance(x, str) else 0)
    task_df['is_urgent'] = task_df['itemtags'].apply(lambda x: 1 if 'urgent' in x else 0)
    task_df['low_progress'] = task_df['progress'].apply(lambda x: 1 if x < 50 else 0)
    task_df['is_short_task'] = task_df['estimationTime'].apply(lambda x: 1 if x < 200 else 0)
    task_df['time_progress_interaction'] = task_df['estimationTime'] * (task_df['progress'] / 100)
    task_df['log_estimationTime'] = np.log(task_df['estimationTime'] + 1)
    task_df['task_length_category'] = pd.cut(
        task_df['estimationTime'], bins=[0, 200, 500, 1500, float('inf')],
        labels=['short', 'medium', 'long', 'extra_long']).cat.codes
    task_df['progress_ratio'] = task_df['progress'] / task_df['estimationTime'].replace(0, 1)
    task_df['short_task_progress'] = task_df['is_short_task'] * task_df['progress']
    task_df['task_user_ratio'] = task_df['estimationTime'] / task_df['num_assigned_users'].clip(lower=1)
    task_df['task_type'] = task_df['itemtags'].apply(
        lambda x: 'bug' if 'bug' in x else 'feature' if 'api' in x or 'frontend' in x or 'backend' in x 
        else 'devops' if 'devops' in x else 'other')
    task_df['progress_task_type_ratio'] = task_df['progress_ratio'] * task_df['task_type'].map(
        {'bug': 1.2, 'devops': 1.5, 'feature': 1.0, 'other': 0.8})
    task_df['team_efficiency'] = task_df['progress'] / (
        task_df['num_assigned_users'].clip(lower=1) * task_df['estimationTime'].replace(0, 1) * 
        task_df['planned_duration'].replace(0, 1) * task_df['task_type'].map(
            {'bug': 1.0, 'devops': 1.2, 'feature': 0.8, 'other': 0.9}))
    task_df['long_task_adjustment'] = task_df['estimationTime'] / (task_df['progress'] + 1)
    task_df['long_task_progress_ratio'] = task_df['progress_ratio'] * (
        task_df['estimationTime'] > 1500).astype(int)
    task_df['extended_task_duration_factor'] = task_df.apply(
        lambda row: min(50, (1.5 + np.log1p(row['estimationTime'] / 1500)) * row['progress'] / 
        (max(row['num_assigned_users'], 1) + 1)) if row['estimationTime'] > 1500 else 1, axis=1)
    
    logger.info("Features après ajout des features dérivées : %s", task_df.columns.tolist())
    
    # Encodage catégorique
    cat_cols = ['status', 'priority', 'task_length_category', 'task_type']
    encoded_cats = encoder.transform(task_df[cat_cols])
    encoded_cats_df = pd.DataFrame(encoded_cats, columns=encoder.get_feature_names_out(cat_cols))
    logger.info("Features catégoriques encodées : %s", encoded_cats_df.columns.tolist())
    
    # Traitement des tags
    task_df['itemtags'] = task_df['itemtags'].apply(
        lambda x: x.split(',') if x and isinstance(x, str) else [])
    tag_cols = []
    for tag in all_tags:
        col_name = f'tag_{tag}'
        task_df[col_name] = task_df['itemtags'].apply(lambda x: 1 if tag in x else 0)
        tag_cols.append(col_name)
    logger.info("Features des tags : %s", tag_cols)
    
    # TF-IDF pour description
    tfidf_matrix = tfidf.transform(task_df['description']).toarray()
    tfidf_cols = [f'tfidf_{i}' for i in range(tfidf_matrix.shape[1])]
    tfidf_df = pd.DataFrame(tfidf_matrix, columns=tfidf_cols)
    logger.info("Features TF-IDF : %s", tfidf_cols)
    
    # Combiner les features
    X = pd.concat([
        task_df[features],
        encoded_cats_df,
        task_df[tag_cols],
        tfidf_df
    ], axis=1)
    logger.info("Features combinées avant alignement : %s", X.columns.tolist())
    
    # Aligner avec les features attendues
    missing_cols = [col for col in expected_features if col not in X.columns]
    extra_cols = [col for col in X.columns if col not in expected_features]
    logger.info("Colonnes manquantes : %s", missing_cols)
    logger.info("Colonnes supplémentaires : %s", extra_cols)
    
    # Ajouter les colonnes manquantes avec des zéros
    for col in missing_cols:
        X[col] = 0
    
    # Supprimer les colonnes supplémentaires
    X = X.drop(columns=extra_cols, errors='ignore')
    
    # Réordonner les colonnes pour correspondre à expected_features
    X = X[expected_features]
    logger.info("Features après alignement : %s", X.columns.tolist())
    
    # Normalisation
    X_scaled = pd.DataFrame(scaler.transform(X), columns=X.columns)
    logger.info("Features finales (X_scaled) : %s", X_scaled.columns.tolist())
    
    return X_scaled, task_df['is_short_task'].iloc[0], task_df['task_type'].iloc[0]