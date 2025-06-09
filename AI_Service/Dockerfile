# ----------- Stage 1: Build -------------
FROM python:3.12-slim AS builder

# Configurer l'environnement pour éviter les invites interactives
ENV DEBIAN_FRONTEND=noninteractive \
    PIP_NO_CACHE_DIR=1

# Installer les dépendances système nécessaires pour compiler les packages
RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    build-essential \
    libopenblas-dev \
    libgomp1 \
    python3-dev \
    g++ \
    gcc \
    libatlas-base-dev && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Définir le répertoire de travail
WORKDIR /app

# Copier le fichier requirements.txt
COPY requirements-ci.txt .

# Installer les dépendances Python avec versions précises
RUN pip install --upgrade pip && \
    pip install --no-cache-dir -r requirements-ci.txt && \
    rm -rf /root/.cache/pip /tmp/* /var/tmp/*

# ----------- Stage 2: Final Image -------------
FROM python:3.12-alpine

# Configurer l'environnement pour éviter les invites interactives
ENV DEBIAN_FRONTEND=noninteractive

# Installer les dépendances système minimales pour l'exécution
RUN apk add --no-cache \
    libgomp \
    openblas && \
    rm -rf /var/cache/apk/* /tmp/* /var/tmp/*

# Définir le répertoire de travail
WORKDIR /app

# Copier les packages Python installés depuis l'étape builder
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin/uvicorn /usr/local/bin/uvicorn

# Copier uniquement les fichiers nécessaires pour l'exécution
COPY app/main.py app/
COPY app/api/ app/api/
COPY app/core/ app/core/
COPY app/models/ app/models/
COPY app/data/intents_complete.json app/data/
COPY app/chatbot app/chatbot
COPY .env .env

# Exposer le port utilisé par FastAPI
EXPOSE 8000

# Commande pour démarrer l'application FastAPI avec uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
