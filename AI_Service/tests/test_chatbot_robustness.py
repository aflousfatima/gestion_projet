import pytest
import asyncio
import json
from fastapi.testclient import TestClient
from fastapi import WebSocketDisconnect
from app.main import app
from fakeredis.aioredis import FakeRedis
from unittest.mock import patch, AsyncMock, MagicMock
import logging

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')

client = TestClient(app, backend="asyncio")

# Définir les fixtures
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
        mock.TASK_API_URL = "http://localhost:8086/api/chatbot/tasks"
        yield mock

@pytest.fixture
def mock_requests_get():
    with patch("requests.get") as mock:
        logger.debug("Mocking requests.get")
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "tasks": [
                {"id": "3", "title": "Implement Real-Time Comment System", "dueDate": "2025-06-01"},
                {"id": "1", "title": "Define UI Requirements", "dueDate": "2025-05-30"}
            ]
        }
        mock.return_value = mock_response
        yield mock

@pytest.fixture
def mock_rag_pipeline():
    with patch("app.api.v1.chatbot_endpoints.rag_pipeline", new_callable=AsyncMock) as mock:
        logger.debug("Mocking rag_pipeline")
        mock.return_value = (
            "intent_7",
            "narjiss el mjass has tasks: #3 - Implement Real-Time Comment System (due 2025-06-01), #1 - Define UI Requirements (due 2025-05-30).",
            [0.3],
            0.1,
            {},
            {"firstName": "narjiss", "lastName": "el mjass"},
            0.05,
            0.02
        )
        yield mock

@pytest.mark.asyncio
async def test_websocket_robustness_redis_failure(mock_validate_token, mock_settings, mock_requests_get, mock_rag_pipeline, caplog):
    logger.info("Running WebSocket robustness test: Redis failure")
    
    # Simuler une panne de Redis
    with patch("app.api.v1.chatbot_endpoints.redis_client") as mock_redis:
        mock_redis.setex = AsyncMock(side_effect=Exception("Redis connection failed"))
        
        try:
            with client.websocket_connect("/api/v1/ws/chat") as websocket:
                # Envoyer un token valide
                websocket.send_json({"token": "Bearer valid_token"})
                # Envoyer une requête valide
                query_data = {
                    "query": "Tasks for Narjiss EL Mjass?",
                    "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"
                }
                websocket.send_json(query_data)
                
                # Consommer le message "typing"
                typing_response = websocket.receive_json()
                logger.debug(f"Received typing response: {typing_response}")
                assert typing_response == {"status": "typing"}
                
                # Vérifier la réponse (endpoint envoie une réponse réussie malgré l'erreur Redis)
                response = websocket.receive_json()
                logger.debug(f"Received response: {response}")
                assert response["intent"] == "intent_7"
                assert "narjiss el mjass has tasks" in response["response"]
                assert response["confidence"] == pytest.approx(0.7, 0.1)
                assert response["parameters"] == {"firstName": "narjiss", "lastName": "el mjass"}
                
                # Vérifier que l'erreur Redis a été loggée
                assert "Failed to store context in Redis: Redis connection failed" in caplog.text
        except WebSocketDisconnect as e:
            logger.error(f"WebSocket disconnected unexpectedly: {str(e)}")
            pytest.fail(f"WebSocket disconnected unexpectedly: {str(e)}")

@pytest.mark.asyncio
async def test_websocket_robustness_invalid_input(mock_validate_token, mock_settings, mock_requests_get, mock_rag_pipeline):
    logger.info("Running WebSocket robustness test: Invalid input")
    
    try:
        with client.websocket_connect("/api/v1/ws/chat") as websocket:
            # Envoyer un token valide
            websocket.send_json({"token": "Bearer valid_token"})
            # Envoyer une requête sans userId
            query_data = {
                "query": "Tasks for Narjiss EL Mjass?"
                # userId manquant intentionnellement
            }
            websocket.send_json(query_data)
            
            # Consommer le message "typing"
            typing_response = websocket.receive_json()
            logger.debug(f"Received typing response: {typing_response}")
            assert typing_response == {"status": "typing"}
            
            # Vérifier la réponse (endpoint utilise user_id du token si userId manquant)
            response = websocket.receive_json()
            logger.debug(f"Received response: {response}")
            assert response["intent"] == "intent_7"
            assert "narjiss el mjass has tasks" in response["response"]
            assert response["confidence"] == pytest.approx(0.7, 0.1)
            assert response["parameters"] == {"firstName": "narjiss", "lastName": "el mjass"}
    except WebSocketDisconnect as e:
        logger.error(f"WebSocket disconnected unexpectedly: {str(e)}")
        pytest.fail(f"WebSocket disconnected unexpectedly: {str(e)}")

@pytest.mark.asyncio
async def test_websocket_robustness_disconnect(mock_validate_token, mock_settings, mock_requests_get, mock_rag_pipeline):
    logger.info("Running WebSocket robustness test: Client disconnect")
    
    try:
        with client.websocket_connect("/api/v1/ws/chat") as websocket:
            # Envoyer un token valide
            websocket.send_json({"token": "Bearer valid_token"})
            # Simuler une déconnexion brutale
            websocket.close()
            
            # Vérifier que le serveur ne plante pas
            logger.debug("Client disconnected, server should handle gracefully")
    except WebSocketDisconnect:
        logger.debug("Expected WebSocketDisconnect caught, server handled disconnection gracefully")