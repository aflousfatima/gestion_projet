from fastapi import FastAPI
from app.api.v1.endpoints import router as api_router
from app.core.config import settings
import logging
from app.core.logging import setup_logging

app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json"
)

# Configurer le logging
setup_logging()

# Inclure les routes API
app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/")
async def root():
    return {"message": "Task Predictor Microservice"}