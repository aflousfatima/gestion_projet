import great_expectations as gx
from great_expectations.checkpoint import Checkpoint
from great_expectations.core.batch import RuntimeBatchRequest
from great_expectations.data_context import FileDataContext
import json
import pandas as pd

# Initialize context
context = FileDataContext()

# Checkpoint name
checkpoint_name = "intents_checkpoint"

# Load intents JSON
try:
    with open("app/data/intents_complete.json", "r", encoding="utf-8") as f:
        intents_data = json.load(f)
except Exception as e:
    print(f"Erreur lors du chargement du JSON : {e}")
    exit(1)

# Flatten JSON into a DataFrame
flattened_data = []
required_fields = ["description", "questions", "questions_fr", "sample_responses"]
for intent_name, intent_data in intents_data["intents"].items():
    row = {"intent_name": intent_name}
    for field in required_fields + ["endpoint", "metadata"]:
        row[field] = json.dumps(intent_data.get(field, None), ensure_ascii=False) if field in intent_data else None
    flattened_data.append(row)
df = pd.DataFrame(flattened_data)

# Create a temporary datasource
try:
    datasource_config = {
        "name": "temp_runtime_intents",
        "class_name": "Datasource",
        "execution_engine": {"class_name": "PandasExecutionEngine"},
        "data_connectors": {
            "runtime_data_connector": {
                "class_name": "RuntimeDataConnector",
                "batch_identifiers": ["identifier"]
            }
        }
    }
    context.add_or_update_datasource(**datasource_config)
except Exception as e:
    print(f"Erreur lors de la création de la datasource : {e}")
    exit(1)

# Batch request (created separately)
batch_request = RuntimeBatchRequest(
    datasource_name="temp_runtime_intents",
    data_asset_name="intents_asset",
    data_connector_name="runtime_data_connector",
    runtime_parameters={"batch_data": df},
    batch_identifiers={"identifier": "intents_batch"}
)

# Checkpoint configuration (without batch_request)
checkpoint_config = {
    "name": checkpoint_name,
    "config_version": 1.0,
    "run_name_template": "%Y%m%d-%H%M%S-intents-checkpoint",
    "validations": [
        {
            "expectation_suite_name": "intents_suite"
        }
    ],
    "action_list": [
        {
            "name": "store_validation_result",
            "action": {"class_name": "StoreValidationResultAction"}
        },
        {
            "name": "update_data_docs",
            "action": {"class_name": "UpdateDataDocsAction"}
        }
    ]
}

# Delete existing checkpoint if it exists
if checkpoint_name in context.list_checkpoints():
    context.delete_checkpoint(checkpoint_name)

# Create and run checkpoint
try:
    checkpoint = Checkpoint(data_context=context, **checkpoint_config)
    context.add_or_update_checkpoint(checkpoint=checkpoint)
    print(f"Checkpoint '{checkpoint_name}' créé avec succès !")
    result = context.run_checkpoint(
        checkpoint_name=checkpoint_name,
        batch_request=batch_request
    )
    print("Résultat de la validation :")
    print(f"Success: {result.success}")
    if not result.success:
        print("Erreurs détectées :")
        for validation_result in result.run_results.values():
            print(json.dumps(validation_result["validation_result"], indent=2, ensure_ascii=False))
except Exception as e:
    print(f"Erreur lors de la création ou de l'exécution du checkpoint : {e}")