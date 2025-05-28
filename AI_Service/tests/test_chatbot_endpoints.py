import pytest
from fastapi.testclient import TestClient
from app.main import app
import json
from unittest.mock import AsyncMock, patch
from fastapi import WebSocketDisconnect

client = TestClient(app)

@pytest.fixture
def chat_request_data():
    return {
        "query": "Tasks for Narjiss EL Mjass?",
        "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"
    }

@pytest.fixture
def mock_rag_pipeline():
    with patch("app.api.v1.chatbot_endpoints.rag_pipeline") as mock:
        mock.return_value = (
            "intent_7",
            "Tasks assigned to Narjiss EL Mjass: #3 - Implement Real-Time Comment System, #1 - Define UI Requirements.",
            [0.3],  # distances
            0.1,    # response_time
            {},     # metadata_results
            {"firstName": "Narjiss", "lastName": "EL Mjass"},  # parameters
            0.05,   # search_time
            0.02    # param_time
        )
        yield mock

@pytest.fixture
def mock_redis_client():
    with patch("app.api.v1.chatbot_endpoints.redis_client") as mock:
        mock.setex = AsyncMock()
        yield mock

@pytest.fixture
def mock_validate_token():
    with patch("app.api.v1.chatbot_endpoints.validate_token") as mock:
        mock.return_value = "c961619d-04ea-4bdb-9f77-fa016c953d32"
        yield mock

def test_chat_endpoint_success(chat_request_data, mock_rag_pipeline, mock_validate_token):
    response = client.post(
        "/api/v1/chat",
        json=chat_request_data,
        headers={"Authorization": "Bearer valid_token"}
    )
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["intent"] == "intent_7"
    assert response_json["response"] == "Tasks assigned to Narjiss EL Mjass: #3 - Implement Real-Time Comment System, #1 - Define UI Requirements."
    assert response_json["parameters"] == {"firstName": "Narjiss", "lastName": "EL Mjass"}
    assert response_json["confidence"] == 0.7  # 1.0 - min([0.3])
    assert response_json["buttons"] == []
    mock_rag_pipeline.assert_called_once_with(
        chat_request_data["query"],
        user_id=chat_request_data["userId"],
        token="Bearer valid_token"
    )

def test_chat_endpoint_missing_token(chat_request_data):
    response = client.post("/api/v1/chat", json=chat_request_data)
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid token: Missing or invalid Authorization header"

def test_chat_endpoint_invalid_query(chat_request_data, mock_validate_token):
    chat_request_data["query"] = ""
    response = client.post(
        "/api/v1/chat",
        json=chat_request_data,
        headers={"Authorization": "Bearer valid_token"}
    )
    assert response.status_code == 422  # FastAPI validation error for empty query

@pytest.mark.asyncio
async def test_websocket_chat_success(mock_rag_pipeline, mock_redis_client, mock_validate_token):
    with client.websocket_connect("/api/v1/ws/chat") as websocket:
        # Send initial token
        websocket.send_json({"token": "Bearer valid_token"})
        # Send query
        query_data = {
            "query": "Tasks for Narjiss EL Mjass?",
            "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"
        }
        websocket.send_json(query_data)
        
        # Receive typing status
        typing_response = websocket.receive_json()
        assert typing_response == {"status": "typing"}
        
        # Receive actual response
        response = websocket.receive_json()
        assert response["intent"] == "intent_7"
        assert response["response"] == "Tasks assigned to Narjiss EL Mjass: #3 - Implement Real-Time Comment System, #1 - Define UI Requirements."
        assert response["parameters"] == {"firstName": "Narjiss", "lastName": "EL Mjass"}
        assert response["confidence"] == 0.7
        assert response["buttons"] == []
        
        mock_rag_pipeline.assert_called_once_with(
            query_data["query"],
            user_id=query_data["userId"],
            token="Bearer valid_token"
        )
        mock_redis_client.setex.assert_called_once()
        redis_call_args = mock_redis_client.setex.call_args[0]
        assert redis_call_args[0] == f"chat_context:{query_data['userId']}"
        assert redis_call_args[1] == 3600
        stored_context = json.loads(redis_call_args[2])
        assert stored_context["last_intent"] == "intent_7"
        assert stored_context["last_query"] == query_data["query"]

@pytest.mark.asyncio
async def test_websocket_chat_invalid_token():
    with client.websocket_connect("/api/v1/ws/chat") as websocket:
        websocket.send_json({"token": "invalid_token"})
        response = websocket.receive_json()
        assert response == {"error": "Jeton invalide ou manquant"}
        with pytest.raises(WebSocketDisconnect):
            websocket.receive_json()

@pytest.mark.asyncio
async def test_websocket_chat_empty_query(mock_validate_token):
    with client.websocket_connect("/api/v1/ws/chat") as websocket:
        websocket.send_json({"token": "Bearer valid_token"})
        websocket.send_json({"query": "", "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"})
        response = websocket.receive_json()
        assert response == {"error": "Question vide"}