from fastapi import FastAPI
from app.api.v1.endpoints import router as api_router
from app.api.v1.chatbot_endpoints import router as chatbot_router
from app.core.config import settings
import logging
from app.core.logging import setup_logging
from fastapi.middleware.cors import CORSMiddleware


app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json"
)



app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],  # URL de ton frontend
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configurer le logging
setup_logging()

# Inclure les routes API
app.include_router(api_router, prefix=settings.API_V1_STR)
app.include_router(chatbot_router, prefix=settings.API_V1_STR)
@app.get("/")
async def root():
    return {"message": "Task Predictor Microservice"}