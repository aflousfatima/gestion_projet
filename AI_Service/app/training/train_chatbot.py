import json
import os
import numpy as np
import faiss
from sentence_transformers import SentenceTransformer, util
import pickle
import time
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.manifold import TSNE
from sklearn.metrics import confusion_matrix
import pandas as pd
from collections import defaultdict
from langchain_community.vectorstores import FAISS as LangChainFAISS
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_core.documents import Document
import logging
from dotenv import load_dotenv
import mlflow
import warnings
from nltk.translate.bleu_score import sentence_bleu, SmoothingFunction
from nltk.translate.meteor_score import meteor_score
from rouge_score import rouge_scorer
import nltk
import re
from langdetect import detect
import gc

# Configure NLTK
nltk.download('punkt')
nltk.download('punkt_tab')
nltk.download('wordnet')

# Suppress warnings
warnings.filterwarnings("ignore", category=FutureWarning, module="transformers")

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(message)s',
    filename='app/logs/chatbot_log.txt',
    filemode='a'
)
logger = logging.getLogger(__name__)

# Load environment variables
load_dotenv()
JSON_PATH = os.getenv("JSON_PATH", "app/data/intents_complete.json")
FAISS_INDEX_PATH = os.getenv("FAISS_INDEX_PATH", "app/models/faiss_index.bin")
METADATA_PATH = os.getenv("METADATA_PATH", "app/models/metadata.pkl")
PLOT_PATH = os.getenv("PLOT_PATH", "app/plots/")
CACHE_FILE = os.getenv("CACHE_FILE", "app/cache/cache.json")

# Create directories
os.makedirs(os.path.dirname(FAISS_INDEX_PATH), exist_ok=True)
os.makedirs(os.path.dirname(CACHE_FILE), exist_ok=True)
os.makedirs(PLOT_PATH, exist_ok=True)

# Load cache with expiration
def load_cache(file_path):
    if os.path.exists(file_path):
        with open(file_path, 'r') as f:
            cache = json.load(f)
        current_time = time.time()
        cache = {k: v for k, v in cache.items() if current_time - v.get('timestamp', 0) < 30}
        logger.info(f"Cache {file_path} loaded with {len(cache)} valid entries.")
    else:
        cache = {}
        logger.info(f"New cache created at {file_path}.")
    return cache

# Conversational context
conversation_context = {'last_intent': None, 'last_parameters': {}, 'confidence': 0.0}
response_cache = load_cache(CACHE_FILE)
embedding_cache = {}

# Load FAISS index and metadata
try:
    index = faiss.read_index(FAISS_INDEX_PATH)
    with open(METADATA_PATH, 'rb') as f:
        metadata = pickle.load(f)
    logger.info(f"Loaded FAISS index with {index.ntotal} vectors and {len(metadata)} metadata entries.")
    print(f"FAISS index loaded with {index.ntotal} vectors.")
    print(f"Number of metadata entries: {len(metadata)}")
except Exception as e:
    logger.error(f"Failed to load FAISS index: {str(e)}")
    print(f"Error loading FAISS index: {e}")
    raise


# Preprocess question
def preprocess_question(question):
    replacements = {
        '{userId}': 'Utilisateur',
        '{projectId}': 'Projet',
        '{taskId}': 'Tache',
        '{sprintId}': 'Sprint',
        '{storyId}': 'Story',
        '{priority}': 'Priorite'
    }
    for placeholder, generic in replacements.items():
        question = question.replace(placeholder, generic)
    return question

# Load intents
def load_intents(json_path):
    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            intents_data = json.load(f)
        logger.info(f"Intents found: {list(intents_data['intents'].keys())}")
        questions = []
        metadata = []
        for intent_key, intent_data in intents_data['intents'].items():
            intent_questions = intent_data.get('questions', []) + intent_data.get('questions_fr', [])
            if not intent_questions:
                logger.warning(f"No questions for intent {intent_key}")
                continue
            sample_responses = intent_data.get('sample_responses', ['No response available.'])
            for idx, q in enumerate(intent_questions):
                processed_question = preprocess_question(q)
                questions.append(processed_question)
                metadata.append({
                    'intent': intent_key,
                    'endpoint': intent_data.get('endpoint', ''),
                    'description': intent_data.get('description', ''),
                    'sample_responses': sample_responses,
                    'question': q,
                    'processed_question': processed_question,
                    'embedding_index': None
                })
        logger.info(f"Loaded {len(questions)} questions and {len(metadata)} metadata entries.")
        return questions, metadata
    except Exception as e:
        logger.error(f"Error loading JSON: {str(e)}")
        return [], []

# Generate embeddings
def generate_embeddings(questions, metadata, batch_size=16, similarity_threshold=0.8):
    try:
        embedder = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')
        embeddings = []
        for i in range(0, len(questions), batch_size):
            batch = questions[i:i + batch_size]
            batch_embeddings = embedder.encode(batch, normalize_embeddings=True, batch_size=batch_size)
            embeddings.extend(batch_embeddings)
        embeddings = np.array(embeddings, dtype=np.float32)
        
        unique_indices = []
        unique_embeddings = []
        for i in range(len(embeddings)):
            if not unique_indices:
                unique_indices.append(i)
                unique_embeddings.append(embeddings[i])
                metadata[i]['embedding_index'] = 0
            else:
                similarities = util.cos_sim(embeddings[i:i+1], np.array(unique_embeddings)).numpy()[0]
                if max(similarities) < similarity_threshold:
                    unique_indices.append(i)
                    unique_embeddings.append(embeddings[i])
                    metadata[i]['embedding_index'] = len(unique_indices) - 1
                else:
                    closest_idx = np.argmax(similarities)
                    metadata[i]['embedding_index'] = closest_idx
        
        logger.info(f"Generated {len(embeddings)} embeddings, deduplicated to {len(unique_embeddings)} vectors.")
        return np.array(unique_embeddings), metadata
    except Exception as e:
        logger.error(f"Error generating embeddings: {str(e)}")
        return np.array([]), []

# Create FAISS index
def create_faiss_index(embeddings, metadata, faiss_index_path, metadata_path):
    try:
        if embeddings.size == 0:
            logger.error("No embeddings provided")
            return None
        dimension = embeddings.shape[1]
        index = faiss.IndexFlatL2(dimension)
        index.add(embeddings)
        logger.info(f"Created FAISS index with {index.ntotal} vectors.")
        
        final_metadata = []
        seen_indices = set()
        for meta in metadata:
            idx = meta.get('embedding_index')
            if idx is not None and idx not in seen_indices:
                seen_indices.add(idx)
                final_metadata.append({
                    'intent': meta['intent'],
                    'endpoint': meta['endpoint'],
                    'description': meta['description'],
                    'sample_responses': meta['sample_responses'],
                    'mapped_questions': [meta['question']],
                    'embedding_indices': [idx]
                })
        
        faiss.write_index(index, faiss_index_path)
        with open(metadata_path, 'wb') as f:
            pickle.dump(final_metadata, f)
        logger.info(f"Saved FAISS index and metadata to {faiss_index_path} and {metadata_path}")
        return index
    except Exception as e:
        logger.error(f"Error creating FAISS index: {str(e)}")
        return None

# Inspect metadata
def inspect_metadata(metadata_path):
    try:
        with open(metadata_path, 'rb') as f:
            metadata = pickle.load(f)
        all_indices = set()
        for meta in metadata:
            indices = meta.get('embedding_indices', [])
            all_indices.update(indices)
        expected_indices = set(range(len(all_indices)))
        missing_indices = expected_indices - all_indices
        logger.info(f"Total unique indices: {len(all_indices)}, Missing indices: {missing_indices}")
        return all_indices, missing_indices
    except Exception as e:
        logger.error(f"Error inspecting metadata: {str(e)}")
        return set(), []

# Detect language
def detect_language(query):
    try:
        lang = detect(query)
        logger.info(f"Detected language for query '{query}': {lang}")
        return lang
    except:
        logger.warning(f"Language detection failed for query '{query}', defaulting to 'en'")
        return 'en'

# Preprocess query
def preprocess_query(query):
    query = re.sub(r'#\d+', 'TaskId', query)
    return query.lower().strip()

# Extract parameters
def extract_parameters(query, intent):
    parameters = {}
    query_lower = query.lower()

    if intent == 'intent_3':
        priority_match = re.search(r'(high|medium|low)\s*priority', query_lower)
        parameters['priority'] = priority_match.group(1).capitalize() if priority_match else 'High'

    if intent in ['intent_2', 'intent_3', 'intent_5', 'intent_6', 'intent_8']:
        project_match = re.search(r'(?:project|for)\s+([a-zA-Z0-9]+)', query_lower)
        if project_match:
            parameters['projectId'] = project_match.group(1).capitalize()

    if intent == 'intent_7':
        user_match = re.search(r'(?:tasks\s*(?:for\s+|of\s+|assigned\s+to\s+)|tâches\s*(?:pour\s+))([a-zA-Z]+)', query_lower)
        if user_match:
            parameters['userId'] = user_match.group(1).capitalize()

    if intent == 'intent_9':
        task_match = re.search(r'#?(\d+)', query_lower)
        if task_match:
            parameters['taskId'] = task_match.group(1)
        else:
            project_match = re.search(r'(?:project|for)\s+([a-zA-Z0-9]+)', query_lower)
            if project_match:
                parameters['projectId'] = project_match.group(1).capitalize()

    if 'and for' in query_lower and conversation_context['last_intent']:
        parameters.update(conversation_context['last_parameters'])
        project_match = re.search(r'project\s+([a-zA-Z0-9]+)', query_lower)
        if project_match:
            parameters['projectId'] = project_match.group(1).capitalize()

    logger.info(f"Extracted parameters for query '{query}' and intent '{intent}': {parameters}")
    return parameters

# Configure embedder
embedder = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')

# Configure LangChain FAISS
from langchain_core.documents import Document
documents = [Document(page_content=" ".join(meta.get('mapped_questions', [])), metadata=meta) for meta in metadata]
vectorstore = LangChainFAISS.from_documents(documents, HuggingFaceEmbeddings(model_name='paraphrase-multilingual-mpnet-base-v2'))


# Simulate API responses
def simulate_api_response(endpoint, parameters):
    api_data = {}
    userId = parameters.get('userId', 'User')
    projectId = parameters.get('projectId', 'Project')
    taskId = parameters.get('taskId', 'Unknown')
    priority = parameters.get('priority', 'High')
    
    if endpoint == '/api/tasks/late':
        api_data = {'tasks': [{'taskId': '123', 'taskTitle': f'Overdue Task for {projectId}', 'dueDate': '2025-05-20'}]}
    elif endpoint == '/api/projects/sprint/summary':
        api_data = {'sprintId': 'Sprint123', 'sprintName': f'Sprint for {projectId}', 'progress': 60, 'endDate': '2025-06-01'}
    elif endpoint == '/api/tasks/assigned':
        api_data = {'tasks': [{'taskId': '456', 'taskTitle': f'Task 1 for {userId}'}, {'taskId': '457', 'taskTitle': f'Task 2 for {userId}'}]}
    elif endpoint == '/api/tasks/status':
        api_data = {'todoCount': 5, 'inProgressCount': 3, 'doneCount': 2}
    elif endpoint == '/api/tasks/predict':
        api_data = {'taskId': taskId, 'estimatedDays': 3, 'estimatedHours': 24}
    elif endpoint == '/api/tasks/priority':
        api_data = {'tasks': [{'taskId': '789', 'taskTitle': f'Urgent Task for {projectId}', 'priority': priority}]}
    elif endpoint == '/api/projects/sprint/user-stories':
        api_data = {'stories': [{'storyId': 'Story789', 'storyTitle': f'Story for {projectId}'}]}
    elif endpoint == '/api/projects/remaining-time':
        api_data = {'remainingDays': 10, 'endDate': '2025-06-10'}
    elif endpoint == '/api/tasks/blocked':
        api_data = {'tasks': [{'taskId': '101', 'taskTitle': f'Blocked Task for {projectId}', 'blockReason': 'External Dependency'}]}
    logger.info(f"Simulated API response for endpoint {endpoint} with parameters {parameters}")
    return api_data

# Semantic search
def similarity_search(query, k=3, threshold=0.25):
    start_time = time.perf_counter()
    lang = detect_language(query)
    question_key = 'questions' if lang == 'en' else 'questions_fr'
    query_clean = preprocess_query(query)
    
    if query_clean in embedding_cache:
        query_embedding = embedding_cache[query_clean]
    else:
        query_embedding = embedder.encode([query_clean], normalize_embeddings=True)[0]
        embedding_cache[query_clean] = query_embedding

    index = faiss.read_index(FAISS_INDEX_PATH)
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

    # Compute confidence
    confidence = 1.0 - min(best_scores) if best_scores else 0.0
    conversation_context['confidence'] = confidence
    logger.info(f"Query '{query}' matched intent '{best_intent}' with confidence {confidence:.2f}")
    
    return best_results[:k], best_scores[:k], search_time

# Keyword fallback
def keyword_fallback(query, lang='en'):
    keywords = {
        'intent_1': ['sprint summary', 'sprint progress', 'current sprint', 'résumé sprint'],
        'intent_2': ['overdue tasks', 'late tasks', 'delays', 'past due', 'tâches en retard'],
        'intent_3': ['priority tasks', 'urgent tasks', 'high priority', 'low priority', 'tâches prioritaires'],
        'intent_4': ['user stories', 'sprint stories', 'active stories', 'histoires utilisateur'],
        'intent_5': ['remaining time', 'project completion', 'end date', 'temps restant'],
        'intent_6': ['blocked tasks', 'stuck tasks', 'obstacles', 'dependencies', 'tâches bloquées'],
        'intent_7': ['assigned to', 'tasks for', 'user tasks', 'tâches pour', 'attribué à'],
        'intent_8': ['task status', 'status distribution', 'task progress', 'état des tâches'],
        'intent_9': ['task duration', 'estimated time', 'time estimate', 'duration of task', 'durée tâche', 'durée projet']
    }
    for intent, kws in keywords.items():
        if any(kw in query for kw in kws):
            return intent
    return 'out_of_scope'

# Generate response
def generate_response(query, metadata, use_cache=True):
    start_time = time.perf_counter()
    cache_key = query.lower()
    
    if use_cache and cache_key in response_cache:
        cached = response_cache[cache_key]
        if time.time() - cached.get('timestamp', 0) < 30:
            logger.info(f"Cache hit for query '{query}'")
            return cached['response'], 0.0, 0.0

    if not metadata or metadata[0]['intent'] == 'out_of_scope':
        response = 'Sorry, I can only answer questions about tasks and projects.'
        response_time = time.perf_counter() - start_time
        response_cache[cache_key] = {'response': response, 'intent': 'out_of_scope', 'parameters': {}, 'timestamp': time.time()}
        with open(CACHE_FILE, 'w') as f:
            json.dump(response_cache, f)
        return response, response_time, 0.0

    intent = metadata[0]['intent']
    param_start = time.perf_counter()
    parameters = extract_parameters(query, intent)
    param_time = time.perf_counter() - param_start
    endpoint = metadata[0].get('endpoint', '')
    api_data = simulate_api_response(endpoint, parameters)

    templates = {
        'intent_1': [
            "Sprint {sprintName} is at {progress}% and ends on {endDate}.",
            "Summary: sprint {sprintId} at {progress}% progress, due {endDate}.",
            "{progress}% of sprint {sprintName} completed, deadline {endDate}."
        ],
        'intent_2': [
            "Overdue tasks: #{taskId} - {taskTitle}, due {dueDate}.",
            "Delays: {taskTitle}, ({taskId}) due {deadlineDate}.",
            "Task #{taskId} ({taskTitle}({ overdue until {dueDate}})"
        ],
        'intent_3': [
            "{priority} priority tasks: #{taskId} - {taskTitle}.",
            "Priority {priority}: {taskTitle}, (#{taskId}).",
            "#{taskId} - {taskTitle} has {priority} priority."
        ],
        'intent_4': [
            "User stories: #{storyId} - {storyTitle}.",
            "Sprint stories: {storyId} ({storyTitle}).",
            "Story #{storyId}: {storyTitle} in progress."
        ],
        'intent_5': [
            "Project {projectId}: {remainingDays} days remaining, ends {endDate}.",
            "Remaining time for {projectId}: {remainingDays} days ({endDate}).",
            "{projectId} set to finish in {remainingDays} days ({endDate})."
        ],
        'intent_6': [
            "Blocked tasks: #{taskId} - {taskTitle}, reason: {blockReason}.",
            "{taskTitle} (#{taskId}) blocked by {blockReason}.",
            "Blockage: {taskId} ({taskTitle}), cause: {blockReason}."
        ],
        'intent_7': [
            "{userId} has tasks: #{taskId_1} - {taskTitle_1}, #{taskId_2} - {taskTitle_2}.",
            "Tasks assigned to {userId}: #{taskId_1} ({taskTitle_1}), #{taskId_2} ({taskTitle_2}).",
            "{userId} is responsible for: {taskTitle_1} (#{taskId_1}), {taskTitle_2} (#{taskId_2})."
        ],
        'intent_8': [
            "Project {projectId}: {todoCount} TODO, {inProgressCount} in progress, {doneCount} done.",
            "Statuses for {projectId}: {todoCount} to do, {inProgressCount} in progress, {doneCount} completed.",
            "{projectId}: {todoCount} TODO tasks, {inProgressCount} in progress, {doneCount} finished."
        ],
        'intent_9': [
            "Task #{taskId} estimated at {estimatedDays} days ({estimatedHours} hours).",
            "Estimated duration for #{taskId}: {estimatedDays} days.",
            "Task #{taskId}: {estimatedHours} hours estimated."
        ],
        'out_of_scope': [
            "Sorry, I can only answer questions about tasks and projects."
        ]
    }

    query_len = len(query.split())
    template_idx = 0 if query_len < 5 else 1 if query_len < 10 else 2
    available_templates = templates.get(intent, ['No data available.'])
    response = available_templates[min(template_idx, len(available_templates)-1)]

    if intent == 'intent_9' and 'taskId' not in parameters and 'projectId' in parameters:
        response = f"Project {parameters['projectId']} average task duration: 3 days (24 hours estimated)."

    for key, value in parameters.items():
        response = response.replace(f"{{{key}}}", str(value))

    if 'tasks' in api_data and api_data['tasks']:
        task = api_data['tasks'][0]
        response = response.replace('{taskId}', str(task.get('taskId', 'Unknown')))
        response = response.replace('{taskTitle}', task.get('taskTitle', 'Task'))
        response = response.replace('{taskId_1}', str(task.get('taskId', '0')))
        response = response.replace('{taskTitle_1}', task.get('taskTitle', 'Task1'))
        response = response.replace('{dueDate}', task.get('dueDate', 'Unknown'))
        response = response.replace('{blockReason}', task.get('blockReason', 'Unknown'))
        response = response.replace('{priority}', task.get('priority', parameters.get('priority', 'High')))
        if len(api_data['tasks']) > 1:
            task2 = api_data['tasks'][1]
            response = response.replace('{taskId_2}', str(task2.get('taskId', '1')))
            response = response.replace('{taskTitle_2}', task2.get('taskTitle', 'Task2'))
        else:
            response = response.replace('{taskId_2}', 'None')
            response = response.replace('{taskTitle_2}', 'None')
    if 'stories' in api_data and api_data['stories']:
        story = api_data['stories'][0]
        response = response.replace('{storyId}', str(story.get('storyId', 'Unknown')))
        response = response.replace('{storyTitle}', story.get('storyTitle', 'Story'))
    for key in ['sprintId', 'sprintName', 'progress', 'endDate', 'todoCount', 'inProgressCount', 'doneCount', 'remainingDays', 'estimatedDays', 'estimatedHours']:
        response = response.replace(f"{{{key}}}", str(api_data.get(key, 'Unknown')))

    response_time = time.perf_counter() - start_time
    response_cache[cache_key] = {'response': response, 'intent': intent, 'parameters': parameters, 'timestamp': time.time()}
    with open(CACHE_FILE, 'w') as f:
        json.dump(response_cache, f)
    conversation_context['last_intent'] = intent
    conversation_context['last_parameters'] = parameters
    logger.info(f"Generated response for query '{query}': {response}")
    return response, response_time, param_time

# RAG pipeline
def rag_pipeline(query, use_cache=True):
    try:
        metadata_results, distances, search_time = similarity_search(query, k=3, threshold=0.25)
        if not metadata_results:
            return 'out_of_scope', "No matching intent found.", [float('inf')] * 3, 0.0, [], {}, 0.0, 0.0, 0.0
        
        intent = metadata_results[0]['intent']
        response, response_time, param_time = generate_response(query, metadata_results, use_cache=use_cache)
        
        return intent, response, distances, response_time, metadata_results, extract_parameters(query, intent), search_time, param_time, conversation_context['confidence']
    except Exception as e:
        logger.error(f"Error in rag_pipeline for query '{query}': {str(e)}")
        return 'out_of_scope', "Error processing query.", [float('inf')] * 3, 0.0, [], {}, 0.0, 0.0, 0.0
    
# Visualizations
def plot_distance_histogram(distances, plot_path):
    plt.figure(figsize=(8, 6), dpi=300)
    plt.hist(distances, bins=20, color='#36A2EB', edgecolor='black')
    plt.title("Distribution des distances L2")
    plt.xlabel("Distance L2")
    plt.ylabel("Fréquence")
    plt.savefig(os.path.join(plot_path, 'distance_histogram.png'))
    mlflow.log_artifact(os.path.join(plot_path, 'distance_histogram.png'))
    plt.close()
    logger.info(f"Histogram saved to {os.path.join(plot_path, 'distance_histogram.png')}")

def plot_tsne_embeddings(embeddings, metadata, plot_path):
    if len(embeddings) < 5:
        logger.error("Not enough embeddings for t-SNE")
        return
    tsne = TSNE(n_components=2, random_state=42, perplexity=min(5, len(embeddings)-1))
    embeddings_2d = tsne.fit_transform(embeddings)
    labels = [meta['intent'] for meta in metadata]
    unique_labels = list(set(labels))
    colors = plt.cm.Paired(np.linspace(0, 1, len(unique_labels)))
    label_to_color = {label: color for label, color in zip(unique_labels, colors)}
    
    plt.figure(figsize=(10, 8), dpi=300)
    for label in unique_labels:
        mask = np.array([l == label for l in labels])
        plt.scatter(embeddings_2d[mask, 0], embeddings_2d[mask, 1], c=[label_to_color[label]], label=label, alpha=0.6)
    plt.title("Visualisation t-SNE des embeddings")
    plt.xlabel("t-SNE 1")
    plt.ylabel("t-SNE 2")
    plt.legend()
    plt.savefig(os.path.join(plot_path, 'tsne_embeddings.png'))
    mlflow.log_artifact(os.path.join(plot_path, 'tsne_embeddings.png'))
    plt.close()
    logger.info(f"t-SNE plot saved to {os.path.join(plot_path, 'tsne_embeddings.png')}")

def plot_similarity_heatmap(questions, embeddings, plot_path):
    if len(embeddings) == 0:
        logger.error("No embeddings for heatmap")
        return
    similarity_matrix = util.cos_sim(embeddings, embeddings).numpy()
    plt.figure(figsize=(12, 10), dpi=300)
    sns.heatmap(similarity_matrix, cmap='viridis', xticklabels=False, yticklabels=False)
    plt.title("Similarité cosinus entre questions")
    plt.xlabel("Questions")
    plt.ylabel("Questions")
    plt.savefig(os.path.join(plot_path, 'similarity_heatmap.png'))
    mlflow.log_artifact(os.path.join(plot_path, 'similarity_heatmap.png'))
    plt.close()
    logger.info(f"Heatmap saved to {os.path.join(plot_path, 'similarity_heatmap.png')}")

def plot_confusion_matrix(true_intents, predicted_intents, plot_path):
    labels = sorted(set(true_intents + predicted_intents))
    cm = confusion_matrix(true_intents, predicted_intents, labels=labels)
    plt.figure(figsize=(10, 8), dpi=300)
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', xticklabels=labels, yticklabels=labels)
    plt.title("Intent Confusion Matrix")
    plt.xlabel("Predicted Intent")
    plt.ylabel("True Intent")
    plt.savefig(os.path.join(plot_path, 'confusion_matrix.png'))
    mlflow.log_artifact(os.path.join(plot_path, 'confusion_matrix.png'))
    plt.close()

def plot_intent_precision(true_intents, predicted_intents, plot_path):
    intent_precision = {intent: calculate_precision_at_k([t for t in true_intents if t == intent], [p for p, t in zip(predicted_intents, true_intents) if t == intent]) for intent in set(true_intents)}
    plt.figure(figsize=(10, 6), dpi=300)
    plt.bar(intent_precision.keys(), intent_precision.values(), color='skyblue')
    plt.title('Precision per Intent')
    plt.xlabel('Intent')
    plt.ylabel('Precision@1')
    plt.xticks(rotation=45)
    plt.tight_layout()
    plt.savefig(os.path.join(plot_path, 'intent_precision_bar.png'))
    mlflow.log_artifact(os.path.join(plot_path, 'intent_precision_bar.png'))
    plt.close()

def plot_correct_vs_incorrect(true_intents, predicted_intents, plot_path):
    correct = sum(1 for t, p in zip(true_intents, predicted_intents) if t == p)
    incorrect = len(true_intents) - correct
    plt.figure(figsize=(6, 6), dpi=300)
    plt.pie([correct, incorrect], labels=['Correct', 'Incorrect'], autopct='%1.1f%%', colors=['#66b3ff', '#ff9999'])
    plt.title('Prediction Accuracy')
    plt.savefig(os.path.join(plot_path, 'correct_vs_incorrect_pie.png'))
    mlflow.log_artifact(os.path.join(plot_path, 'correct_vs_incorrect_pie.png'))
    plt.close()

def plot_response_times(test_queries, search_times, param_times, response_times, plot_path):
    plt.figure(figsize=(12, 6), dpi=300)
    x = range(len(test_queries))
    plt.plot(x, search_times, label='Search Time', color='blue')
    plt.plot(x, param_times, label='Param Time', color='red')
    plt.plot(x, response_times, label='Total Time', color='green')
    plt.xticks(x, test_queries, rotation=45)
    plt.xlabel('Query')
    plt.ylabel('Time (s)')
    plt.title('Response Time Breakdown')
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(plot_path, 'response_times_line.png'))
    mlflow.log_artifact(os.path.join(plot_path, 'response_times_line.png'))
    plt.close()

def plot_distance_boxplot(distances_list, predicted_intents, plot_path):
    data = []
    labels = []
    for dists, intent in zip(distances_list, predicted_intents):
        if dists:
            data.append(dists)
            labels.append(intent)
    plt.figure(figsize=(10, 6), dpi=300)
    plt.boxplot(data, labels=labels)
    plt.title('FAISS Distances by Intent')
    plt.xlabel('Intent')
    plt.ylabel('Distance')
    plt.xticks(rotation=45)
    plt.tight_layout()
    plt.savefig(os.path.join(plot_path, 'distance_boxplot.png'))
    mlflow.log_artifact(os.path.join(plot_path, 'distance_boxplot.png'))
    plt.close()

def save_metrics_table(precision, fidelity,rouge_scores, similarity, bleu, meteor, rouge1, rougeL, diversity, search_times, response_times, param_times, plot_path):
    metrics = {
        'Metric': ['Precision@1', 'Fidelity', 'Similarity', 'BLEU', 'METEOR', 'ROUGE-1', 'ROUGE-L', 'Diversity', 'Avg Search Time (s)', 'Avg Response Time (s)', 'Avg Param Time (s)'],
        'Value': [
            f"{precision:.2f}",
            f"{np.mean(fidelity):.2f}",
            f"{np.mean(similarity):.2f}",
            f"{np.mean(bleu):.2f}",
            f"{np.mean(meteor):.2f}",
            f"{np.mean([score['rouge1'] for score in rouge_scores]):.2f}",
            f"{np.mean([score['rougeL'] for score in rouge_scores]):.2f}",
            f"{diversity:.2f}",
            f"{np.mean(search_times):.6f}",
            f"{np.mean(response_times):.6f}",
            f"{np.mean(param_times):.6f}"
        ]
    }
    df = pd.DataFrame(metrics)
    df.to_csv(os.path.join(plot_path, 'metrics_table.csv'), index=False)
    mlflow.log_artifact(os.path.join(plot_path, 'metrics_table.csv'))

# Calculate precision@k
def calculate_precision_at_k(true_intents, predicted_intents, k=1):
    precision_at_k = []
    for i in range(len(true_intents)):
        top_k = [predicted_intents[i]] if k == 1 else predicted_intents[max(0, i-k+1):i+1]
        correct = 1 if true_intents[i] in top_k else 0
        precision_at_k.append(correct)
    return sum(precision_at_k) / len(precision_at_k) if precision_at_k else 0.0

# Check previous performance
def check_previous_performance():
    try:
        previous_runs = mlflow.search_runs(filter_string="tags.mlflow.runName = 'chatbot_training'")
        if not previous_runs.empty:
            best_run = previous_runs.loc[previous_runs['metrics.precision_at_1'].idxmax()]
            return best_run['metrics.precision_at_1']
        return 0.0
    except Exception as e:
        logger.error(f"Error checking previous performance: {e}")
        return 0.0

# Main function
def main():
    with mlflow.start_run(run_name="chatbot_training"):
        logger.info("Starting chatbot training...")

        # Load intents
        questions, metadata = load_intents(JSON_PATH)
        if not questions:
            logger.error("No questions loaded, exiting.")
            return
        
        # Generate embeddings
        embeddings, metadata = generate_embeddings(questions, metadata)
        if embeddings.size == 0:
            logger.error("No embeddings generated, exiting.")
            return
        
        # Create FAISS index
        index = create_faiss_index(embeddings, metadata, FAISS_INDEX_PATH, METADATA_PATH)
        if index is None:
            logger.error("Failed to create FAISS index, exiting.")
            return
        
        # Inspect metadata
        all_indices, missing_indices = inspect_metadata(METADATA_PATH)
        
        # LangChain FAISS
        documents = [Document(page_content=" ".join(meta.get('mapped_questions', [])), metadata=meta) for meta in metadata]
        vectorstore = LangChainFAISS.from_documents(documents, HuggingFaceEmbeddings(model_name='paraphrase-multilingual-mpnet-base-v2'))
        
        # Visualizations
        distances_list = []
        test_queries = ["Quelles tâches sont en retard ?", "Quel est le résumé du sprint actuel ?"]
        embedder = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')
        for query in test_queries:
            query_embedding = embedder.encode([query], normalize_embeddings=True)
            distances, _ = index.search(query_embedding, k=3)
            distances_list.extend(distances[0])
        
        plot_distance_histogram(distances_list, PLOT_PATH)
        plot_tsne_embeddings(embeddings, metadata[:len(embeddings)], PLOT_PATH)
        plot_similarity_heatmap(questions, embeddings, PLOT_PATH)

        # Test queries
        test_queries = [
            "What tasks are overdue?", "Current sprint summary?", "Tasks for Alice?",
            "Task statuses for project Alpha", "Duration of task #789?",
            "High priority tasks for Beta", "User stories for active sprint?",
            "Remaining time for Gamma?", "Blocked tasks for Alpha?",
            "What's the weather?", "Tasks duration for project Alpha?",
            "Quelles tâches pour Alice ?"
        ]
        true_intents = [
            'intent_2', 'intent_1', 'intent_7', 'intent_8', 'intent_9',
            'intent_3', 'intent_4', 'intent_5', 'intent_6', 'out_of_scope',
            'intent_9', 'intent_7'
        ]
        query_types = [
            'normal', 'normal', 'normal', 'normal', 'normal',
            'normal', 'normal', 'normal', 'normal', 'out_of_scope',
            'ambiguous', 'french'
        ]

        # Clear cache
        response_cache.clear()
        with open(CACHE_FILE, 'w') as f:
            json.dump(response_cache, f)

        # Run tests
        predicted_intents = []
        response_times = []
        search_times = []
        param_times = []
        distances_list = []
        responses = []
        parameters_list = []
        confidences = []
        error_log = []
        for query, true_intent, q_type in zip(test_queries, true_intents, query_types):
            intent, response, distances, response_time, metadata_results, parameters, search_time, param_time, confidence = rag_pipeline(query, use_cache=False)
            predicted_intents.append(intent)
            valid_distances = [d for d in distances if d != float('inf') and not np.isnan(d)]
            distances_list.append(valid_distances)
            response_times.append(response_time)
            search_times.append(search_time)
            param_times.append(param_time)
            responses.append(response)
            parameters_list.append(parameters)
            confidences.append(confidence)
            
            logger.info(f"Query: {query}")
            logger.info(f"Predicted intent: {intent} (True: {true_intent})")
            logger.info(f"Response: {response}")
            logger.info(f"Parameters: {parameters}")
            logger.info(f"Distances: {distances}")
            logger.info(f"Confidence: {confidence:.2f}")
            logger.info(f"Search Time: {search_time:.6f} s")
            logger.info(f"Param Time: {param_time:.6f} s")
            logger.info(f"Total Time: {response_time:.6f} s")
            
            if intent != true_intent:
                error_log.append({
                    'query': query,
                    'true_intent': true_intent,
                    'predicted_intent': intent,
                    'distances': distances,
                    'confidence': confidence,
                    'parameters': parameters,
                    'response': response
                })

        # Metrics
        precision_at_1 = calculate_precision_at_k(true_intents, predicted_intents)
        mlflow.log_metric("precision_at_1", precision_at_1)
        logger.info(f"Precision@1: {precision_at_1:.2f}")

        fidelity_scores = []
        similarity_scores = []
        bleu_scores = []
        meteor_scores = []
        rouge_scores = []
        scorer = rouge_scorer.RougeScorer(['rouge1', 'rougeL'], use_stemmer=True)
        intent_metrics = defaultdict(lambda: {'fidelity': [], 'similarity': [], 'bleu': [], 'meteor': [], 'rouge1': [], 'rougeL': []})
        templates = {
        'intent_1': [
            "Sprint {sprintName} is at {progress}% and ends on {endDate}.",
            "Summary: sprint {sprintId} at {progress}% progress, due {endDate}.",
            "{progress}% of sprint {sprintName} completed, deadline {endDate}."
        ],
        'intent_2': [
            "Overdue tasks: #{taskId} - {taskTitle}, due {dueDate}.",
            "Delays: {taskTitle}, ({taskId}) due {deadlineDate}.",
            "Task #{taskId} ({taskTitle}({ overdue until {dueDate}})"
        ],
        'intent_3': [
            "{priority} priority tasks: #{taskId} - {taskTitle}.",
            "Priority {priority}: {taskTitle}, (#{taskId}).",
            "#{taskId} - {taskTitle} has {priority} priority."
        ],
        'intent_4': [
            "User stories: #{storyId} - {storyTitle}.",
            "Sprint stories: {storyId} ({storyTitle}).",
            "Story #{storyId}: {storyTitle} in progress."
        ],
        'intent_5': [
            "Project {projectId}: {remainingDays} days remaining, ends {endDate}.",
            "Remaining time for {projectId}: {remainingDays} days ({endDate}).",
            "{projectId} set to finish in {remainingDays} days ({endDate})."
        ],
        'intent_6': [
            "Blocked tasks: #{taskId} - {taskTitle}, reason: {blockReason}.",
            "{taskTitle} (#{taskId}) blocked by {blockReason}.",
            "Blockage: {taskId} ({taskTitle}), cause: {blockReason}."
        ],
        'intent_7': [
            "{userId} has tasks: #{taskId_1} - {taskTitle_1}, #{taskId_2} - {taskTitle_2}.",
            "Tasks assigned to {userId}: #{taskId_1} ({taskTitle_1}), #{taskId_2} ({taskTitle_2}).",
            "{userId} is responsible for: {taskTitle_1} (#{taskId_1}), {taskTitle_2} (#{taskId_2})."
        ],
        'intent_8': [
            "Project {projectId}: {todoCount} TODO, {inProgressCount} in progress, {doneCount} done.",
            "Statuses for {projectId}: {todoCount} to do, {inProgressCount} in progress, {doneCount} completed.",
            "{projectId}: {todoCount} TODO tasks, {inProgressCount} in progress, {doneCount} finished."
        ],
        'intent_9': [
            "Task #{taskId} estimated at {estimatedDays} days ({estimatedHours} hours).",
            "Estimated duration for #{taskId}: {estimatedDays} days.",
            "Task #{taskId}: {estimatedHours} hours estimated."
        ],
        'out_of_scope': [
            "Sorry, I can only answer questions about tasks and projects."
        ]
    }

        for query, response, intent in zip(test_queries, responses, predicted_intents):
            metadata_list_query, _, _ = similarity_search(query)
            if not metadata_list_query:
                continue
            meta = metadata_list_query[0]
            expected = np.random.choice(templates.get(intent, ['No response expected.']))

            parameters = extract_parameters(query, intent)
            endpoint = meta.get('endpoint', '')
            api_data = simulate_api_response(endpoint, parameters)
            for key, value in parameters.items():
                expected = expected.replace(f"{{{key}}}", str(value))
            if 'tasks' in api_data and api_data['tasks']:
                task = api_data['tasks'][0]
                expected = expected.replace('{taskId}', str(task.get('taskId', 'Unknown')))
                expected = expected.replace('{taskTitle}', task.get('taskTitle', 'Task'))
                expected = expected.replace('{taskId_1}', str(task.get('taskId', '0')))
                expected = expected.replace('{taskTitle_1}', task.get('taskTitle', 'Task1'))
                expected = expected.replace('{dueDate}', task.get('dueDate', 'Unknown'))
                expected = expected.replace('{blockReason}', task.get('blockReason', 'Unknown'))
                expected = expected.replace('{priority}', task.get('priority', parameters.get('priority', 'High')))
                if len(api_data['tasks']) > 1:
                    task2 = api_data['tasks'][1]
                    expected = expected.replace('{taskId_2}', str(task2.get('taskId', '1')))
                    expected = expected.replace('{taskTitle_2}', task2.get('taskTitle', 'Task2'))
                else:
                    expected = expected.replace('{taskId_2}', 'None')
                    expected = expected.replace('{taskTitle_2}', 'None')
            if 'stories' in api_data and api_data['stories']:
                story = api_data['stories'][0]
                expected = expected.replace('{storyId}', str(story.get('storyId', 'Unknown')))
                expected = expected.replace('{storyTitle}', story.get('storyTitle', 'Story'))
            for key in ['sprintId', 'sprintName', 'progress', 'endDate', 'todoCount', 'inProgressCount', 'doneCount', 'remainingDays', 'estimatedDays', 'estimatedHours']:
                expected = expected.replace(f"{{{key}}}", str(api_data.get(key, 'Unknown')))

            score = 1.0 if response == expected else 0.5
            fidelity_scores.append(score)
            intent_metrics[intent]['fidelity'].append(score)

            embeddings = embedder.encode([response, expected], convert_to_tensor=True)
            similarity = util.cos_sim(embeddings[0], embeddings[1]).item()
            similarity_scores.append(similarity)
            intent_metrics[intent]['similarity'].append(similarity)

            reference = [expected.split()]
            candidate = response.split()
            bleu = sentence_bleu(reference, candidate, weights=(0.25, 0.25, 0.25, 0.25), smoothing_function=SmoothingFunction().method1)
            bleu_scores.append(bleu)
            intent_metrics[intent]['bleu'].append(bleu)

            tokenized_reference = nltk.word_tokenize(expected)
            tokenized_hypothesis = nltk.word_tokenize(response)
            meteor = meteor_score([tokenized_reference], tokenized_hypothesis)
            meteor_scores.append(meteor)
            intent_metrics[intent]['meteor'].append(meteor)

            rouge = scorer.score(expected, response)
            rouge_scores.append({'rouge1': rouge['rouge1'].fmeasure, 'rougeL': rouge['rougeL'].fmeasure})
            intent_metrics[intent]['rouge1'].append(rouge['rouge1'].fmeasure)
            intent_metrics[intent]['rougeL'].append(rouge['rougeL'].fmeasure)

        # Diversity
        unique_responses = len(set(responses))
        response_diversity = unique_responses / len(responses)
        mlflow.log_metric("diversity", response_diversity)
        logger.info(f"Diversity: {response_diversity:.2f} ({unique_responses}/{len(responses)} unique responses)")

        # Additional metrics
        mlflow.log_metric("fidelity", np.mean(fidelity_scores))
        mlflow.log_metric("similarity", np.mean(similarity_scores))
        mlflow.log_metric("bleu", np.mean(bleu_scores))
        mlflow.log_metric("meteor", np.mean(meteor_scores))
        mlflow.log_metric("rouge1", np.mean([score['rouge1'] for score in rouge_scores]))
        mlflow.log_metric("rougeL", np.mean([score['rougeL'] for score in rouge_scores]))
        mlflow.log_metric("avg_search_time", np.mean(search_times))
        mlflow.log_metric("avg_response_time", np.mean(response_times))
        mlflow.log_metric("avg_param_time", np.mean(param_times))

        # Visualizations
        plot_confusion_matrix(true_intents, predicted_intents, PLOT_PATH)
        plot_intent_precision(true_intents, predicted_intents, PLOT_PATH)
        plot_correct_vs_incorrect(true_intents, predicted_intents, PLOT_PATH)
        plot_response_times(test_queries, search_times, param_times, response_times, PLOT_PATH)
        plot_distance_boxplot(distances_list, predicted_intents, PLOT_PATH)
        save_metrics_table(
            precision_at_1, fidelity_scores, similarity_scores, bleu_scores, meteor_scores,
            rouge_scores, rouge_scores, response_diversity, search_times, response_times, param_times, PLOT_PATH
        )

        # Save error log
        if error_log:
            error_df = pd.DataFrame(error_log)
            error_df.to_csv(os.path.join(PLOT_PATH, 'error_log.csv'), index=False)
            mlflow.log_artifact(os.path.join(PLOT_PATH, 'error_log.csv'))
            logger.info(f"Error log saved with {len(error_log)} entries")

        # Report
        report_path = os.path.join(PLOT_PATH, 'evaluation_report.txt')
        with open(report_path, 'w') as f:
            f.write(f"Chatbot Evaluation - {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"Precision@1: {precision_at_1:.2f}\n")
            f.write(f"Fidelity: {np.mean(fidelity_scores):.2f}\n")
            f.write(f"Similarity: {np.mean(similarity_scores):.2f}\n")
            f.write(f"BLEU: {np.mean(bleu_scores):.2f}\n")
            f.write(f"METEOR: {np.mean(meteor_scores):.2f}\n")
            f.write(f"ROUGE-1: {np.mean([score['rouge1'] for score in rouge_scores]):.2f}\n")
            f.write(f"ROUGE-L: {np.mean([score['rougeL'] for score in rouge_scores]):.2f}\n")
            f.write(f"Diversity: {response_diversity:.2f}\n")
            f.write(f"Average Search Time: {np.mean(search_times):.6f} s\n")
            f.write(f"Average Response Time: {np.mean(response_times):.6f} s\n")
            f.write(f"Average Parameter Time: {np.mean(param_times):.6f} s\n")
            f.write(f"Errors: {len(error_log)}\n")
        mlflow.log_artifact(report_path)
        logger.info(f"Report saved at {report_path}")

        # Check previous performance
        previous_precision = check_previous_performance()
        if precision_at_1 < previous_precision:
            logger.error(f"New model underperforms: Precision@1={precision_at_1:.2f} < {previous_precision:.2f}")
            raise Exception("New model rejected: Precision@1 lower than previous run")

        # Cleanup
        gc.collect()

if __name__ == "__main__":
    main()