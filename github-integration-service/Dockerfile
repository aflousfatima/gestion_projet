# Utiliser une image de base avec Java
FROM openjdk:17-jdk-slim

# Définir le répertoire de travail
WORKDIR /app

# Copier le fichier JAR
COPY target/github-integration-service-0.0.1-SNAPSHOT.jar app.jar

# Exposer le port de l'application
EXPOSE 8087

# Commande pour lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]