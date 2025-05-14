import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_predict_endpoint():
    task = {
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
    
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 200
    assert "predicted_duration" in response.json()
    assert "task_title" in response.json()
    assert response.json()["task_title"] == task["title"]