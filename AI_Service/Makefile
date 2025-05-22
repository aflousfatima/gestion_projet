# Makefile for Task Predictor MLOps project
# Standardizes tasks: install dependencies, run tests, train model, start API

# Default target: display help
help:
	@echo "Available commands:"
	@echo "  make setup    : Install dependencies from requirements.txt and Google Drive libraries"
	@echo "  make test     : Run all tests (unit, performance, robustness)"
	@echo "  make train    : Train the model using src/train.py"
	@echo "  make run-api  : Start the FastAPI microservice"
	@echo "  make clean    : Remove temporary files and caches"

# Install dependencies
setup:
	pip install -r requirements/requirements.txt
	pip install google-auth google-api-python-client

# Run all tests with pytest
test:
	pytest tests/ -v --maxfail=1 --disable-warnings

# Train the model (assumes train.py exists)
train:
	python src/train.py

# Start FastAPI microservice
run-api:
	uvicorn api.v1.endpoints:app --host 0.0.0.0 --port 8000 --reload

# Clean temporary files
clean:
	rm -rf __pycache__ */__pycache__ */*/__pycache__ *.log .pytest_cache
