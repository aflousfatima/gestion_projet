import os
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, KFold
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import ExtraTreesRegressor, RandomForestRegressor
from lightgbm import LGBMRegressor
from catboost import CatBoostRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import optuna
import matplotlib.pyplot as plt
import seaborn as sns
import joblib
import mlflow
import mlflow.sklearn
import logging
from dotenv import load_dotenv
import warnings
warnings.filterwarnings('ignore')

# Configurer le logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Charger les variables d'environnement
load_dotenv()
DATA_PATH = os.getenv("DATA_PATH", "app/data/tasks_dataset.csv")
MODEL_PATH = os.getenv("MODEL_PATH", "app/models/best_model.pkl")
SCALER_PATH = os.getenv("SCALER_PATH", "app/models/scaler.pkl")
ENCODER_PATH = os.getenv("ENCODER_PATH", "app/models/encoder.pkl")
TFIDF_PATH = os.getenv("TFIDF_PATH", "app/models/tfidf.pkl")
TAGS_PATH = os.getenv("TAGS_PATH", "app/models/all_tags.pkl")
FEATURES_PATH = os.getenv("FEATURES_PATH", "app/models/features.pkl")
PLOTS_DIR = os.getenv("PLOTS_DIR", "app/plots/")

# Fonction pour MMRE
def mmre(y_true, y_pred):
    y_true = np.maximum(y_true, 1)
    y_pred = np.maximum(y_pred, 0)
    relative_errors = np.abs((y_true - y_pred) / y_true)
    return np.mean(relative_errors[np.isfinite(relative_errors)])

# Fonction d'évaluation avec validation croisée
def evaluate_with_cv(model, X, y, cv=3):
    kf = KFold(n_splits=cv, shuffle=True, random_state=42)
    mae_scores, rmse_scores, r2_scores, mmre_scores = [], [], [], []
    
    for train_idx, test_idx in kf.split(X):
        X_train_cv, X_test_cv = X.iloc[train_idx], X.iloc[test_idx]
        y_train_cv, y_test_cv = y.iloc[train_idx], y.iloc[test_idx]
        
        model.fit(X_train_cv, y_train_cv)
        y_pred_cv = np.maximum(model.predict(X_test_cv), 0)
        
        mae_scores.append(mean_absolute_error(y_test_cv, y_pred_cv))
        rmse_scores.append(np.sqrt(mean_squared_error(y_test_cv, y_pred_cv)))
        r2_scores.append(r2_score(y_test_cv, y_pred_cv))
        mmre_scores.append(mmre(y_test_cv, y_pred_cv))
    
    return {
        'MAE': np.mean(mae_scores),
        'RMSE': np.mean(rmse_scores),
        'R²': np.mean(r2_scores),
        'MMRE': np.mean(mmre_scores),
        'MMRE_STD': np.std(mmre_scores)
    }

# Fonction d'optimisation des hyperparamètres pour LightGBM
def optimize_lightgbm(X_train, y_train, X_val, y_val):
    def objective(trial):
        param = {
            'n_estimators': trial.suggest_int('n_estimators', 200, 600),
            'num_leaves': trial.suggest_int('num_leaves', 20, 50),
            'learning_rate': trial.suggest_float('learning_rate', 0.005, 0.01),
            'min_child_samples': trial.suggest_int('min_child_samples', 10, 50),
            'random_state': 42
        }
        model = LGBMRegressor(**param)
        model.fit(X_train, y_train)
        y_pred = np.maximum(model.predict(X_val), 0)
        return mmre(y_val, y_pred)
    
    study = optuna.create_study(direction='minimize')
    study.optimize(objective, n_trials=20)
    return study.best_params

# Fonction pour prétraiter les données
def preprocess_data(df):
    logger.info("Prétraitement des données...")
    # Gérer les valeurs manquantes
    df['description'].fillna('', inplace=True)
    df['completedDate'].fillna('', inplace=True)
    df['itemtags'].fillna('', inplace=True)
    df['assignedUserIds'].fillna('', inplace=True)

    # Convertir les dates
    df['creationDate'] = pd.to_datetime(df['creationDate'])
    for col in ['startDate', 'dueDate', 'completedDate']:
        df[col] = pd.to_datetime(df[col], errors='coerce')
    df['days_to_start'] = (df['startDate'] - df['creationDate']).dt.days.fillna(0)
    df['days_to_due'] = (df['dueDate'] - df['creationDate']).dt.days.fillna(30)
    df['days_to_complete'] = (df['completedDate'] - df['creationDate']).dt.days.fillna(-1)

    # Features temporelles et autres
    df['planned_duration'] = (df['days_to_due'] - df['days_to_start']).clip(lower=1)
    df['description_length'] = df['description'].apply(len)
    df['num_assigned_users'] = df['assignedUserIds'].apply(lambda x: len(x.split(',')) if x and isinstance(x, str) else 0)
    df['is_short_task'] = df['estimationTime'].apply(lambda x: 1 if x < 200 else 0)
    df['time_per_user'] = df['estimationTime'] / df['num_assigned_users'].clip(lower=1)
    df['complexity_score'] = df['itemtags'].apply(lambda x: len(x.split(',')) if x and isinstance(x, str) else 0)
    df['is_urgent'] = df['itemtags'].apply(lambda x: 1 if 'urgent' in x else 0)
    df['low_progress'] = df['progress'].apply(lambda x: 1 if x < 50 else 0)
    df['time_progress_interaction'] = df['estimationTime'] * (df['progress'] / 100)
    df['log_estimationTime'] = np.log(df['estimationTime'] + 1)
    df['task_length_category'] = pd.cut(df['estimationTime'], bins=[0, 200, 500, 1500, float('inf')], labels=['short', 'medium', 'long', 'extra_long']).cat.codes
    df['progress_ratio'] = df['progress'] / df['estimationTime'].replace(0, 1)
    df['short_task_progress'] = df['is_short_task'] * df['progress']
    df['task_user_ratio'] = df['estimationTime'] / df['num_assigned_users'].clip(lower=1)
    df['task_type'] = df['itemtags'].apply(lambda x: 'bug' if 'bug' in x else 'feature' if 'api' in x or 'frontend' in x or 'backend' in x else 'devops' if 'devops' in x else 'other')
    df['progress_task_type_ratio'] = df['progress_ratio'] * df['task_type'].map({'bug': 1.2, 'devops': 1.5, 'feature': 1.0, 'other': 0.8})
    df['team_efficiency'] = df['progress'] / (df['num_assigned_users'].clip(lower=1) * df['estimationTime'].replace(0, 1) * df['planned_duration'].replace(0, 1) * df['task_type'].map({'bug': 1.0, 'devops': 1.2, 'feature': 0.8, 'other': 0.9}))
    df['long_task_adjustment'] = df['estimationTime'] / (df['progress'] + 1)
    df['long_task_progress_ratio'] = df['progress_ratio'] * (df['estimationTime'] > 1500).astype(int)
    df['extended_task_duration_factor'] = df.apply(lambda row: min(50, (1.5 + np.log1p(row['estimationTime'] / 1500)) * row['progress'] / (max(row['num_assigned_users'], 1) + 1)) if row['estimationTime'] > 1500 else 1, axis=1)

    # Ajuster totalTimeSpent pour IN_PROGRESS
    df['adjusted_totalTimeSpent'] = df.apply(
        lambda row: min(
            row['totalTimeSpent'] / (row['progress'] / 100) * (1.07 if row['is_short_task'] == 1 else 1.0),
            row['estimationTime'] * 2
        ) if row['status'] == 'IN_PROGRESS' else row['totalTimeSpent'],
        axis=1
    )
    df.loc[df['estimationTime'] > 1500, 'adjusted_totalTimeSpent'] = df.loc[df['estimationTime'] > 1500, 'adjusted_totalTimeSpent'] * 0.92
    df = df[df['adjusted_totalTimeSpent'] <= df['estimationTime'] * 2.0].reset_index(drop=True)

    # Clipper les valeurs extrêmes
    for task_type in ['feature', 'devops']:
        mean_val = df.loc[df['task_type'] == task_type, 'adjusted_totalTimeSpent'].mean()
        std_val = df.loc[df['task_type'] == task_type, 'adjusted_totalTimeSpent'].std()
        upper_bound = mean_val + 2.5 * std_val
        df.loc[(df['task_type'] == task_type) & (df['adjusted_totalTimeSpent'] > upper_bound), 'adjusted_totalTimeSpent'] = upper_bound
    df.loc[df['task_type'] == 'feature', 'adjusted_totalTimeSpent'] = df.loc[df['task_type'] == 'feature', 'adjusted_totalTimeSpent'].clip(upper=df.loc[df['task_type'] == 'feature', 'adjusted_totalTimeSpent'].quantile(0.95))
    df['adjusted_totalTimeSpent'] = df['adjusted_totalTimeSpent'].clip(upper=df['adjusted_totalTimeSpent'].quantile(0.98))

    # Poids pour l'entraînement
    sample_weights = df.apply(
        lambda row: 7.0 if row['is_short_task'] == 1 else 5.2 if row['estimationTime'] > 1500 else 3.5 if row['task_type'] == 'feature' else 3.0 if row['task_type'] == 'devops' else 1.0,
        axis=1
    ).values

    return df, sample_weights

# Fonction pour préparer les features
def prepare_features(df):
    logger.info("Préparation des features...")
    # Encodage catégorique
    encoder = OneHotEncoder(sparse_output=False, handle_unknown='ignore')
    cat_cols = ['status', 'priority', 'task_length_category', 'task_type']
    encoded_cats = encoder.fit_transform(df[cat_cols])
    encoded_cats_df = pd.DataFrame(encoded_cats, columns=encoder.get_feature_names_out(cat_cols), index=df.index)

    # Traitement des tags
    df['itemtags'] = df['itemtags'].apply(lambda x: x.split(',') if x and isinstance(x, str) else [])
    all_tags = set(tag for tags in df['itemtags'] for tag in tags if tag)
    for tag in all_tags:
        df[f'tag_{tag}'] = df['itemtags'].apply(lambda x: 1 if tag in x else 0)

    # TF-IDF pour description
    tfidf = TfidfVectorizer(max_features=50, stop_words='english')
    tfidf_matrix = tfidf.fit_transform(df['description']).toarray()
    tfidf_df = pd.DataFrame(tfidf_matrix, columns=[f'tfidf_{i}' for i in range(tfidf_matrix.shape[1])], index=df.index)

    # Features
    features = [
        'estimationTime', 'progress', 'projectId', 
        'days_to_start', 'days_to_due', 'days_to_complete', 
        'num_assigned_users', 'planned_duration', 'description_length',
        'time_per_user', 'complexity_score', 'is_urgent', 'low_progress',
        'is_short_task', 'time_progress_interaction', 'log_estimationTime',
        'progress_ratio', 'short_task_progress', 'task_user_ratio',
        'progress_task_type_ratio', 'team_efficiency', 'long_task_adjustment',
        'long_task_progress_ratio', 'extended_task_duration_factor'
    ]
    X = pd.concat([df[features], encoded_cats_df, df[[f'tag_{tag}' for tag in all_tags]], tfidf_df], axis=1)
    y = df['adjusted_totalTimeSpent'].clip(lower=0)

    # Normalisation
    scaler = StandardScaler()
    X_scaled = pd.DataFrame(scaler.fit_transform(X), columns=X.columns)

    return X_scaled, y, encoder, tfidf, scaler, all_tags, features

# Fonction pour prétraiter une nouvelle tâche
def preprocess_task(task, encoder, tfidf, scaler, all_tags, features):
    task_df = pd.DataFrame([task])
    
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
    task_df['num_assigned_users'] = task_df['assignedUserIds'].apply(lambda x: len(x.split(',')) if x and isinstance(x, str) else 0)
    
    # Nouvelles features
    task_df['time_per_user'] = task_df['estimationTime'] / task_df['num_assigned_users'].clip(lower=1)
    task_df['complexity_score'] = task_df['itemtags'].apply(lambda x: len(x.split(',')) if x and isinstance(x, str) else 0)
    task_df['is_urgent'] = task_df['itemtags'].apply(lambda x: 1 if 'urgent' in x else 0)
    task_df['low_progress'] = task_df['progress'].apply(lambda x: 1 if x < 50 else 0)
    task_df['is_short_task'] = task_df['estimationTime'].apply(lambda x: 1 if x < 200 else 0)
    task_df['time_progress_interaction'] = task_df['estimationTime'] * (task_df['progress'] / 100)
    task_df['log_estimationTime'] = np.log(task_df['estimationTime'] + 1)
    task_df['task_length_category'] = pd.cut(task_df['estimationTime'], bins=[0, 200, 500, 1500, float('inf')], labels=['short', 'medium', 'long', 'extra_long']).cat.codes
    task_df['progress_ratio'] = task_df['progress'] / task_df['estimationTime'].replace(0, 1)
    task_df['short_task_progress'] = task_df['is_short_task'] * task_df['progress']
    task_df['task_user_ratio'] = task_df['estimationTime'] / task_df['num_assigned_users'].clip(lower=1)
    task_df['task_type'] = task_df['itemtags'].apply(lambda x: 'bug' if 'bug' in x else 'feature' if 'api' in x or 'frontend' in x or 'backend' in x else 'devops' if 'devops' in x else 'other')
    task_df['progress_task_type_ratio'] = task_df['progress_ratio'] * task_df['task_type'].map({'bug': 1.2, 'devops': 1.5, 'feature': 1.0, 'other': 0.8})
    task_df['team_efficiency'] = task_df['progress'] / (task_df['num_assigned_users'].clip(lower=1) * task_df['estimationTime'].replace(0, 1) * task_df['planned_duration'].replace(0, 1) * task_df['task_type'].map({'bug': 1.0, 'devops': 1.2, 'feature': 0.8, 'other': 0.9}))
    task_df['long_task_adjustment'] = task_df['estimationTime'] / (task_df['progress'] + 1)
    task_df['long_task_progress_ratio'] = task_df['progress_ratio'] * (task_df['estimationTime'] > 1500).astype(int)
    task_df['extended_task_duration_factor'] = task_df.apply(lambda row: min(50, (1.5 + np.log1p(row['estimationTime'] / 1500)) * row['progress'] / (max(row['num_assigned_users'], 1) + 1)) if row['estimationTime'] > 1500 else 1, axis=1)
    
    # Encodage catégorique
    cat_cols = ['status', 'priority', 'task_length_category', 'task_type']
    encoded_cats = encoder.transform(task_df[cat_cols])
    encoded_cats_df = pd.DataFrame(encoded_cats, columns=encoder.get_feature_names_out(cat_cols))
    
    # Traitement des tags
    task_df['itemtags'] = task_df['itemtags'].apply(lambda x: x.split(',') if x and isinstance(x, str) else [])
    for tag in all_tags:
        task_df[f'tag_{tag}'] = task_df['itemtags'].apply(lambda x: 1 if tag in x else 0)
    
    # TF-IDF pour description
    tfidf_matrix = tfidf.transform(task_df['description']).toarray()
    tfidf_df = pd.DataFrame(tfidf_matrix, columns=[f'tfidf_{i}' for i in range(tfidf_matrix.shape[1])])
    
    # Combiner les features
    X = pd.concat([
        task_df[features],
        encoded_cats_df,
        task_df[[f'tag_{tag}' for tag in all_tags if f'tag_{tag}' in task_df.columns]],
        tfidf_df
    ], axis=1)
    
    # Normalisation
    X_scaled = pd.DataFrame(scaler.transform(X), columns=X.columns)
    return X_scaled, task_df['is_short_task'].iloc[0], task_df['task_type'].iloc[0]

# Vérifier les performances précédentes
def check_previous_performance():
    try:
        previous_runs = mlflow.search_runs(filter_string="tags.mlflow.runName = 'predictor_training'")
        if not previous_runs.empty:
            best_run = previous_runs.loc[previous_runs['metrics.MMRE'].idxmin()]
            return best_run['metrics.MMRE']
        return float('inf')
    except Exception as e:
        logger.error(f"Erreur lors de la vérification des performances précédentes : {e}")
        return float('inf')

# Fonction principale
def main():
    with mlflow.start_run(run_name="predictor_training"):
        logger.info("Démarrage de l'entraînement du modèle de prédiction...")

        # Charger les données
        df = pd.read_csv(DATA_PATH)
        df = df[df['totalTimeSpent'] > 0].reset_index(drop=True)
        logger.info(f"Données chargées : {df.shape}")

        # Exploration des données
        logger.info("Exploration des données...")
        logger.info(f"Statistiques descriptives pour totalTimeSpent:\n{df['totalTimeSpent'].describe()}")
        logger.info(f"Nombre de valeurs manquantes par colonne:\n{df.isnull().sum()}")
        logger.info(f"Colonnes du DataFrame:\n{df.columns.tolist()}")
        logger.info(f"Distribution de progress pour IN_PROGRESS:\n{df[df['status'] == 'IN_PROGRESS']['progress'].describe()}")
        logger.info(f"Nombre de tâches avec estimationTime > 1500 min:\n{df[df['estimationTime'] > 1500].shape}")

        # Prétraiter les données
        df, sample_weights = preprocess_data(df)
        logger.info(f"Vérification des colonnes créées :\n{df.columns.tolist()}")
        logger.info(f"Proportion de is_short_task:\n{df['is_short_task'].value_counts(normalize=True)}")
        logger.info(f"Distribution de task_length_category:\n{df['task_length_category'].value_counts()}")
        logger.info(f"Statistiques pour tâches > 1500 min:\n{df[df['estimationTime'] > 1500][['totalTimeSpent', 'adjusted_totalTimeSpent', 'progress', 'estimationTime', 'extended_task_duration_factor']].describe()}")

        # Préparer les features
        X_scaled, y, encoder, tfidf, scaler, all_tags, features = prepare_features(df)

        # Diviser en train/test
        X_train, X_test, y_train, y_test = train_test_split(X_scaled, y, test_size=0.2, random_state=42)
        train_weights = sample_weights[X_train.index]
        logger.info(f"Taille entraînement : {X_train.shape[0]}, Test : {X_test.shape[0]}")

        # Optimiser les hyperparamètres pour LightGBM
        logger.info("Optimisation des hyperparamètres pour LightGBM...")
        best_params = optimize_lightgbm(X_train, y_train, X_test, y_test)
        mlflow.log_params(best_params)
        logger.info(f"Meilleurs paramètres pour LightGBM : {best_params}")

        # Modèles
        models = {
            'Extra Trees': ExtraTreesRegressor(n_estimators=400, max_depth=20, min_samples_split=4, min_samples_leaf=2, random_state=42),
            'Random Forest': RandomForestRegressor(n_estimators=300, max_depth=20, min_samples_split=4, min_samples_leaf=2, random_state=42),
            'LightGBM': LGBMRegressor(**best_params),
            'CatBoost': CatBoostRegressor(n_estimators=850, depth=6, learning_rate=0.009, verbose=0, random_state=42)
        }

        # Entraînement et évaluation
        logger.info("Phase d'entraînement et évaluation...")
        results = []
        for name, model in models.items():
            logger.info(f"Entraînement et validation croisée de {name}...")
            model.fit(X_train, y_train, **({'sample_weight': train_weights} if name in ['Extra Trees', 'Random Forest'] else {}))
            
            cv_results = evaluate_with_cv(model, X_scaled, y)
            
            y_pred = np.maximum(model.predict(X_test), 0)
            
            mae = mean_absolute_error(y_test, y_pred)
            rmse = np.sqrt(mean_squared_error(y_test, y_pred))
            r2 = r2_score(y_test, y_pred)
            mmre_score = mmre(y_test, y_pred)
            
            results.append({
                'Model': name,
                'MAE': mae,
                'RMSE': rmse,
                'R²': r2,
                'MMRE': mmre_score,
                'CV_MMRE': cv_results['MMRE'],
                'CV_MMRE_STD': cv_results['MMRE_STD']
            })
            mlflow.log_metric(f"{name}_MAE", mae)
            mlflow.log_metric(f"{name}_RMSE", rmse)
            mlflow.log_metric(f"{name}_R2", r2)
            mlflow.log_metric(f"{name}_MMRE", mmre_score)
            mlflow.log_metric(f"{name}_CV_MMRE", cv_results['MMRE'])

        # Comparaison des modèles
        results_df = pd.DataFrame(results)
        logger.info("Comparaison des modèles :\n" + results_df.sort_values(by='MMRE').to_string(index=False))
        mlflow.log_artifact(pd.DataFrame.to_csv(results_df, 'results.csv'))

        # Visualisations
        os.makedirs(PLOTS_DIR, exist_ok=True)
        plt.figure(figsize=(12, 6))
        metrics = ['MAE', 'RMSE', 'MMRE', 'R²']
        for i, metric in enumerate(metrics, 1):
            plt.subplot(2, 2, i)
            sns.barplot(x='Model', y=metric, data=results_df)
            plt.title(f'Comparaison par {metric}')
            plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(os.path.join(PLOTS_DIR, 'model_metrics_comparison.png'))
        mlflow.log_artifact(os.path.join(PLOTS_DIR, 'model_metrics_comparison.png'))
        plt.close()

        # Sélection du meilleur modèle
        best_model_name = results_df.sort_values('MMRE').iloc[0]['Model']
        best_model = models[best_model_name]
        y_pred_best = np.maximum(best_model.predict(X_test), 0)

        # Erreur relative par task_type
        test_df = df.iloc[X_test.index].reset_index()
        error_by_type = pd.DataFrame({
            'Task Type': test_df['task_type'],
            'Relative Error': np.abs((y_test - y_pred_best) / np.maximum(y_test, 1))
        })
        plt.figure(figsize=(10, 5))
        sns.boxplot(x='Task Type', y='Relative Error', data=error_by_type)
        plt.title('Erreur relative par type de tâche')
        plt.savefig(os.path.join(PLOTS_DIR, 'error_by_task_type.png'))
        mlflow.log_artifact(os.path.join(PLOTS_DIR, 'error_by_task_type.png'))
        plt.close()

        # Distribution des erreurs
        error_df = pd.DataFrame({
            'True': y_test,
            'Predicted': y_pred_best,
            'Error': y_test - y_pred_best,
            'RelativeError': np.abs((y_test - y_pred_best) / np.maximum(y_test, 1))
        })
        plt.figure(figsize=(10, 5))
        sns.histplot(error_df['RelativeError'], bins=50, kde=True)
        plt.title('Distribution des erreurs relatives')
        plt.xlabel('Erreur relative')
        plt.savefig(os.path.join(PLOTS_DIR, 'error_distribution.png'))
        mlflow.log_artifact(os.path.join(PLOTS_DIR, 'error_distribution.png'))
        plt.close()

        # Importance des features
        if best_model_name in ['Extra Trees', 'Random Forest', 'LightGBM', 'CatBoost']:
            feature_importance = pd.DataFrame({
                'Feature': X_scaled.columns,
                'Importance': best_model.feature_importances_ if best_model_name in ['Extra Trees', 'Random Forest', 'LightGBM'] else best_model.get_feature_importance()
            }).sort_values(by='Importance', ascending=False).head(10)
            plt.figure(figsize=(10, 5))
            sns.barplot(x='Importance', y='Feature', data=feature_importance)
            plt.title(f'Importance des features ({best_model_name})')
            plt.savefig(os.path.join(PLOTS_DIR, 'feature_importance.png'))
            mlflow.log_artifact(os.path.join(PLOTS_DIR, 'feature_importance.png'))
            plt.close()
            logger.info(f"Importance des features ({best_model_name}):\n" + feature_importance.to_string(index=False))
            mlflow.log_artifact(pd.DataFrame.to_csv(feature_importance, 'feature_importance.csv'))

        # Comparaison des modèles par MMRE
        plt.figure(figsize=(12, 6))
        sns.barplot(x='MMRE', y='Model', data=results_df.sort_values('MMRE'))
        plt.title('Comparaison des modèles par MMRE')
        plt.xlabel('MMRE (plus bas = meilleur)')
        plt.ylabel('Modèle')
        plt.tight_layout()
        plt.savefig(os.path.join(PLOTS_DIR, 'model_comparison_mmre.png'))
        mlflow.log_artifact(os.path.join(PLOTS_DIR, 'model_comparison_mmre.png'))
        plt.close()

        # Analyse des erreurs
        logger.info(f"Analyse des erreurs ({best_model_name})...")
        logger.info(f"Erreur moyenne absolue: {error_df['Error'].abs().mean():.2f}")
        logger.info(f"Erreur relative moyenne: {error_df['RelativeError'].mean():.2f}")
        logger.info(f"Pourcentage d'erreurs > 50%: {(error_df['RelativeError'] > 0.5).mean() * 100:.2f}%")
        plt.figure(figsize=(10, 6))
        plt.scatter(error_df['True'], error_df['Predicted'], alpha=0.5)
        plt.plot([0, max(y_test)], [0, max(y_test)], 'r--')
        plt.xlabel('Temps réel (minutes)')
        plt.ylabel('Temps prédit (minutes)')
        plt.title(f'Comparaison des temps réels vs prédits ({best_model_name})')
        plt.savefig(os.path.join(PLOTS_DIR, 'prediction_vs_actual.png'))
        mlflow.log_artifact(os.path.join(PLOTS_DIR, 'prediction_vs_actual.png'))
        plt.close()

        # Comparer avec les performances précédentes
        previous_mmre = check_previous_performance()
        mmre_score = results_df[results_df['Model'] == best_model_name]['MMRE'].iloc[0]
        if mmre_score > previous_mmre:
            logger.error(f"Nouveau modèle moins performant : MMRE={mmre_score:.2f} > {previous_mmre:.2f}")
            raise Exception("Nouveau modèle rejeté : MMRE supérieur à la précédente run")

        # Sauvegarder les artefacts
        os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
        joblib.dump(best_model, MODEL_PATH)
        joblib.dump(scaler, SCALER_PATH)
        joblib.dump(encoder, ENCODER_PATH)
        joblib.dump(tfidf, TFIDF_PATH)
        joblib.dump(all_tags, TAGS_PATH)
        joblib.dump(features, FEATURES_PATH)
        mlflow.log_artifact(MODEL_PATH)
        mlflow.log_artifact(SCALER_PATH)
        mlflow.log_artifact(ENCODER_PATH)
        mlflow.log_artifact(TFIDF_PATH)
        mlflow.log_artifact(TAGS_PATH)
        mlflow.log_artifact(FEATURES_PATH)
        logger.info("Modèle et préprocesseurs sauvegardés.")

        # Prédictions sur tâches fictives
        logger.info("Prédictions sur 3 tâches fictives...")
        fictitious_tasks = [
            {
                'title': 'Développer nouvelle API',
                'description': 'Créer endpoint pour gestion des utilisateurs',
                'estimationTime': 600,
                'progress': 50.0,
                'projectId': 1,
                'creationDate': '2025-01-01',
                'startDate': '2025-01-02',
                'dueDate': '2025-01-08',
                'num_assigned_users': 2,
                'status': 'IN_PROGRESS',
                'priority': 'HIGH',
                'itemtags': 'backend,api',
                'assignedUserIds': 'user1,user2'
            },
            {
                'title': 'Corriger bug interface',
                'description': 'Fixer problème d\'affichage sur mobile',
                'estimationTime': 120,
                'progress': 50.0,
                'projectId': 2,
                'creationDate': '2025-01-01',
                'startDate': '2025-01-01',
                'dueDate': '2025-01-04',
                'num_assigned_users': 1,
                'status': 'IN_PROGRESS',
                'priority': 'MEDIUM',
                'itemtags': 'frontend,bug',
                'assignedUserIds': 'user3'
            },
            {
                'title': 'Refactoriser système complet',
                'description': 'Refonte complète du backend et tests',
                'estimationTime': 2000,
                'progress': 30.0,
                'projectId': 3,
                'creationDate': '2025-01-01',
                'startDate': '2025-01-03',
                'dueDate': '2025-02-01',
                'num_assigned_users': 3,
                'status': 'IN_PROGRESS',
                'priority': 'CRITICAL',
                'itemtags': 'backend,refactor',
                'assignedUserIds': 'user2,user4,user5'
            }
        ]

        predictions = []
        for task in fictitious_tasks:
            X_task, is_short_task, task_type = preprocess_task(task, encoder, tfidf, scaler, all_tags, features)
            for name, model in models.items():
                pred = np.maximum(model.predict(X_task)[0], 0)
                if is_short_task:
                    pred *= 1.07
                elif task['estimationTime'] > 1500:
                    pred *= 0.82
                elif task['estimationTime'] > 900:
                    pred *= 0.95
                elif task['estimationTime'] > 500:
                    pred *= 0.92
                if task_type == 'devops':
                    pred *= 0.96
                elif task_type == 'feature':
                    pred *= 0.959
                predictions.append({
                    'Model': name,
                    'Task': task['title'],
                    'Prediction (min)': pred,
                    'Expected (min)': task['estimationTime']
                })

        predictions_df = pd.DataFrame(predictions)
        logger.info("Prédictions pour les tâches fictives :\n" + predictions_df.pivot(index='Model', columns='Task', values='Prediction (min)').join(
            predictions_df.groupby('Model')['Expected (min)'].first().rename('Expected (min)')).to_string())
        mlflow.log_artifact(pd.DataFrame.to_csv(predictions_df, 'predictions.csv'))

        # Moyenne des deux meilleurs modèles
        best_models = results_df.sort_values('MMRE').iloc[:2]['Model'].tolist()
        best_predictions = predictions_df[predictions_df['Model'].isin(best_models)]
        avg_predictions = best_predictions.groupby('Task')['Prediction (min)'].mean()
        logger.info("Prédiction moyenne des deux meilleurs modèles :")
        for task_title, pred in avg_predictions.items():
            expected = next(task['estimationTime'] for task in fictitious_tasks if task['title'] == task_title)
            logger.info(f"{task_title}: {pred:.1f} min (Estimation : {expected} min)")

        # Validation par task_type et durée
        logger.info("Validation par task_type (bugs) :\n" + df[df['task_type'] == 'bug'][['totalTimeSpent', 'adjusted_totalTimeSpent', 'progress', 'estimationTime']].describe().to_string())
        logger.info("Validation par task_type (feature) :\n" + df[df['task_type'] == 'feature'][['totalTimeSpent', 'adjusted_totalTimeSpent', 'progress', 'estimationTime']].describe().to_string())
        logger.info("Validation par task_type (devops) :\n" + df[df['task_type'] == 'devops'][['totalTimeSpent', 'adjusted_totalTimeSpent', 'progress', 'estimationTime']].describe().to_string())
        logger.info("Validation pour tâches > 1500 min :\n" + df[df['estimationTime'] > 1500][['totalTimeSpent', 'adjusted_totalTimeSpent', 'progress', 'estimationTime', 'extended_task_duration_factor']].describe().to_string())

        # Recommandations
        logger.info("Recommandations...")
        logger.info(f"Facteur d'ajustement moyen (meilleur modèle {best_model_name}): {avg_predictions.mean() / 907:.2f} (907 = moyenne des estimations initiales)")
        logger.info(f"Features clés: {', '.join(feature_importance.head(5)['Feature'].tolist())}")

if __name__ == "__main__":
    main()