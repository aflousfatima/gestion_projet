# ----------- Stage 1: Build -------------
FROM python:3.12-slim AS builder

# Configurer l'environnement pour éviter les invites interactives
ENV DEBIAN_FRONTEND=noninteractive

# Installer les dépendances système nécessaires pour compiler les packages Python
RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    build-essential \
    libopenblas-dev \
    libgomp1 \
    python3-dev \
    g++ \
    gcc \
    libatlas-base-dev \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Définir le répertoire de travail
WORKDIR /app

RUN pip install --upgrade pip && \
    pip install fastapi==0.115.12 uvicorn==0.34.3 pydantic==2.11.5 pandas==2.1.4 numpy==1.26.4 scikit-learn==1.7.0 scipy==1.15.3 joblib==1.5.1 python-dotenv==1.1.0 httpx==0.27.2 sentence-transformers==2.7.0 faiss-cpu==1.8.0 langchain==0.3.0 langchain-community==0.3.0 langchain-huggingface==0.2.0 nltk==3.9.1 langdetect==1.0.9 redis==5.2.0 websockets==12.0 && \
    rm -rf /root/.cache/pip /tmp/*
    
# ----------- Stage 2: Final Image -------------
FROM python:3.12-slim

# Configurer l'environnement pour éviter les invites interactives
ENV DEBIAN_FRONTEND=noninteractive

# Installer les dépendances système nécessaires pour l'exécution
RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    libopenblas-dev \
    libgomp1 \
    libatlas-base-dev \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Définir le répertoire de travail
WORKDIR /app

# Copier les packages Python installés depuis l'étape builder
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin/uvicorn /usr/local/bin/uvicorn

# Copier le code applicatif et les fichiers de données (gérés par DVC)
COPY app/ app/

# Exposer le port utilisé par FastAPI
EXPOSE 8000

# Commande pour démarrer l'application FastAPI avec uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
