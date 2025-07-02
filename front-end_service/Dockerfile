# Étape 1 : Build
FROM node:22.11.0-alpine AS builder
WORKDIR /app

# Installer les dépendances système nécessaires pour le build
RUN apk add --no-cache \
    openssl \
    ca-certificates \
    python3 \
    make \
    g++

# Copier les fichiers de configuration
COPY package.json package-lock.json ./
COPY tsconfig.json ./
COPY next.config.ts ./

# Installer les dépendances
RUN npm ci

# Copier le reste des fichiers (respecte .dockerignore)
COPY . .

# Construire l'application
RUN npm run build

# Étape 2 : Runtime
FROM node:22.11.0-alpine AS runner
WORKDIR /app

# Installer les dépendances système minimales pour l'exécution
RUN apk add --no-cache \
    openssl \
    ca-certificates

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S nextgroup && adduser -S -G nextgroup nextuser
USER nextuser

# Copier les fichiers nécessaires depuis l'étape de build avec les destinations correctes
COPY --from=builder --chown=nextuser:nextgroup /app/.next/ ./.next/
COPY --from=builder --chown=nextuser:nextgroup /app/node_modules/ ./node_modules/
COPY --from=builder --chown=nextuser:nextgroup /app/package.json ./package.json
COPY --from=builder --chown=nextuser:nextgroup /app/public/ ./public/

# Exposer le port
EXPOSE 3000

# Définir les variables d'environnement
ENV NODE_ENV=production
ENV PORT=3000

# Lancer l'application
CMD ["npm", "start"]
