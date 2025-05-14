from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List

class TaskInput(BaseModel):
    title: str
    description: Optional[str] = ""
    estimationTime: float
    progress: float
    projectId: int
    creationDate: str  # Format: "YYYY-MM-DD"
    startDate: Optional[str] = None
    dueDate: Optional[str] = None
    status: str
    priority: str
    itemtags: Optional[str] = ""
    assignedUserIds: Optional[str] = ""

    class Config:
        json_schema_extra = {
            "example": {
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
        }

class PredictionResponse(BaseModel):
    task_title: str
    predicted_duration: float
    expected_duration: float
    model_name: str

    class Config:
        json_schema_extra = {
            "example": {
                "task_title": "Développer nouvelle API",
                "predicted_duration": 620.5,
                "expected_duration": 600.0,
                "model_name": "LightGBM"
            }
        }