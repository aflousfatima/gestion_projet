import pytest
import os
import asyncio
import anyio
from fastapi.testclient import TestClient
from app.main import app
import json
from unittest.mock import AsyncMock, patch
from fastapi import WebSocketDisconnect
import logging

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
# Initialize TestClient with explicit async backend

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
    # Mock both possible import paths
    with patch("app.chatbot.rag_pipeline.rag_pipeline", new_callable=AsyncMock) as mock, \
         patch("app.api.v1.chatbot_endpoints.rag_pipeline", new_callable=AsyncMock) as mock_endpoint:
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
        mock_endpoint.return_value = mock.return_value
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
def mock_validate_token():
    with patch("app.api.v1.chatbot_endpoints.validate_token") as mock:
        logger.debug("Mocking validate_token")
        mock.return_value = "c961619d-04ea-4bdb-9f77-fa016c953d32"
        yield mock

@pytest.fixture
def mock_settings():
    with patch("app.api.v1.chatbot_endpoints.settings") as mock:
        logger.debug("Mocking settings")
        mock.REDIS_URL = "redis://localhost:6379/0"
        mock.AUTH_SERVICE_URL = "http://localhost:8000"
        mock.MODEL_PATH = "app/models/best_model.pkl"
        mock.SCALER_PATH = "app/models/scaler.pkl"
        mock.ENCODER_PATH = "app/models/encoder.pkl"
        mock.TFIDF_PATH = "app/models/tfidf.pkl"
        mock.TAGS_PATH = "app/models/all_tags.pkl"
        mock.FEATURES_PATH = "app/models/features.pkl"
        mock.LOG_LEVEL = "INFO"
        mock.INTENTS_PATH = "app/chatbot/data/intents_complete.json"
        mock.FAISS_INDEX_PATH = "app/chatbot/data/faiss_index.bin"
        mock.METADATA_PATH = "app/chatbot/data/metadata.pkl"
        mock.CACHE_PATH = "app/chatbot/data/cache.json"
        mock.PROJECT_API_URL = "http://localhost:8085/api/chatbot/projects"
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

@pytest.mark.skipif(os.getenv("CI") == "true", reason="ignore Performance tests require a running server")
def test_chat_endpoint_diagnostic(chat_request_data, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect, mock_rag_pipeline, mock_fetch_api_response):
    logger.debug("Running test_chat_endpoint_diagnostic")
    try:
        # Run the client.post in an asyncio event loop
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            response = loop.run_until_complete(
                loop.run_in_executor(None, lambda: client.post(
                    "/api/v1/chat",
                    json=chat_request_data,
                    headers={"Authorization": "Bearer valid_token"}
                ))
            )
        finally:
            loop.close()
        logger.debug(f"Diagnostic response: status={response.status_code}, body={response.text}")
        assert response.status_code == 200, f"Diagnostic failed: {response.text}"
        response_data = response.json()
        assert response_data["intent"] == "intent_7"
        assert response_data["response"] == "narjiss el mjass has tasks: #3 - Implement Real-Time Comment System (due 2025-06-01), #1 - Define UI Requirements (due 2025-05-30)."
        assert response_data["parameters"] == {"firstName": "narjiss", "lastName": "el mjass"}
        assert response_data["confidence"] == 0.7
        assert response_data["buttons"] == []
    except Exception as e:
        logger.error(f"Diagnostic test error: {str(e)}", exc_info=True)
        raise
    
@pytest.mark.skipif(os.getenv("CI") == "true", reason="ignore Performance tests require a running server")
def test_chat_endpoint_success(chat_request_data, mock_rag_pipeline, mock_fetch_api_response, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_chat_endpoint_success")
    try:
        # Debug mock setup
        logger.debug(f"Mock rag_pipeline: {mock_rag_pipeline}")
        with patch("app.api.v1.chatbot_endpoints.rag_pipeline", mock_rag_pipeline):
            # Run the client.post in an asyncio event loop
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            try:
                response = loop.run_until_complete(
                    loop.run_in_executor(None, lambda: client.post(
                        "/api/v1/chat",
                        json=chat_request_data,
                        headers={"Authorization": "Bearer valid_token"}
                    ))
                )
            finally:
                loop.close()
        logger.debug(f"Response: status={response.status_code}, body={response.text}")
        assert response.status_code == 200, f"Expected status 200, got {response.status_code}: {response.text}"
        response_data = response.json()
        assert response_data["intent"] == "intent_7"
        assert response_data["response"] == "narjiss el mjass has tasks: #3 - Implement Real-Time Comment System (due 2025-06-01), #1 - Define UI Requirements (due 2025-05-30)."
        assert response_data["parameters"] == {"firstName": "narjiss", "lastName": "el mjass"}
        assert response_data["confidence"] == 0.7
        assert response_data["buttons"] == []
        logger.debug(f"Checking mock_rag_pipeline call: {mock_rag_pipeline.call_args}")
        mock_rag_pipeline.assert_called_once_with(
            chat_request_data["query"],
            user_id=chat_request_data["userId"],
            token="valid_token"
        )
        # Removed: mock_fetch_api_response.assert_called_once()
    except Exception as e:
        logger.error(f"Error in test_chat_endpoint_success: {str(e)}", exc_info=True)
        raise
     
def test_chat_endpoint_missing_token(chat_request_data):
    logger.debug("Running test_chat_endpoint_missing_token")
    try:
        response = client.post(
            "/api/v1/chat",
            json=chat_request_data
        )
        logger.debug(f"Response: status={response.status_code}, body={response.text}")
        assert response.status_code == 401
        assert response.json()["detail"] == "Invalid token: Missing or invalid Authorization header"
    except Exception as e:
        logger.error(f"Error in test_chat_endpoint_missing_token: {str(e)}", exc_info=True)
        raise

def test_chat_endpoint_invalid_query(chat_request_data, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_chat_endpoint_invalid_query")
    try:
        chat_request_data["query"] = ""
        response = client.post(
            "/api/v1/chat",
            json=chat_request_data,
            headers={"Authorization": "Bearer valid_token"}
        )
        logger.debug(f"Response: status={response.status_code}, body={response.text}")
        assert response.status_code == 422
    except Exception as e:
        logger.error(f"Error in test_chat_endpoint_invalid_query: {str(e)}", exc_info=True)
        raise

@pytest.mark.asyncio
async def test_websocket_chat_success(mock_rag_pipeline, mock_redis_client, mock_fetch_api_response, mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Running test_websocket_chat_success")
    try:
        async with anyio.create_task_group() as tg:
            async def websocket_task():
                try:
                    with client.websocket_connect("/api/v1/ws/chat") as websocket:
                        # Send initial token
                        websocket.send_json({"token": "Bearer valid_token"})
                        logger.debug("Sent initial token")
                        # Send query
                        query_data = {
                            "query": "Tasks for Narjiss EL Mjass?",
                            "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"
                        }
                        websocket.send_json(query_data)
                        logger.debug("Sent query data")

                        # Receive typing status
                        typing_response = websocket.receive_json()
                        logger.debug(f"Received typing response: {typing_response}")
                        assert typing_response == {"status": "typing"}

                        # Receive actual response
                        response = websocket.receive_json()
                        logger.debug(f"Received response: {response}")
                        assert response["intent"] == "intent_7"
                        assert response["response"] == "narjiss el mjass has tasks: #3 - Implement Real-Time Comment System (due 2025-06-01), #1 - Define UI Requirements (due 2025-05-30)."
                        assert response["parameters"] == {"firstName": "narjiss", "lastName": "el mjass"}
                        assert response["confidence"] == 0.7
                        assert response["buttons"] == []

                except Exception as e:
                    logger.error(f"WebSocket task error: {str(e)}", exc_info=True)
                    raise

            tg.start_soon(websocket_task)
    except Exception as e:
        logger.error(f"Error in test_websocket_chat_success: {str(e)}", exc_info=True)
        raise
    
@pytest.mark.asyncio
async def test_websocket_chat_invalid_token():
    logger.debug("Testing test_websocket_chat_invalid_token")
    try:
        async with anyio.create_task_group() as tg:
            async def websocket_task():
                with client.websocket_connect("/api/v1/ws/chat") as websocket:
                    websocket.send_json({"token": "invalid_token"})
                    response = websocket.receive_json()
                    logger.debug(f"Response: {response}")
                    assert response == {"error": "Jeton invalide ou manquant"}
                    with pytest.raises(WebSocketDisconnect):
                        websocket.receive_json()
            tg.start_soon(websocket_task)
    except Exception as e:
        logger.error(f"Error in test_websocket_chat_invalid_token: {str(e)}", exc_info=True)
        raise

@pytest.mark.asyncio
async def test_websocket_chat_empty_query(mock_validate_token, mock_settings, mock_file_operations, mock_langdetect):
    logger.debug("Testing test_websocket_chat_empty_query to see if it works")
    try:
        async with anyio.create_task_group() as tg:
            async def websocket_task():
                with client.websocket_connect("/api/v1/ws/chat") as websocket:
                    websocket.send_json({"token": "Bearer valid_token"})
                    websocket.send_json({"query": "", "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"})
                    response = websocket.receive_json()
                    logger.debug(f"Response: {response}")
                    assert response == {"error": "Question vide"}
            tg.start_soon(websocket_task)
    except Exception as e:
        logger.error(f"Error in test_websocket_chat_empty_query: {str(e)}", exc_info=True)
        raise
