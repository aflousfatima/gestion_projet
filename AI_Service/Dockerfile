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

# Mettre à jour pip pour éviter les problèmes de dépendances
RUN pip install --no-cache-dir --upgrade pip

# Copier et installer les dépendances Python depuis PyPI
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt > /tmp/pip_install_log.txt 2>&1 && \
    ls -la /usr/local/lib/python3.12/site-packages || { echo "Error: site-packages directory not found"; cat /tmp/pip_install_log.txt; exit 1; }

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
