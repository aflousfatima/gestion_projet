from fastapi import APIRouter, HTTPException, WebSocket, WebSocketDisconnect, Header, Depends
from pydantic import BaseModel, ConfigDict
from pydantic.functional_validators import field_validator
from app.chatbot.rag_pipeline import rag_pipeline
import logging
import json
import requests
from redis.asyncio import Redis
from app.core.config import settings
from typing import Optional
import asyncio
import os

router = APIRouter()
logger = logging.getLogger(__name__)

# Configure logging to be more verbose
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')

# Log settings initialization
logger.debug(f"Settings loaded: REDIS_URL={settings.REDIS_URL}, AUTH_SERVICE_URL={settings.AUTH_SERVICE_URL}")

# Initialize Redis client
try:
    redis_client = Redis.from_url(settings.REDIS_URL, decode_responses=True)
    logger.debug("Redis client initialized successfully")
except Exception as e:
    logger.error(f"Failed to initialize Redis client: {str(e)}")
    raise

class ChatRequest(BaseModel):
    query: str
    userId: Optional[str] = None

    @field_validator('query')
    def query_must_not_be_empty(cls, v):
        if not v.strip():
            raise ValueError("Query cannot be empty")
        return v

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "query": "Tasks for Narjiss EL Mjass?",
                "userId": "c961619d-04ea-4bdb-9f77-fa016c953d32"
            }
        }
    )

class ChatResponse(BaseModel):
    intent: str
    response: str
    parameters: dict
    confidence: float
    buttons: list = []

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "intent": "intent_7",
                "response": "Tasks assigned to narjiss el mjass: #3 - Implement Real-Time Comment System, #1 - Define UI Requirements.",
                "parameters": {"firstName": "narjiss", "lastName": "el mjass"},
                "confidence": 0.7,
                "buttons": []
            }
        }
    )

async def get_token(authorization: Optional[str] = Header(None)) -> Optional[str]:
    logger.debug(f"Received Authorization header: {authorization}")
    if not authorization or not authorization.startswith("Bearer "):
        logger.warning("Missing or invalid Authorization header")
        return None
    token = authorization.replace("Bearer ", "")
    logger.debug(f"Extracted token: {token[:10]}...")
    return token

def validate_token(token: str) -> str:
    logger.debug(f"Validating token: {token[:10]}...")
    if os.getenv("TESTING") == "true" and token == "valid_token":
        logger.debug("Bypassing token validation for test")
        return "c961619d-04ea-4bdb-9f77-fa016c953d32"
    try:
        logger.debug(f"Calling auth service at: {settings.AUTH_SERVICE_URL}/api/me")
        response = requests.get(
            f"{settings.AUTH_SERVICE_URL}/api/me",
            headers={"Authorization": f"Bearer {token}"},
            timeout=5
        )
        response.raise_for_status()
        user_info = response.json()
        logger.debug(f"Auth service response: {user_info}")
        user_id = user_info.get("id")
        if not user_id:
            logger.error("No 'id' field in auth service response")
            raise HTTPException(status_code=401, detail="Invalid token: no user ID")
        logger.info(f"Token validated successfully for user ID: {user_id}")
        return user_id
    except requests.exceptions.RequestException as e:
        logger.error(f"Token validation failed: {str(e)}")
        raise HTTPException(status_code=401, detail=f"Invalid token: {str(e)}")
    except Exception as e:
        logger.error(f"Unexpected error in validate_token: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Error validating token: {str(e)}")

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest, token: Optional[str] = Depends(get_token)):
    logger.debug(f"Received POST /chat request: query={request.query}, userId={request.userId}, token={'present' if token else 'missing'}")
    if not token:
        logger.error("Missing or invalid Authorization header")
        raise HTTPException(status_code=401, detail="Invalid token: Missing or invalid Authorization header")
    try:
        logger.debug(f"Calling rag_pipeline with query: {request.query}, user_id: {request.userId}, token: {token[:10]}...")
        intent, response, distances, response_time, metadata_results, parameters, search_time, param_time = await rag_pipeline(
            request.query, user_id=request.userId, token=token
        )
        logger.debug("rag_pipeline returned successfully")
        confidence = 1.0 - min([d for d in distances if d != float('inf')] or [0.0])
        logger.info(f"Chat query processed: query={request.query}, intent={intent}, confidence={confidence:.2f}, response={response}")
        return ChatResponse(
            intent=intent,
            response=response,
            parameters=parameters,
            confidence=confidence,
            buttons=[]
        )
    except Exception as e:
        logger.error(f"Error processing chat query: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Error processing query: {str(e)}")

@router.websocket("/ws/chat")
async def websocket_chat(websocket: WebSocket):
    await websocket.accept()
    logger.info("WebSocket connection accepted")
    user_id = None
    try:
        logger.debug("Waiting for initial JSON message with token")
        initial_data = await asyncio.wait_for(websocket.receive_json(), timeout=10.0)
        logger.debug(f"Received initial message: {initial_data}")
        token = initial_data.get("token")
        if not token or not token.startswith("Bearer "):
            logger.error("Invalid or missing token in initial message")
            await websocket.send_json({"error": "Jeton invalide ou manquant"})
            await websocket.close(code=1008)
            return

        logger.debug(f"Extracted token: {token[:10]}...")
        token = token.replace("Bearer ", "")
        logger.debug(f"Validating token: {token[:10]}...")
        user_id = validate_token(token)
        logger.info(f"WebSocket authenticated for user ID: {user_id}")

        while True:
            logger.debug("Waiting for client message")
            data = await websocket.receive_json()
            logger.debug(f"Received message: {data}")
            query = data.get("query", "")
            user_id_from_message = data.get("userId") or user_id

            if not query:
                logger.warning("Empty query received")
                await websocket.send_json({"error": "Question vide"})
                continue

            logger.info(f"Processing query: {query} for user ID: {user_id_from_message}")
            await websocket.send_json({"status": "typing"})
            logger.debug("Sent typing status")

            logger.debug(f"Calling rag_pipeline with query: {query}, user_id: {user_id_from_message}, token: Bearer {token[:10]}...")
            intent, response, distances, response_time, metadata_results, parameters, search_time, param_time = await rag_pipeline(
                query, user_id=user_id_from_message, token=f"Bearer {token}"
            )
            logger.debug("rag_pipeline returned successfully")
            confidence = 1.0 - min([d for d in distances if d != float('inf')] or [0.0])
            logger.info(f"Query processed: intent={intent}, confidence={confidence:.2f}, response={response}")

            redis_key = f"chat_context:{user_id_from_message}"
            logger.debug(f"Storing context in Redis with key: {redis_key}")
            try:
                await redis_client.setex(redis_key, 3600, json.dumps({
                    "last_intent": intent,
                    "last_parameters": parameters,
                    "last_query": query
                }))
                logger.debug("Context stored in Redis")
            except Exception as e:
                logger.error(f"Failed to store context in Redis: {str(e)}")

            response_data = {
                "intent": intent,
                "response": response,
                "parameters": parameters,
                "confidence": confidence,
                "buttons": []
            }
            logger.debug(f"Sending response: {response_data}")
            await websocket.send_json(response_data)
            logger.info(f"Response sent for query: {query}")

    except asyncio.TimeoutError:
        logger.error("Timeout waiting for initial message with token")
        await websocket.send_json({"error": "Aucun jeton reçu dans les 10 secondes"})
        await websocket.close(code=1008)
    except WebSocketDisconnect:
        logger.info(f"WebSocket disconnected for user ID: {user_id or 'unknown'}")
    except Exception as e:
        logger.error(f"WebSocket error: {str(e)}", exc_info=True)
        await websocket.send_json({"error": str(e)})
        await websocket.close(code=1011)