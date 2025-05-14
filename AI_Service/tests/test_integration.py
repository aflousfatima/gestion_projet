import pytest
from fastapi.testclient import TestClient
from app.main import app
import copy

client = TestClient(app)

# Tâche valide pour les tests
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

def test_valid_task():
    """Test qu'une tâche valide retourne HTTP 200 avec une réponse correcte."""
    response = client.post("/api/v1/predict", json=valid_task)
    assert response.status_code == 200
    data = response.json()
    assert data["task_title"] == valid_task["title"]
    assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
    assert data["expected_duration"] == valid_task["estimationTime"]
    assert data["model_name"] in ["LightGBM", "RandomForest", "XGBoost", "ExtraTreesRegressor"]
    assert all(key in data for key in ["task_title", "predicted_duration", "expected_duration", "model_name"])
    print(f"Valid task prediction: {data['predicted_duration']:.2f} minutes")

def test_valid_task_empty_optional_fields():
    """Test qu'une tâche valide avec des champs optionnels vides retourne HTTP 200."""
    task = copy.deepcopy(valid_task)
    task["description"] = ""
    task["itemtags"] = ""
    task["assignedUserIds"] = ""
    task["startDate"] = None
    task["dueDate"] = None
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 200
    data = response.json()
    assert data["task_title"] == task["title"]
    assert data["predicted_duration"] >= 0
    assert data["expected_duration"] == task["estimationTime"]
    print(f"Valid task with empty optional fields prediction: {data['predicted_duration']:.2f} minutes")

def test_missing_title():
    """Test qu'un titre manquant retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    del task["title"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "title" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required"
    print("Missing title handled correctly")

def test_missing_estimationTime():
    """Test qu'un estimationTime manquant retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    del task["estimationTime"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "estimationTime" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required"
    print("Missing estimationTime handled correctly")

def test_missing_progress():
    """Test qu'un progress manquant retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    del task["progress"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "progress" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required"
    print("Missing progress handled correctly")

def test_missing_projectId():
    """Test qu'un projectId manquant retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    del task["projectId"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "projectId" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required"
    print("Missing projectId handled correctly")

def test_missing_creationDate():
    """Test qu'une creationDate manquante retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    del task["creationDate"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "creationDate" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required"
    print("Missing creationDate handled correctly")

def test_missing_status():
    """Test qu'un status manquant retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    del task["status"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "status" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required"
    print("Missing status handled correctly")

def test_missing_priority():
    """Test qu'une priority manquante retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    del task["priority"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "priority" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required"
    print("Missing priority handled correctly")

def test_invalid_estimationTime_type():
    """Test qu'un estimationTime non numérique retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    task["estimationTime"] = "invalid"
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "estimationTime" in response.json()["detail"][0]["loc"]
    assert "number" in response.json()["detail"][0]["msg"].lower()
    print("Invalid estimationTime type handled correctly")

def test_invalid_progress_type():
    """Test qu'un progress non numérique retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    task["progress"] = "invalid"
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "progress" in response.json()["detail"][0]["loc"]
    assert "number" in response.json()["detail"][0]["msg"].lower()
    print("Invalid progress type handled correctly")

def test_non_string_itemtags():
    """Test qu'un itemtags non chaîne retourne HTTP 422."""
    task = copy.deepcopy(valid_task)
    task["itemtags"] = ["backend", "api"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "itemtags" in response.json()["detail"][0]["loc"]
    assert "string" in response.json()["detail"][0]["msg"].lower()
    print("Non-string itemtags handled correctly")