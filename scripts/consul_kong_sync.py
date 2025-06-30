import requests
import time
import json

CONSUL_URL = "http://localhost:8500/v1/health/service"
KONG_ADMIN_URL = "http://localhost:8001"
HOST_IP = "192.168.1.154"  # Ton IP locale

def get_all_services():
    """Récupérer tous les services sains depuis Consul."""
    try:
        catalog_response = requests.get("http://localhost:8500/v1/catalog/services").json()
        services = {}
        for service_name in catalog_response.keys():
            if service_name == "consul":
                continue
            health_response = requests.get(f"{CONSUL_URL}/{service_name}?passing").json()
            if health_response:
                services[service_name] = health_response
        return services
    except Exception as e:
        print(f"Erreur lors de la récupération des services Consul : {e}")
        return {}

def generate_kong_config(services):
    """Générer une configuration Kong dynamique."""
    config = {
        "_format_version": "3.0",
        "services": []
    }
    for service_name, instances in services.items():
        if not instances:
            continue
        svc = instances[0]["Service"]
        addr = HOST_IP if svc["Address"] == "localhost" else svc["Address"]
        port = svc["Port"]
        service_url = f"http://{addr}:{port}"
        kong_service_name = service_name.lower()

        config["services"].append({
            "name": kong_service_name,
            "url": service_url,
            "routes": [{"name": f"{kong_service_name}-route", "paths": [f"/{kong_service_name}"]}]
        })
    return config

def sync_services():
    """Synchroniser dynamiquement Consul avec Kong."""
    try:
        services = get_all_services()
        if not services:
            print("Aucun service sain trouvé dans Consul.")
            return

        # Générer la config en mémoire
        config = generate_kong_config(services)
        config_json = json.dumps(config)

        # Pousser la config dans Kong via l’API /config
        response = requests.post(
            f"{KONG_ADMIN_URL}/config",
            headers={"Content-Type": "application/json"},
            data=config_json
        )
        if response.status_code == 201:
            print("Configuration synchronisée avec succès dans Kong.")
            for svc in config["services"]:
                print(f"Service {svc['name']} configuré avec URL {svc['url']}")
        else:
            print(f"Erreur lors de la synchronisation: {response.status_code} - {response.text}")

    except Exception as e:
        print(f"Erreur lors de la synchronisation : {e}")

if __name__ == "__main__":
    print("Démarrage de la synchronisation dynamique Consul -> Kong...")
    while True:
        sync_services()
        time.sleep(10)  # Synchronisation toutes les 10 secondes