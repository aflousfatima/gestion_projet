from fastapi import APIRouter, HTTPException, Header
from pydantic import BaseModel
from app.chatbot.rag_pipeline import rag_pipeline
import logging

router = APIRouter()
logger = logging.getLogger(__name__)

class ChatRequest(BaseModel):
    query: str
    userId: str | None = None  # Optional for authenticated users or other intents

    class Config:
        json_schema_extra = {
            "example": {
                "query": "Tasks for Narjiss EL Mekkadem?"
            }
        }

class ChatResponse(BaseModel):
    intent: str
    response: str
    parameters: dict
    confidence: float

    class Config:
        json_schema_extra = {
            "example": {
                "intent": "intent_7",
                "response": "Tasks assigned to Narjiss EL Mekkadem: #3 (Implement Real-Time Comment System with WebSocket), #1 (Define User Interface Requirements).",
                "parameters": {"firstName": "Narjiss", "lastName": "EL Mekkadem"},
                "confidence": 0.70
            }
        }

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest, authorization: str | None = Header(None)):
    try:
        intent, response, distances, response_time, metadata_results, parameters, search_time, param_time = rag_pipeline(
            request.query, user_id=request.userId, token=authorization
        )
        confidence = 1.0 - min([d for d in distances if d != float('inf')] or [0.0])
        logger.info(f"Chat query: {request.query}, Intent: {intent}, Confidence: {confidence:.2f}")
        return ChatResponse(
            intent=intent,
            response=response,
            parameters=parameters,
            confidence=confidence
        )
    except Exception as e:
        logger.error(f"Error processing chat query: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error processing query: {str(e)}")