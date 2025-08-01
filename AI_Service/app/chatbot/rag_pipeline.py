import json
import os
import numpy as np
import faiss
from sentence_transformers import SentenceTransformer, util
import pickle
import time
from langchain_community.vectorstores import FAISS as LangChainFAISS
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_core.documents import Document
import logging
import requests
from app.core.config import settings
from collections import defaultdict
import re
from langdetect import detect
import nltk
from functools import lru_cache
from app.api.v1.schemas import TaskInput, PredictionResponse
from typing import Dict, Any, Optional

nltk.download('punkt')
nltk.download('punkt_tab')
nltk.download('wordnet')

logger = logging.getLogger(__name__)

# Initialize caches
embedding_cache = {}
response_cache = {}
conversation_context = {'last_intent': None, 'last_parameters': {}, 'confidence': 0.0}

# File paths
faiss_index_path = settings.FAISS_INDEX_PATH
metadata_path = settings.METADATA_PATH
cache_file = settings.CACHE_PATH
json_path = settings.INTENTS_PATH

# Load FAISS index and metadata
try:
    index = faiss.read_index(faiss_index_path)
    with open(metadata_path, 'rb') as f:
        metadata = pickle.load(f)
    logger.info(f"Loaded FAISS index with {index.ntotal} vectors and {len(metadata)} metadata entries.")
except Exception as e:
    logger.error(f"Failed to load FAISS index: {str(e)}")
    raise

# Load intents JSON
try:
    with open(json_path, 'r') as f:
        intents_data = json.load(f)
    logger.info(f"Available intents: {list(intents_data['intents'].keys())}")
except Exception as e:
    logger.error(f"Failed to load JSON: {str(e)}")
    raise

# Initialize embedder
embedder = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')

# Initialize LangChain FAISS
documents = [Document(page_content=" ".join(meta.get('mapped_questions', [])), metadata=meta) for meta in metadata]
vectorstore = LangChainFAISS.from_documents(documents, HuggingFaceEmbeddings(model_name='paraphrase-multilingual-mpnet-base-v2'))

@lru_cache(maxsize=1000)
def fetch_task_details_cached(title: str, token: str) -> Dict[str, Any]:
    params = {'title': title}
    headers = {'Authorization': token}
    logger.debug(f"Fetching task details from cache for title: {title}")
    try:
        response = requests.get(f"{settings.TASK_API_URL}/details", params=params, headers=headers, timeout=5.0)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        logger.error(f"Failed to fetch cached task details for title {title}: {str(e)}")
        raise

def fetch_api_response(endpoint: str, parameters: Dict[str, Any], token: Optional[str] = None) -> Dict[str, Any]:
    try:
        headers = {'Authorization': token} if token else {}
        params = {}
        api_data = {}

        if endpoint == '/api/tasks/late':
            response = requests.get(f"{settings.TASK_API_URL}/late", headers=headers, timeout=5)
            response.raise_for_status()
            tasks = response.json()
            api_data = {
                'tasks': [
                    {
                        'taskId': str(task.get('id')),
                        'taskTitle': task.get('title', 'Task'),
                        'dueDate': task.get('dueDate', 'Unknown')
                    } for task in tasks
                ]
            }

        elif endpoint == '/api/projects/sprint/summary':
            project_name = parameters.get('title')
            if not project_name:
                raise ValueError("projectName is required")
            params['projectName'] = project_name
            response = requests.get(f"{settings.PROJECT_API_URL}/sprint/summary", params=params, headers=headers, timeout=5)
            response.raise_for_status()
            summary = response.json()
            api_data = {
                'sprintName': summary.get('sprintName', f'Sprint for {project_name}'),
                'progress': summary.get('progress', 0),
                'totalTasks': summary.get('totalTasks', 0),
                'completedTasks': summary.get('completedTasks', 0),
                'endDate': summary.get('endDate', 'Unknown')
            }

        elif endpoint == '/api/tasks/assigned':
            first_name = parameters.get('firstName')
            last_name = parameters.get('lastName')
            if not first_name or not last_name:
                raise ValueError("firstName and lastName are required")
            params = {'firstName': first_name, 'lastName': last_name}
            response = requests.get(f"{settings.TASK_API_URL}/assigned/by-name", params=params, headers=headers, timeout=5)
            response.raise_for_status()
            tasks = response.json()
            api_data = {
                'tasks': [
                    {
                        'taskId': str(task.get('id')),
                        'taskTitle': task.get('title', 'Task'),
                        'dueDate': task.get('dueDate', 'Unknown')
                    } for task in tasks
                ]
            }

        elif endpoint == '/api/tasks/status':
            title = parameters.get('title', 'Unknown')
            response = requests.get(f"{settings.TASK_API_URL}/status", params={'title': title}, headers=headers, timeout=5)
            response.raise_for_status()
            statuses = response.json()
            api_data = {
                'todoCount': next((s.get('count') for s in statuses if s.get('status') == 'TO_DO'), 0),
                'inProgressCount': next((s.get('count') for s in statuses if s.get('status') == 'IN_PROGRESS'), 0),
                'doneCount': next((s.get('count') for s in statuses if s.get('status') == 'DONE'), 0)
            }

        elif endpoint == '/api/tasks/predict':
            title = parameters.get('title')
            if not title:
                raise ValueError("title is required")
            task_details = fetch_task_details_cached(title, token)
            task_input = TaskInput(**task_details)
            from fastapi.testclient import TestClient
            from app.main import app
            client = TestClient(app)
            response = client.post("/api/v1/predict", json=task_input.model_dump())
            response.raise_for_status()
            prediction = response.json()
            api_data = {
                'title': title,
                'estimatedDays': float(prediction.get('predicted_duration', 0) / 24),
                'estimatedHours': float(prediction.get('predicted_duration', 0))
            }

        elif endpoint == '/api/tasks/priority':
            priority = parameters.get('priority', 'HIGH').upper()
            response = requests.get(f"{settings.TASK_API_URL}/priority", params={'priority': priority}, headers=headers, timeout=5)
            response.raise_for_status()
            tasks = response.json()
            api_data = {
                'tasks': [
                    {
                        'taskId': str(task.get('id')),
                        'taskTitle': task.get('title', 'Task'),
                        'priority': task.get('priority', priority),
                        'dueDate': task.get('dueDate', 'Unknown'),
                        'assignedUsers': task.get('assignedUsers', [])
                    } for task in tasks
                ]
            }

        elif endpoint == '/api/projects/sprint/user-stories':
            project_name = parameters.get('title')
            if not project_name:
                raise ValueError("projectName is required")
            params['projectName'] = project_name
            response = requests.get(f"{settings.PROJECT_API_URL}/sprint/user-stories", params=params, headers=headers, timeout=5)
            response.raise_for_status()
            stories = response.json()
            api_data = {
                'stories': [
                    {
                        'storyId': str(story.get('id')),
                        'storyTitle': story.get('title', 'Story')
                    } for story in stories
                ]
            }

        elif endpoint == '/api/projects/remaining-time':
            project_name = parameters.get('title')
            if not project_name:
                raise ValueError("projectName is required")
            params['projectName'] = project_name
            response = requests.get(f"{settings.PROJECT_API_URL}/remaining-time", params=params, headers=headers, timeout=5)
            response.raise_for_status()
            remaining = response.json()
            api_data = {
                'remainingDays': remaining.get('remainingDays', 0),
                'endDate': remaining.get('endDate', 'Unknown')
            }

        elif endpoint == '/api/tasks/blocked':
            response = requests.get(f"{settings.TASK_API_URL}/blocked", headers=headers, timeout=5)
            response.raise_for_status()
            tasks = response.json()
            api_data = {
                'tasks': [
                    {
                        'taskId': str(task.get('id')),
                        'taskTitle': task.get('title', 'Task'),
                        'dueDate': task.get('dueDate', 'Unknown'),
                        'priority': task.get('priority', 'Unknown'),
                        'assignedUsers': task.get('assignedUsers', []),
                        'dependenciesId': task.get('dependenciesId', []),
                        'blockReason': task.get('blockReason', 'Blocked due to dependencies or other issues')
                    } for task in tasks
                ]
            }

        logger.info(f"Fetched API response for endpoint {endpoint}: {api_data}")
        return api_data

    except requests.exceptions.RequestException as e:
        logger.error(f"API call failed for endpoint {endpoint}: {str(e)}")
        return {}
    except Exception as e:
        logger.error(f"Unexpected error in fetch_api_response for endpoint {endpoint}: {str(e)}")
        raise

def detect_language(query: str) -> str:
    try:
        lang = detect(query)
        logger.info(f"Detected language for query '{query}': {lang}")
        return lang
    except:
        logger.warning(f"Language detection failed for query '{query}', defaulting to 'en'")
        return 'en'

def preprocess_query(query: str) -> str:
    query = re.sub(r'#\d+', 'TaskId', query)
    return query.lower().strip()

def extract_parameters(query: str, intent: str, user_id: Optional[str] = None) -> Dict[str, Any]:
    parameters = {}
    query = query.lower()

    if intent == 'intent_3':
        if 'high' in query:
            parameters['priority'] = 'High'
        elif 'medium' in query:
            parameters['priority'] = 'Medium'
        elif 'low' in query:
            parameters['priority'] = 'Low'
        else:
            parameters['priority'] = 'High'

    if intent in ['intent_1', 'intent_4', 'intent_5', 'intent_8', 'intent_9']:
        project_match = re.search(r'project\s+([a-zA-Z0-9\s\-]+)', query, re.IGNORECASE)
        if project_match:
            parameters['title'] = project_match.group(1).strip()
        else:
            parameters['title'] = 'Unknown'

    if intent == 'intent_7':
        name_match = re.search(r'for\s+([a-zA-Z\s]+)', query)
        if name_match:
            full_name = name_match.group(1).strip().split()
            parameters['firstName'] = full_name[0]
            parameters['lastName'] = ' '.join(full_name[1:]) if len(full_name) > 1 else ''

    if intent == 'intent_9':
        title_match = re.search(r'task\s+([a-zA-Z0-9\s\-]+)', query, re.IGNORECASE)
        if title_match:
            parameters['title'] = title_match.group(1).strip()
        else:
            parameters['title'] = 'Unknown'

    return parameters

def similarity_search(query: str, k: int = 3, threshold: float = 0.25):
    start_time = time.perf_counter()
    lang = detect_language(query)
    query_clean = preprocess_query(query)
    
    if query_clean in embedding_cache:
        query_embedding = embedding_cache[query_clean]
    else:
        query_embedding = embedder.encode([query_clean], normalize_embeddings=True)[0]
        embedding_cache[query_clean] = query_embedding

    distances, indices = index.search(np.array([query_embedding], dtype=np.float32), k)
    distances = distances[0]
    indices = indices[0]

    filtered_results = []
    for dist, idx in zip(distances, indices):
        if dist > threshold:
            continue
        for meta in metadata:
            if idx in meta.get('embedding_indices', []):
                filtered_results.append((meta, dist))
                break

    intent = keyword_fallback(query_clean, lang)
    if intent != 'out_of_scope' and (not filtered_results or min(distances) > 0.3):
        for meta in metadata:
            if meta['intent'] == intent:
                filtered_results.append((meta, min(distances) if filtered_results else 0.3))
                break

    if not filtered_results:
        search_time = time.perf_counter() - start_time
        logger.warning(f"No results for query '{query}', returning out_of_scope")
        return [{'intent': 'out_of_scope', 'response': 'Sorry, I can only answer questions about tasks and projects.'}], [0.0] * k, search_time

    intent_scores = defaultdict(list)
    for meta, score in filtered_results:
        intent = meta['intent']
        adjusted_score = score
        if intent == 'intent_6' and any(keyword in query_clean for keyword in ['blocked', 'stuck', 'obstacle', 'bloqué']):
            adjusted_score *= 0.3
        elif intent == 'intent_7' and any(keyword in query_clean for keyword in ['assigned to', 'tasks for', 'tâches pour']):
            adjusted_score *= 0.3
        elif intent == 'intent_9' and any(keyword in query_clean for keyword in ['duration', 'estimate', 'time', 'durée']):
            adjusted_score *= 0.2
        intent_scores[intent].append(adjusted_score)

    intent_avg_scores = {intent: min(scores) for intent, scores in intent_scores.items()}
    best_intent = min(intent_avg_scores, key=intent_avg_scores.get)
    best_results = [meta for meta, score in filtered_results if meta['intent'] == best_intent]
    best_scores = [score for meta, score in filtered_results if meta['intent'] == best_intent]
    search_time = time.perf_counter() - start_time

    confidence = 1.0 - min(best_scores) if best_scores else 0.0
    conversation_context['confidence'] = confidence
    logger.info(f"Query '{query}' matched intent '{best_intent}' with confidence {confidence:.2f}")
    
    return best_results[:k], best_scores[:k], search_time

def keyword_fallback(query: str, lang: str = 'en') -> str:
    keywords = {
        'intent_1': ['sprint summary', 'sprint progress', 'current sprint', 'résumé sprint'],
        'intent_2': ['overdue tasks', 'late tasks', 'delays', 'past due', 'tâches en retard'],
        'intent_3': ['priority tasks', 'urgent tasks', 'high priority', 'low priority', 'tâches prioritaires'],
        'intent_4': ['user stories', 'sprint stories', 'active stories', 'histoires utilisateur'],
        'intent_5': ['remaining time', 'time left', 'project completion', 'end date', 'temps restant', 'temps qu\'il reste', 'date de fin'],
        'intent_6': ['blocked tasks', 'stuck tasks', 'obstacles', 'dependencies', 'tâches bloquées'],
        'intent_7': ['assigned to', 'tasks for', 'user tasks', 'tâches pour', 'attribué à'],
        'intent_8': ['task status', 'status distribution', 'task progress', 'état des tâches'],
        'intent_9': ['task duration', 'estimated time', 'time estimate', 'duration of task', 'durée tâche']
    }
    for intent, kws in keywords.items():
        if any(kw in query for kw in kws):
            return intent
    return 'out_of_scope'

def generate_response(query: str, metadata: list, user_id: Optional[str] = None, use_cache: bool = True, token: Optional[str] = None):
    start_time = time.perf_counter()
    cache_key = f"response:{user_id}:{query.lower()}"
    
    if use_cache and cache_key in response_cache:
        cached = response_cache[cache_key]
        if time.time() - cached.get('timestamp', 0) < 30:
            logger.info(f"Cache hit for query '{query}'")
            return cached['response'], 0.0, 0.0

    if not metadata or metadata[0]['intent'] == 'out_of_scope':
        response = 'Sorry, I can only answer questions about tasks and projects.'
        response_time = time.perf_counter() - start_time
        response_cache[cache_key] = {'response': response, 'intent': 'out_of_scope', 'parameters': {}, 'timestamp': time.time()}
        try:
            with open(cache_file, 'w') as f:
                json.dump(response_cache, f)
        except Exception as e:
            logger.error(f"Failed to write cache file: {str(e)}")
        return response, response_time, 0.0

    intent = metadata[0]['intent']
    param_start = time.perf_counter()
    parameters = extract_parameters(query, intent, user_id=user_id)
    param_time = time.perf_counter() - param_start
    endpoint = metadata[0].get('endpoint', '')
    api_data = fetch_api_response(endpoint, parameters, token=token)

    if not api_data or ('tasks' in api_data and not api_data['tasks']) or ('stories' in api_data and not api_data['stories']):
        empty_responses = {
            'intent_2': "No overdue tasks found.",
            'intent_3': f"No {parameters.get('priority', 'High')} priority tasks found.",
            'intent_4': "No user stories found for the sprint.",
            'intent_6': "No blocked tasks found.",
            'intent_7': f"No tasks assigned to {parameters.get('firstName', 'user')} {parameters.get('lastName', '')}.",
            'intent_1': "No sprint summary available.",
            'intent_5': f"No remaining time data for project {parameters.get('title', 'unknown')}.",
            'intent_8': f"No task status data for project {parameters.get('title', 'unknown')}.",
            'intent_9': f"No duration estimate for task {parameters.get('title', 'unknown')}."
        }
        response = empty_responses.get(intent, "No data available.")
        response_time = time.perf_counter() - start_time
        response_cache[cache_key] = {'response': response, 'intent': intent, 'parameters': parameters, 'timestamp': time.time()}
        try:
            with open(cache_file, 'w') as f:
                json.dump(response_cache, f)
        except Exception as e:
            logger.error(f"Failed to write cache file: {str(e)}")
        return response, response_time, param_time

    templates = {
        'intent_1': [
            "Sprint {sprintName} is at {progress}% and ends on {endDate}.",
            "Summary: sprint {sprintName} at {progress}% progress, due {endDate}.",
            "{progress}% of sprint {sprintName} completed, deadline {endDate}."
        ],
        'intent_2': [
            "Overdue tasks: {task_list}.",
            "Delays: {task_list}.",
            "The following tasks are overdue: {task_list}."
        ],
        'intent_3': [
            "{priority} priority tasks: {task_list}.",
            "Priority {priority}: {task_list}.",
            "Tasks with {priority} priority: {task_list}."
        ],
        'intent_4': [
            "User stories: {story_list}.",
            "Sprint stories: {story_list}.",
            "Active user stories: {story_list}."
        ],
        'intent_5': [
            "Project {title}: {remainingDays} days remaining, ends {endDate}.",
            "Remaining time for {title}: {remainingDays} days ({endDate}).",
            "{title} set to finish in {remainingDays} days ({endDate})."
        ],
        'intent_6': [
            "Blocked tasks: {task_list}.",
            "Tasks blocked: {task_list}.",
            "Blocked: {task_list}."
        ],
        'intent_7': [
            "Tasks assigned to {firstName} {lastName}: {task_list}.",
            "{firstName} {lastName} has tasks: {task_list}.",
            "{firstName} {lastName} is responsible for: {task_list}."
        ],
        'intent_8': [
            "Project {title}: {todoCount} TODO, {inProgressCount} in progress, {doneCount} done.",
            "Statuses for {title}: {todoCount} to do, {inProgressCount} in progress, {doneCount} completed.",
            "{title}: {todoCount} TODO tasks, {inProgressCount} in progress, {doneCount} finished."
        ],
        'intent_9': [
            "Task {title} estimated at {estimatedDays:.2f} days ({estimatedHours:.2f} hours).",
            "Estimated duration for {title}: {estimatedDays:.2f} days.",
            "Task {title}: {estimatedHours:.2f} hours estimated."
        ]
    }

    query_len = len(query.split())
    template_idx = 0 if query_len < 5 else 1 if query_len < 10 else 2
    available_templates = templates.get(intent, ['No data available.'])
    response = available_templates[min(template_idx, len(available_templates)-1)]

    if intent == 'intent_9' and api_data:
        response = response.format(
            title=parameters.get('title', 'Unknown'),
            estimatedDays=api_data.get('estimatedDays', 0),
            estimatedHours=api_data.get('estimatedHours', 0)
        )
    else:
        for key, value in parameters.items():
            response = response.replace(f"{{{key}}}", str(value))

    if intent in ['intent_2', 'intent_3', 'intent_6', 'intent_7'] and 'tasks' in api_data and api_data['tasks']:
        task_list = [f"#{task.get('taskId', 'Unknown')} - {task.get('taskTitle', 'Task')} (due {task.get('dueDate', 'Unknown')})" for task in api_data['tasks']]
        response = response.replace('{task_list}', ', '.join(task_list))
    elif intent == 'intent_4' and 'stories' in api_data and api_data['stories']:
        story_list = [f"#{story.get('storyId', 'Unknown')} - {story.get('storyTitle', 'Story')}" for story in api_data['stories']]
        response = response.replace('{story_list}', ', '.join(story_list))
    else:
        for key in ['sprintName', 'progress', 'endDate', 'todoCount', 'inProgressCount', 'doneCount', 'remainingDays', 'estimatedDays', 'estimatedHours']:
            response = response.replace(f"{{{key}}}", str(api_data.get(key, 'Unknown')))

    response_time = time.perf_counter() - start_time
    response_cache[cache_key] = {'response': response, 'intent': intent, 'parameters': parameters, 'timestamp': time.time()}
    try:
        with open(cache_file, 'w') as f:
            json.dump(response_cache, f)
    except Exception as e:
        logger.error(f"Failed to write cache file: {str(e)}")
    return response, response_time, param_time

def rag_pipeline(query: str, user_id: Optional[str] = None, use_cache: bool = True, token: Optional[str] = None):
    try:
        metadata_results, distances, search_time = similarity_search(query, k=3, threshold=0.2)
        if not metadata_results:
            return 'out_of_scope', "No matching intent found.", [float('inf')] * 3, 0.0, [], {}, 0.0, 0.0
        
        intent = metadata_results[0]['intent']
        response, response_time, param_time = generate_response(query, metadata_results, user_id=user_id, use_cache=use_cache, token=token)
        parameters = extract_parameters(query, intent, user_id=user_id)
        
        return intent, response, distances, response_time, metadata_results, parameters, search_time, param_time
    except Exception as e:
        logger.error(f"Error in rag_pipeline for query '{query}': {str(e)}")
        return 'out_of_scope', "Error processing query.", [float('inf')] * 3, 0.0, [], {}, 0.0, 0.0