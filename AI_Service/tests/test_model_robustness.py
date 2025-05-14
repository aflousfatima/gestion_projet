import pytest
from fastapi.testclient import TestClient
from app.main import app
import copy

client = TestClient(app)

# Base valid task for reference
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
    """Test a valid task returns a reasonable prediction."""
    response = client.post("/api/v1/predict", json=valid_task)
    assert response.status_code == 200
    data = response.json()
    assert data["task_title"] == valid_task["title"]
    assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
    assert data["predicted_duration"] <= valid_task["estimationTime"] * 2, "Prediction exceeds reasonable bound"
    assert data["expected_duration"] == valid_task["estimationTime"]
    assert data["model_name"] in ["LightGBM", "RandomForest", "XGBoost", "ExtraTreesRegressor"], "Invalid model name"
    print(f"Valid task prediction: {data['predicted_duration']:.2f} minutes")

def test_missing_title():
    """Test missing title returns 422."""
    task = copy.deepcopy(valid_task)
    del task["title"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "title" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required", "Unexpected error message"
    print("Missing title handled correctly")

def test_missing_estimationTime():
    """Test missing estimationTime returns 422."""
    task = copy.deepcopy(valid_task)
    del task["estimationTime"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "estimationTime" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required", "Unexpected error message"
    print("Missing estimationTime handled correctly")

def test_missing_progress():
    """Test missing progress returns 422."""
    task = copy.deepcopy(valid_task)
    del task["progress"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "progress" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required", "Unexpected error message"
    print("Missing progress handled correctly")

def test_missing_projectId():
    """Test missing projectId returns 422."""
    task = copy.deepcopy(valid_task)
    del task["projectId"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "projectId" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required", "Unexpected error message"
    print("Missing projectId handled correctly")

def test_missing_creationDate():
    """Test missing creationDate returns 422."""
    task = copy.deepcopy(valid_task)
    del task["creationDate"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "creationDate" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required", "Unexpected error message"
    print("Missing creationDate handled correctly")

def test_missing_status():
    """Test missing status returns 422."""
    task = copy.deepcopy(valid_task)
    del task["status"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "status" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required", "Unexpected error message"
    print("Missing status handled correctly")

def test_missing_priority():
    """Test missing priority returns 422."""
    task = copy.deepcopy(valid_task)
    del task["priority"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "priority" in response.json()["detail"][0]["loc"]
    assert response.json()["detail"][0]["msg"].lower() == "field required", "Unexpected error message"
    print("Missing priority handled correctly")

def test_empty_itemtags():
    """Test empty itemtags is accepted (optional field)."""
    task = copy.deepcopy(valid_task)
    task["itemtags"] = ""
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 200
    data = response.json()
    assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
    print(f"Empty itemtags prediction: {data['predicted_duration']:.2f} minutes")

def test_invalid_estimationTime_type():
    """Test non-numeric estimationTime returns 422."""
    task = copy.deepcopy(valid_task)
    task["estimationTime"] = "invalid"
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "estimationTime" in response.json()["detail"][0]["loc"]
    assert "number" in response.json()["detail"][0]["msg"].lower(), "Unexpected error message"
    print("Invalid estimationTime type handled correctly")

def test_negative_estimationTime():
    """Test negative estimationTime returns 200, 422, or 500 (depending on preprocess_task)."""
    task = copy.deepcopy(valid_task)
    task["estimationTime"] = -100.0
    response = client.post("/api/v1/predict", json=task)
    if response.status_code == 200:
        data = response.json()
        assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
        print(f"Negative estimationTime prediction: {data['predicted_duration']:.2f} minutes")
    else:
        assert response.status_code in [422, 500], f"Unexpected status code: {response.status_code}"
        assert "estimationTime" in response.json()["detail"].lower() or "negative" in response.json()["detail"].lower()
        print("Negative estimationTime rejected correctly")

def test_zero_estimationTime():
    """Test zero estimationTime returns 200, 422, or 500."""
    task = copy.deepcopy(valid_task)
    task["estimationTime"] = 0.0
    response = client.post("/api/v1/predict", json=task)
    if response.status_code == 200:
        data = response.json()
        assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
        print(f"Zero estimationTime prediction: {data['predicted_duration']:.2f} minutes")
    else:
        assert response.status_code in [422, 500]
        print("Zero estimationTime rejected correctly")

def test_extreme_estimationTime():
    """Test very large estimationTime returns reasonable prediction."""
    task = copy.deepcopy(valid_task)
    task["estimationTime"] = 1e6
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 200
    data = response.json()
    assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
    assert data["predicted_duration"] <= task["estimationTime"] * 2, "Prediction exceeds reasonable bound"
    print(f"Extreme estimationTime prediction: {data['predicted_duration']:.2f} minutes")

def test_invalid_progress_type():
    """Test non-numeric progress returns 422."""
    task = copy.deepcopy(valid_task)
    task["progress"] = "invalid"
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "progress" in response.json()["detail"][0]["loc"]
    assert "number" in response.json()["detail"][0]["msg"].lower(), "Unexpected error message"
    print("Invalid progress type handled correctly")

def test_negative_progress():
    """Test negative progress returns 200, 422, or 500."""
    task = copy.deepcopy(valid_task)
    task["progress"] = -10.0
    response = client.post("/api/v1/predict", json=task)
    if response.status_code == 200:
        data = response.json()
        assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
        print(f"Negative progress prediction: {data['predicted_duration']:.2f} minutes")
    else:
        assert response.status_code in [422, 500]
        assert "progress" in response.json()["detail"].lower() or "negative" in response.json()["detail"].lower()
        print("Negative progress rejected correctly")

def test_excessive_progress():
    """Test progress > 100 returns 200, 422, or 500."""
    task = copy.deepcopy(valid_task)
    task["progress"] = 150.0
    response = client.post("/api/v1/predict", json=task)
    if response.status_code == 200:
        data = response.json()
        assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
        print(f"Excessive progress prediction: {data['predicted_duration']:.2f} minutes")
    else:
        assert response.status_code in [422, 500]
        assert "progress" in response.json()["detail"].lower() or "100" in response.json()["detail"].lower()
        print("Excessive progress rejected correctly")

def test_invalid_status():
    """Test invalid status returns 200, 422, or 500."""
    task = copy.deepcopy(valid_task)
    task["status"] = "INVALID"
    response = client.post("/api/v1/predict", json=task)
    if response.status_code == 200:
        data = response.json()
        assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
        print(f"Invalid status prediction: {data['predicted_duration']:.2f} minutes")
    else:
        assert response.status_code in [422, 500]
        assert "status" in response.json()["detail"].lower() or "invalid" in response.json()["detail"].lower()
        print("Invalid status rejected correctly")

def test_malformed_itemtags():
    """Test malformed itemtags returns 200, 422, or 500."""
    task = copy.deepcopy(valid_task)
    task["itemtags"] = ",,,"
    response = client.post("/api/v1/predict", json=task)
    if response.status_code == 200:
        data = response.json()
        assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
        print(f"Malformed itemtags prediction: {data['predicted_duration']:.2f} minutes")
    else:
        assert response.status_code in [422, 500]
        assert "itemtags" in response.json()["detail"].lower() or "invalid" in response.json()["detail"].lower()
        print("Malformed itemtags rejected correctly")

def test_non_string_itemtags():
    """Test non-string itemtags returns 422."""
    task = copy.deepcopy(valid_task)
    task["itemtags"] = ["backend", "api"]
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 422
    assert "itemtags" in response.json()["detail"][0]["loc"]
    assert "string" in response.json()["detail"][0]["msg"].lower(), "Unexpected error message"
    print("Non-string itemtags handled correctly")

def test_unknown_itemtags():
    """Test unknown itemtags returns 200, 422, or 500."""
    task = copy.deepcopy(valid_task)
    task["itemtags"] = "unknown_tag"
    response = client.post("/api/v1/predict", json=task)
    if response.status_code == 200:
        data = response.json()
        assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
        print(f"Unknown itemtags prediction: {data['predicted_duration']:.2f} minutes")
    else:
        assert response.status_code in [422, 500]
        print("Unknown itemtags rejected correctly")

def test_stress_large_itemtags():
    """Test large itemtags list returns reasonable prediction or 422/500."""
    task = copy.deepcopy(valid_task)
    task["itemtags"] = ",".join(["tag" + str(i) for i in range(100)])
    response = client.post("/api/v1/predict", json=task)
    if response.status_code == 200:
        data = response.json()
        assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
        print(f"Large itemtags prediction: {data['predicted_duration']:.2f} minutes")
    else:
        assert response.status_code in [422, 500]
        print("Large itemtags handled correctly")

def test_stress_long_description():
    """Test long description returns reasonable prediction."""
    task = copy.deepcopy(valid_task)
    task["description"] = "A" * 10000
    response = client.post("/api/v1/predict", json=task)
    assert response.status_code == 200
    data = response.json()
    assert data["predicted_duration"] >= 0, "Prediction must be non-negative"
    print(f"Long description prediction: {data['predicted_duration']:.2f} minutes")

def test_consistent_predictions():
    """Test similar valid tasks yield consistent predictions."""
    task1 = copy.deepcopy(valid_task)
    task2 = copy.deepcopy(valid_task)
    task2["title"] = "Slightly Different API"
    task2["description"] = "Créer un autre endpoint"
    response1 = client.post("/api/v1/predict", json=task1)
    response2 = client.post("/api/v1/predict", json=task2)
    assert response1.status_code == 200
    assert response2.status_code == 200
    pred1 = response1.json()["predicted_duration"]
    pred2 = response2.json()["predicted_duration"]
    assert abs(pred1 - pred2) < 50, f"Predictions differ too much: {pred1:.2f} vs {pred2:.2f}"
    print(f"Consistent predictions: task1={pred1:.2f}, task2={pred2:.2f}")