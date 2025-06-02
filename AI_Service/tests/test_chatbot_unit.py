import pytest
import os
import asyncio
import anyio
from fastapi.testclient import TestClient
from app.main import app
import json
from unittest.mock import AsyncMock, patch, Mock
from fastapi import WebSocketDisconnect, HTTPException
import logging
import requests

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')

# Set Windows event loop policy to avoid asyncio issues
if os.name == 'nt':
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    logger.debug("Set WindowsSelectorEventLoopPolicy for asyncio")

# Initialize TestClient
try:
    client = TestClient(app, backend="asyncio")
    logger.debug("TestClient initialized")
except Exception as e:
    logger.error(f"Failed to initialize TestClient: {str(e)}", exc_info=True)
    raise

@pytest.fixture(autouse=True)
def set_testing_env():
    logger.debug("Setting TESTING environment variable")
    os.environ["TESTING"] = "true"
    yield
    os.environ.pop("TESTING", None)

@pytest.fixture
def chat_request_data():
    return {
        "query": "Tasks for Narjiss EL Mjass?",
        "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"
    }

@pytest.fixture
def mock_rag_pipeline():
    with patch("app.api.v1.chatbot_endpoints.rag_pipeline", new_callable=AsyncMock) as mock:
        logger.debug("Mocking rag_pipeline")
        mock.return_value = (
            "intent_7",
            "narjiss el mjass has tasks: #3 - Implement Real-Time Comment System (due 2025-06-01), #1 - Define UI Requirements (due 2025-05-30).",
            [0.3],  # distances
            0.1,    # response_time
            {},     # metadata_results
            {"firstName": "narjiss", "lastName": "el mjass"},  # parameters
            0.05,   # search_time
            0.02    # param_time
        )
        yield mock

@pytest.fixture
def mock_rag_pipeline_error():
    with patch("app.chatbot.rag_pipeline.rag_pipeline", new_callable=AsyncMock) as mock, \
         patch("app.api.v1.chatbot_endpoints.rag_pipeline", new_callable=AsyncMock) as mock_endpoint:
        logger.debug("Mocking rag_pipeline to raise exception")
        mock.side_effect = Exception("RAG pipeline failed")
        mock_endpoint.side_effect = mock.side_effect
        yield mock

@pytest.fixture
def mock_fetch_api_response():
    with patch("app.chatbot.rag_pipeline.fetch_api_response") as mock:
        logger.debug("Mocking fetch_api_response")
        mock.return_value = {
            "tasks": [
                {"taskId": "3", "taskTitle": "Implement Real-Time Comment System", "dueDate": "2025-06-01"},
                {"taskId": "1", "taskTitle": "Define UI Requirements", "dueDate": "2025-05-30"}
            ]
        }
        yield mock

@pytest.fixture
def mock_redis_client():
    with patch("app.api.v1.chatbot_endpoints.redis_client") as mock:
        logger.debug("Mocking redis_client")
        mock.setex = AsyncMock()
        yield mock

@pytest.fixture
def mock_redis_client_error():
    with patch("app.api.v1.chatbot_endpoints.redis_client") as mock:
        logger.debug("Mocking redis_client to raise exception")
        mock.setex = AsyncMock(side_effect=Exception("Redis connection failed"))
        yield mock

@pytest.fixture
def mock_validate_token():
    with patch("app.api.v1.chatbot_endpoints.validate_token") as mock:
        logger.debug("Mocking validate_token")
        mock.return_value = "c961619d-04ea-4bdb-9f77-fa016c953d32"
        yield mock

@pytest.fixture
def mock_requests_get():
    with patch("requests.get") as mock:
        logger.debug("Mocking requests.get")
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"id": "c961619d-04ea-4bdb-9f77-fa016c953d32"}
        mock.return_value = mock_response
        yield mock

@pytest.fixture
def mock_settings():
    with patch("app.api.v1.chatbot_endpoints.settings") as mock:
        logger.debug("Mocking settings")
        mock.REDIS_URL = "redis://localhost:6379/0"
        mock.AUTH_SERVICE_URL = "http://localhost:8000"
        mock.TASK_API_URL = "http://localhost:8086/api/chatbot/tasks"
        yield mock

@pytest.fixture
def mock_file_operations():
    with patch("builtins.open") as mock_open, patch("pickle.load") as mock_pickle_load, patch("json.dump") as mock_json_dump:
        logger.debug("Mocking file operations")
        mock_open.return_value.__enter__.return_value = None
        mock_pickle_load.return_value = {}
        mock_json_dump.return_value = None
        yield mock_open, mock_pickle_load, mock_json_dump

@pytest.fixture
def mock_langdetect():
    with patch("app.chatbot.rag_pipeline.detect") as mock_detect:
        logger.debug("Mocking langdetect.detect")
        mock_detect.return_value = "en"
        yield mock_detect

# Tests for get_token
@pytest.mark.asyncio
async def test_get_token_valid():
    logger.debug("Running test_get_token_valid")
    from app.api.v1.chatbot_endpoints import get_token
    result = await get_token("Bearer valid_token")
    assert result == "valid_token"

@pytest.mark.asyncio
async def test_get_token_missing():
    logger.debug("Running test_get_token_missing")
    from app.api.v1.chatbot_endpoints import get_token
    result = await get_token(None)
    assert result is None

@pytest.mark.asyncio
async def test_get_token_no_bearer():
    logger.debug("Running test_get_token_no_bearer")
    from app.api.v1.chatbot_endpoints import get_token
    result = await get_token("invalid_token")
    assert result is None
    

# Tests for validate_token
def test_validate_token_test_mode(mock_settings):
    logger.debug("Running test_validate_token_test_mode")
    from app.api.v1.chatbot_endpoints import validate_token
    result = validate_token("valid_token")
    assert result == "c961619d-04ea-4bdb-9f77-fa016c953d32"

def test_validate_token_success(mock_requests_get, mock_settings):
    logger.debug("Running test_validate_token_success")
    os.environ["TESTING"] = "false"
    from app.api.v1.chatbot_endpoints import validate_token
    try:
        result = validate_token("real_token")
        assert result == "c961619d-04ea-4bdb-9f77-fa016c953d32"
    finally:
        os.environ["TESTING"] = "true"

def test_validate_token_no_id(mock_requests_get, mock_settings):
    logger.debug("Running test_validate_token_no_id")
    os.environ["TESTING"] = "false"
    mock_requests_get.return_value.json.return_value = {}
    from app.api.v1.chatbot_endpoints import validate_token
    try:
        with pytest.raises(HTTPException) as exc:
            validate_token("real_token")
        assert exc.value.status_code == 500  # Ajusté à 500
        assert "Error validating token: 401: Invalid token: no user ID" in exc.value.detail
    finally:
        os.environ["TESTING"] = "true"

def test_validate_token_request_failure(mock_requests_get, mock_settings):
    logger.debug("Running test_validate_token_request_failure")
    os.environ["TESTING"] = "false"
    mock_requests_get.side_effect = requests.exceptions.RequestException("Network error")
    from app.api.v1.chatbot_endpoints import validate_token
    try:
        with pytest.raises(HTTPException) as exc:
            validate_token("real_token")
        assert exc.value.status_code == 401
        assert "Network error" in exc.value.detail
    finally:
        os.environ["TESTING"] = "true"

# Tests for chat endpoint
def test_chat_endpoint_success(chat_request_data, mock_rag_pipeline, mock_fetch_api_response, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_chat_endpoint_success")
    response = client.post(
        "/api/v1/chat",
        json=chat_request_data,
        headers={"Authorization": "Bearer valid_token"}
    )
    logger.debug(f"Response status: {response.status_code}, body: {response.text}")
    assert response.status_code == 200, f"Expected status 200, got {response.status_code}"
    response_data = response.json()
    assert response_data["intent"] == "intent_7", f"Expected intent_7, got {response_data.get('intent')}"
    assert response_data["response"] == "narjiss el mjass has tasks: #3 - Implement Real-Time Comment System (due 2025-06-01), #1 - Define UI Requirements (due 2025-05-30)."
    assert response_data["parameters"] == {"firstName": "narjiss", "lastName": "el mjass"}
    assert response_data["confidence"] == 0.7
    assert response_data["buttons"] == []
    mock_rag_pipeline.assert_called_once_with(
        chat_request_data["query"],
        user_id=chat_request_data["userId"],
        token="valid_token"
    )
    
def test_chat_endpoint_missing_token(chat_request_data):
    logger.debug("Running test_chat_endpoint_missing_token")
    response = client.post(
        "/api/v1/chat",
        json=chat_request_data
    )
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid token: Missing or invalid Authorization header"

def test_chat_endpoint_invalid_query(chat_request_data, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_chat_endpoint_invalid_query")
    chat_request_data["query"] = ""
    response = client.post(
        "/api/v1/chat",
        json=chat_request_data,
        headers={"Authorization": "Bearer valid_token"}
    )
    assert response.status_code == 422

def test_chat_endpoint_no_user_id(chat_request_data, mock_rag_pipeline, mock_fetch_api_response, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_chat_endpoint_no_user_id")
    chat_request_data.pop("userId")
    response = client.post(
        "/api/v1/chat",
        json=chat_request_data,
        headers={"Authorization": "Bearer valid_token"}
    )
    assert response.status_code == 200
    response_data = response.json()
    assert response_data["intent"] == "intent_7"
    mock_rag_pipeline.assert_called_once_with(
        chat_request_data["query"],
        user_id=None,
        token="valid_token"
    )

def test_chat_endpoint_rag_pipeline_failure(chat_request_data, mock_rag_pipeline_error, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_chat_endpoint_rag_pipeline_failure")
    response = client.post(
        "/api/v1/chat",
        json=chat_request_data,
        headers={"Authorization": "Bearer valid_token"}
    )
    assert response.status_code == 500
    assert "Error processing query" in response.json()["detail"]

# Tests for websocket_chat endpoint
@pytest.mark.asyncio
async def test_websocket_chat_success(mock_rag_pipeline, mock_redis_client, mock_fetch_api_response, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_websocket_chat_success")
    with client.websocket_connect("/api/v1/ws/chat") as websocket:
        websocket.send_json({"token": "Bearer valid_token"})
        query_data = {
            "query": "Tasks for Narjiss EL Mjass?",
            "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"
        }
        websocket.send_json(query_data)
        typing_response = websocket.receive_json()
        assert typing_response == {"status": "typing"}
        response = websocket.receive_json()
        assert response["intent"] == "intent_7"
        assert response["response"] == "narjiss el mjass has tasks: #3 - Implement Real-Time Comment System (due 2025-06-01), #1 - Define UI Requirements (due 2025-05-30)."
        assert response["parameters"] == {"firstName": "narjiss", "lastName": "el mjass"}
        assert response["confidence"] == 0.7
        assert response["buttons"] == []
        mock_rag_pipeline.assert_called_once_with(
            query_data["query"],
            user_id=query_data["userId"],
            token="Bearer valid_token"  # Match endpoint behavior
        )
        mock_redis_client.setex.assert_called_once()
        
@pytest.mark.asyncio
async def test_websocket_chat_invalid_token():
    logger.debug("Running test_websocket_chat_invalid_token")
    async with anyio.create_task_group() as tg:
        async def websocket_task():
            with client.websocket_connect("/api/v1/ws/chat") as websocket:
                websocket.send_json({"token": "invalid_token"})
                response = websocket.receive_json()
                assert response == {"error": "Jeton invalide ou manquant"}
                with pytest.raises(WebSocketDisconnect):
                    websocket.receive_json()
        tg.start_soon(websocket_task)

@pytest.mark.asyncio
async def test_websocket_chat_empty_query(mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_websocket_chat_empty_query")
    async with anyio.create_task_group() as tg:
        async def websocket_task():
            with client.websocket_connect("/api/v1/ws/chat") as websocket:
                websocket.send_json({"token": "Bearer valid_token"})
                websocket.send_json({"query": "", "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"})
                response = websocket.receive_json()
                assert response == {"error": "Question vide"}
        tg.start_soon(websocket_task)

@pytest.mark.asyncio
async def test_websocket_chat_no_user_id(mock_rag_pipeline, mock_redis_client, mock_fetch_api_response, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_websocket_chat_no_user_id")
    with client.websocket_connect("/api/v1/ws/chat") as websocket:
        websocket.send_json({"token": "Bearer valid_token"})
        query_data = {"query": "Tasks for Narjiss EL Mjass?"}
        websocket.send_json(query_data)
        typing_response = websocket.receive_json()
        assert typing_response == {"status": "typing"}
        response = websocket.receive_json()
        assert response["intent"] == "intent_7"
        mock_rag_pipeline.assert_called_once_with(
            query_data["query"],
            user_id="c961619d-04ea-4bdb-9f77-fa016c953d32",  # From validate_token
            token="Bearer valid_token"  # Match endpoint behavior
        )
        mock_redis_client.setex.assert_called_once()
        
          
@pytest.mark.asyncio
async def test_websocket_chat_redis_failure(mock_rag_pipeline, mock_redis_client_error, mock_fetch_api_response, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_websocket_chat_redis_failure")
    async with anyio.create_task_group() as tg:
        async def websocket_task():
            with client.websocket_connect("/api/v1/ws/chat") as websocket:
                websocket.send_json({"token": "Bearer valid_token"})
                query_data = {
                    "query": "Tasks for Narjiss EL Mjass?",
                    "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"
                }
                websocket.send_json(query_data)
                typing_response = websocket.receive_json()
                assert typing_response == {"status": "typing"}
                response = websocket.receive_json()
                assert response["intent"] == "intent_7"
                mock_redis_client_error.setex.assert_called_once()
        tg.start_soon(websocket_task)

@pytest.mark.asyncio
async def test_websocket_chat_timeout(mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_websocket_chat_timeout")
    async with anyio.create_task_group() as tg:
        async def websocket_task():
            with client.websocket_connect("/api/v1/ws/chat") as websocket:
                # Simulate timeout by not sending initial token
                await asyncio.sleep(11)  # Exceed 10-second timeout
                response = websocket.receive_json()
                assert response == {"error": "Aucun jeton reçu dans les 10 secondes"}
                with pytest.raises(WebSocketDisconnect):
                    websocket.receive_json()
        tg.start_soon(websocket_task)

@pytest.mark.asyncio
async def test_websocket_chat_rag_pipeline_failure(mock_rag_pipeline_error, mock_redis_client, mock_fetch_api_response, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_websocket_chat_rag_pipeline_failure")
    async with anyio.create_task_group() as tg:
        async def websocket_task():
            with client.websocket_connect("/api/v1/ws/chat") as websocket:
                websocket.send_json({"token": "Bearer valid_token"})
                query_data = {
                    "query": "Tasks for Narjiss EL Mjass?",
                    "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"
                }
                websocket.send_json(query_data)
                typing_response = websocket.receive_json()
                assert typing_response == {"status": "typing"}
                response = websocket.receive_json()
                assert response["error"] == "RAG pipeline failed"
                with pytest.raises(WebSocketDisconnect):
                    websocket.receive_json()
        tg.start_soon(websocket_task)

@pytest.mark.asyncio
async def test_websocket_chat_invalid_json(mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_websocket_chat_invalid_json")
    async with anyio.create_task_group() as tg:
        async def websocket_task():
            with client.websocket_connect("/api/v1/ws/chat") as websocket:
                websocket.send_json({"token": "Bearer valid_token"})
                websocket.send_text("invalid_json")
                response = websocket.receive_json()
                assert "error" in response
                with pytest.raises(WebSocketDisconnect):
                    websocket.receive_json()
        tg.start_soon(websocket_task)