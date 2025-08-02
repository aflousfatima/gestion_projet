from fastapi import FastAPI
from app.api.v1.endpoints import router as api_router
from app.api.v1.chatbot_endpoints import router as chatbot_router
from app.core.config import settings
import logging
from app.core.logging import setup_logging
from fastapi.middleware.cors import CORSMiddleware
import requests
import uuid
import socket
from prometheus_fastapi_instrumentator import Instrumentator
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


Instrumentator().instrument(app).expose(app, endpoint="/actuator/prometheus")


# Configurer le logging
setup_logging()

# Inclure les routes API
app.include_router(api_router, prefix=settings.API_V1_STR)
app.include_router(chatbot_router, prefix=settings.API_V1_STR)
@app.get("/")
async def root():
    return {"message": "Task Predictor Microservice"}

# Adresse et port de ton service FastAPI
SERVICE_NAME = "IA_Service"
SERVICE_HOST = "192.168.1.154"
SERVICE_PORT = 8000
CONSUL_HOST = "localhost"
CONSUL_PORT = 8500

# Génère un ID unique pour ce service
service_id = f"{SERVICE_NAME}-{uuid.uuid4()}"


@app.on_event("startup")
def register_to_consul():
    registration = {
        "ID": service_id,
        "Name": SERVICE_NAME,
        "Address": SERVICE_HOST,
        "Port": SERVICE_PORT,
        "Check": {
            "HTTP": f"http://{SERVICE_HOST}:{SERVICE_PORT}/health",
            "Interval": "10s",
            "Timeout": "5s"
        }
    }
    url = f"http://{CONSUL_HOST}:{CONSUL_PORT}/v1/agent/service/register"
    try:
        res = requests.put(url, json=registration)
        if res.status_code == 200:
            print("✅ Service enregistré dans Consul avec succès")
        else:
            print(f"❌ Échec de l'enregistrement ({res.status_code}): {res.text}")
    except Exception as e:
        print(f"❌ Erreur de connexion à Consul: {e}")


@app.on_event("shutdown")
def deregister_from_consul():
    url = f"http://{CONSUL_HOST}:{CONSUL_PORT}/v1/agent/service/deregister/{service_id}"
    try:
        res = requests.put(url)
        if res.status_code == 200:
            print(" Service supprimé de Consul")
        else:
            print(f"❌ Échec de la suppression ({res.status_code}): {res.text}")
    except Exception as e:
        print(f"❌ Erreur de suppression de Consul: {e}")


@app.get("/health")
def health_check():
    return {"status": "UP"}