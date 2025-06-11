#!/bin/bash
SERVICE_PATH=$1
if [ -z "$SERVICE_PATH" ]; then
  echo "Error: Service path not provided"
  exit 1
fi
cd "$SERVICE_PATH" || exit 1
mvn clean package -DskipTests

