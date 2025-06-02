# configure_datasource.py
import great_expectations as gx
from great_expectations.data_context import FileDataContext

# Initialiser le contexte
context = FileDataContext(project_root_dir=".")

# Définir le nom de la datasource
datasource_name = "ai_service_data"

# Supprimer la datasource existante si elle existe
if datasource_name in context.datasources:
    context.delete_datasource(datasource_name)

# Ajouter une datasource Pandas
try:
    datasource = context.sources.add_or_update_pandas(name=datasource_name)
    
    # Ajouter l'asset CSV
    csv_asset = datasource.add_csv_asset(
        name="csv_asset",
        filepath_or_buffer="app/data/tasks_dataset.csv"
    )
    
    # Ajouter l'asset JSON
    json_asset = datasource.add_json_asset(
        name="json_asset",
        path_or_buf="app/data/intents_complete.json"  # Chemin du fichier JSON
    )
    
    print(f"Datasource '{datasource_name}' ajoutée avec succès avec les assets CSV et JSON !")
except Exception as e:
    print(f"Erreur lors de l'ajout de la datasource : {e}")

# Lister les datasources disponibles
print("Datasources disponibles :")
print(context.datasources)