import joblib
import numpy as np
from typing import Tuple

class TaskPredictor:
    def __init__(self, model_path: str):
        self.model = joblib.load(model_path)
        self.model_name = self.model.__class__.__name__

    def predict(self, X: np.ndarray, is_short_task: int, task_type: str, 
                estimation_time: float) -> float:
        pred = np.maximum(self.model.predict(X)[0], 0)
        
        # Ajustements comme dans votre script
        if is_short_task:
            pred *= 1.07
        elif estimation_time > 1500:
            pred *= 0.82
        elif estimation_time > 900:
            pred *= 0.95
        elif estimation_time > 500:
            pred *= 0.92
        if task_type == 'devops':
            pred *= 0.96
        elif task_type == 'feature':
            pred *= 0.959
            
        return pred