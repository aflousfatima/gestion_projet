import pandas as pd
import numpy as np
from datetime import datetime, timedelta

# Configurer la graine pour reproductibilité
np.random.seed(42)

# Paramètres
n_tasks = 200
start_date = datetime(2025, 1, 1)
end_date = datetime(2025, 12, 31)

# Listes pour cohérence IT
tags_list = ['frontend', 'backend', 'bug', 'urgent', 'api', 'devops', 'security', 'testing', 'refactor', 'database']
status_list = ['TO_DO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'ARCHIVED', 'CANCELLED']
priority_list = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
users_list = ['user1', 'user2', 'user3', 'user4', 'user5']

# Dictionnaire pour titres et descriptions alignés avec les tags
task_templates = {
    'frontend': {
        'titles': ['Fix UI rendering bug', 'Implement responsive navbar', 'Update CSS styles', 'Optimize client-side rendering'],
        'descriptions': ['Resolve layout issues on mobile devices', 'Add responsive navigation bar', 'Refactor CSS for better performance', 'Improve React component rendering']
    },
    'backend': {
        'titles': ['Develop REST API endpoint', 'Implement user authentication', 'Optimize database queries', 'Refactor payment processing'],
        'descriptions': ['Create API for user management', 'Add JWT-based authentication', 'Improve SQL query performance', 'Update payment gateway integration']
    },
    'bug': {
        'titles': ['Fix login failure bug', 'Resolve data validation error', 'Patch security vulnerability', 'Correct API response issue'],
        'descriptions': ['Fix issue preventing user login', 'Handle invalid input data', 'Patch XSS vulnerability', 'Ensure correct JSON response format']
    },
    'urgent': {
        'titles': ['Hotfix production outage', 'Patch critical security flaw', 'Resolve urgent API failure', 'Fix emergency UI crash'],
        'descriptions': ['Restore service after outage', 'Apply urgent security patch', 'Fix API downtime issue', 'Resolve mobile app crash']
    },
    'api': {
        'titles': ['Implement GraphQL endpoint', 'Develop webhook integration', 'Update API versioning', 'Create data export API'],
        'descriptions': ['Add GraphQL query support', 'Integrate with third-party webhook', 'Support v2 API endpoints', 'Enable CSV data export via API']
    },
    'devops': {
        'titles': ['Set up CI/CD pipeline', 'Configure Kubernetes cluster', 'Optimize Docker images', 'Automate deployment scripts'],
        'descriptions': ['Implement GitHub Actions pipeline', 'Deploy app on Kubernetes', 'Reduce Docker image size', 'Automate ECS deployment']
    },
    'security': {
        'titles': ['Implement OAuth2 authentication', 'Add rate limiting to API', 'Conduct security audit', 'Patch SQL injection vulnerability'],
        'descriptions': ['Integrate OAuth2 for secure login', 'Prevent API abuse with rate limits', 'Review codebase for vulnerabilities', 'Fix SQL injection in queries']
    },
    'testing': {
        'titles': ['Write unit tests for API', 'Automate end-to-end tests', 'Perform load testing', 'Update test coverage'],
        'descriptions': ['Add pytest tests for endpoints', 'Create Cypress E2E tests', 'Simulate high traffic with Locust', 'Improve test coverage to 90%']
    },
    'refactor': {
        'titles': ['Refactor legacy codebase', 'Modularize backend services', 'Optimize frontend components', 'Rewrite database schema'],
        'descriptions': ['Break down monolithic app', 'Split services into microservices', 'Refactor React components', 'Normalize database tables']
    },
    'database': {
        'titles': ['Migrate to PostgreSQL', 'Optimize database indexing', 'Implement data archiving', 'Update schema for new feature'],
        'descriptions': ['Migrate from MySQL to PostgreSQL', 'Add indexes for faster queries', 'Archive old records', 'Add new tables for feature']
    }
}

# Fonction pour générer des dates cohérentes
def random_date(start, end):
    if start is None or end is None:
        raise ValueError("start and end must be datetime objects, not None")
    delta = (end - start).days
    if delta < 0:
        raise ValueError("end date must be after start date")
    return start + timedelta(days=np.random.randint(0, delta))

# Générer les tâches
tasks = []
for task_id in range(1, n_tasks + 1):
    # Type de tâche et estimationTime
    task_type = np.random.choice(['short', 'medium', 'long'], p=[0.4, 0.4, 0.2])
    if task_type == 'short':
        base_estimation = np.random.uniform(10, 200)
    elif task_type == 'medium':
        base_estimation = np.random.uniform(200, 1000)
    else:  # long
        base_estimation = np.random.uniform(1000, 5000)

    # Sélectionner les tags (0 à 3)
    num_tags = np.random.randint(0, 4)
    itemtags = np.random.choice(tags_list, size=num_tags, replace=False)
    itemtags = ','.join(itemtags) if num_tags > 0 else ''

    # Ajuster estimationTime selon les tags
    estimation_time = base_estimation
    if 'urgent' in itemtags:
        estimation_time *= 1.2  # +20% pour urgent
    if 'backend' in itemtags or 'security' in itemtags or 'database' in itemtags:
        estimation_time *= 1.3  # +30% pour tâches complexes
    if 'refactor' in itemtags:
        estimation_time *= 1.5  # +50% pour refactorisation

    # Choisir un tag principal pour aligner titre et description
    main_tag = itemtags.split(',')[0] if itemtags else np.random.choice(tags_list)
    title = np.random.choice(task_templates[main_tag]['titles'])
    description = np.random.choice(task_templates[main_tag]['descriptions'])

    # Dates
    creation_date = random_date(start_date, end_date)
    task_start_date = creation_date + timedelta(days=np.random.randint(0, 5))
    task_due_date = task_start_date + timedelta(days=np.random.randint(1, 30))

    # Statut et progress
    status = np.random.choice(status_list, p=[0.30, 0.40, 0.20, 0.05, 0.03, 0.02])
    if status == 'TO_DO':
        progress = 0.0
        task_start_date = None
        actual_duration = 0.0
        task_completed_date = None
    elif status == 'IN_PROGRESS':
        progress = np.random.uniform(0.1, 99.9)
        actual_duration = estimation_time * (progress / 100) * np.random.uniform(0.8, 1.2)
        task_completed_date = None
    elif status == 'DONE':
        progress = 100.0
        actual_duration = estimation_time * np.random.uniform(0.8, 1.2)
        task_completed_date = task_due_date - timedelta(days=np.random.randint(0, 5))
    elif status == 'BLOCKED':
        progress = np.random.uniform(0.0, 50.0)
        actual_duration = estimation_time * (progress / 100) * np.random.uniform(0.5, 1.0)
        task_completed_date = None
    elif status == 'ARCHIVED':
        progress = 100.0
        actual_duration = estimation_time * np.random.uniform(0.8, 1.2)
        task_completed_date = task_due_date - timedelta(days=np.random.randint(0, 5))
    else:  # CANCELLED
        progress = np.random.uniform(0.0, 50.0)
        actual_duration = estimation_time * (progress / 100) * np.random.uniform(0.5, 1.0)
        task_due_date = None
        task_completed_date = None

    # Autres colonnes
    priority = np.random.choice(priority_list, p=[0.30, 0.40, 0.20, 0.10])
    num_users = np.random.randint(0, 4)
    assigned_users = np.random.choice(users_list, size=num_users, replace=False)
    assigned_user_ids = ','.join(assigned_users) if num_users > 0 else ''
    project_id = np.random.randint(1, 21)

    tasks.append({
        'id': task_id,
        'title': title,
        'description': description,
        'creationDate': creation_date.strftime('%Y-%m-%d'),
        'startDate': task_start_date.strftime('%Y-%m-%d') if task_start_date else '',
        'dueDate': task_due_date.strftime('%Y-%m-%d') if task_due_date else '',
        'completedDate': task_completed_date.strftime('%Y-%m-%d') if task_completed_date else '',
        'estimationTime': estimation_time,
        'status': status,
        'priority': priority,
        'progress': progress,
        'itemtags': itemtags,
        'assignedUserIds': assigned_user_ids,
        'projectId': project_id,
        'actual_duration': actual_duration
    })

# Créer DataFrame et sauvegarder
df = pd.DataFrame(tasks)
df.to_csv('tests/data/test_tasks.csv', index=False)
print(f"Dataset de test généré : {len(df)} tâches")
print(f"Tâches courtes (< 200 min) : {len(df[df['estimationTime'] < 200])}")
print(f"Tâches moyennes (200-1000 min) : {len(df[(df['estimationTime'] >= 200) & (df['estimationTime'] <= 1000)])}")
print(f"Tâches longues (> 1000 min) : {len(df[df['estimationTime'] > 1000])}")