# create_checkpoint.py
import great_expectations as gx
from great_expectations.checkpoint import Checkpoint
from great_expectations.data_context import FileDataContext

# Initialiser le contexte
context = FileDataContext(project_root_dir=".")

# Nom du checkpoint
checkpoint_name = "tasks_dataset_checkpoint"

# Configuration du batch_request
batch_request = {
    "datasource_name": "ai_service_data",
    "data_asset_name": "csv_asset"
}

# Configuration du checkpoint
checkpoint_config = {
    "name": checkpoint_name,
    "config_version": 1.0,
    "run_name_template": "%Y%m%d-%H%M%S-tasks-checkpoint",
    "validations": [
        {
            "batch_request": batch_request,
            "expectation_suite_name": "tasks_dataset_suite"
        }
    ],
    "action_list": [
        {
            "name": "store_validation_result",
            "action": {
                "class_name": "StoreValidationResultAction"
            }
        },
        {
            "name": "update_data_docs",
            "action": {
                "class_name": "UpdateDataDocsAction"
            }
        }
    ]
}

# Supprimer le checkpoint s'il existe
if checkpoint_name in context.list_checkpoints():
    context.delete_checkpoint(checkpoint_name)

# Ajouter le checkpoint
try:
    checkpoint = Checkpoint(
        data_context=context,
        **checkpoint_config
    )
    context.add_or_update_checkpoint(checkpoint=checkpoint)
    print(f"Checkpoint '{checkpoint_name}' créé avec succès !")
except Exception as e:
    print(f"Erreur lors de la création du checkpoint : {e}")

# Exécuter le checkpoint
try:
    result = context.run_checkpoint(checkpoint_name=checkpoint_name)
    print("Résultat de la validation :")
    print(f"Success: {result.success}")
    if not result.success:
        print("Erreurs détectées :")
        for validation_result in result.run_results.values():
            print(validation_result["validation_result"])
except Exception as e:
    print(f"Erreur lors de l'exécution du checkpoint : {e}")