import great_expectations as gx
from great_expectations.core.expectation_suite import ExpectationSuite
from great_expectations.core.expectation_configuration import ExpectationConfiguration
import json
import pandas as pd

# Initialize context
context = gx.get_context()

# Suite name
suite_name = "intents_suite"

# Delete existing suite if it exists
if suite_name in context.list_expectation_suite_names():
    context.delete_expectation_suite(expectation_suite_name=suite_name)

# Create new suite
suite = ExpectationSuite(expectation_suite_name=suite_name, data_context=context)

# Load intents_complete.json
try:
    with open("app/data/intents_complete.json", "r", encoding="utf-8") as f:
        intents_data = json.load(f)
except Exception as e:
    print(f"Erreur lors du chargement du JSON : {e}")
    exit(1)

# Flatten JSON into a DataFrame
flattened_data = []
required_fields = ["description", "questions", "questions_fr", "sample_responses"]  # metadata retiré
for intent_name, intent_data in intents_data["intents"].items():
    row = {"intent_name": intent_name}
    for field in required_fields + ["endpoint", "metadata"]:
        row[field] = json.dumps(intent_data.get(field, None), ensure_ascii=False) if field in intent_data else None
    flattened_data.append(row)
df = pd.DataFrame(flattened_data)

# Add expectations
expectations = [
    # Check that intent_name is not null
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_not_be_null",
        kwargs={"column": "intent_name"},
        meta={"notes": "Intent name must exist"}
    ),
    # Check row count
    ExpectationConfiguration(
        expectation_type="expect_table_row_count_to_be_between",
        kwargs={"min_value": 1, "max_value": 100},
        meta={"notes": "At least one intent must exist, but not too many"}
    )
]

# Check required fields for all intents
for field in required_fields:
    expectations.append(
        ExpectationConfiguration(
            expectation_type="expect_column_values_to_not_be_null",
            kwargs={"column": field},
            meta={"notes": f"{field} must not be empty for any intent"}
        )
    )

# Check that list fields are lists and have at least one item
for list_field in ["questions", "questions_fr", "sample_responses"]:
    expectations.append(
        ExpectationConfiguration(
            expectation_type="expect_column_values_to_be_in_type_list",
            kwargs={"column": list_field, "type_list": ["str"]},  # Changé de "string" à "str"
            meta={"notes": f"{list_field} must be a valid JSON string"}
        )
    )
    expectations.append(
        ExpectationConfiguration(
            expectation_type="expect_column_values_to_match_json_schema",
            kwargs={
                "column": list_field,
                "json_schema": {"type": "array", "minItems": 1, "items": {"type": "string"}}
            },
            meta={"notes": f"{list_field} must be a non-empty list"}
        )
    )

# Check endpoint for non-out_of_scope intents
expectations.append(
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_not_be_null",
        kwargs={
            "column": "endpoint",
            "row_condition": "intent_name != 'out_of_scope'",
            "condition_parser": "pandas"
        },
        meta={"notes": "endpoint must not be empty for non-out_of_scope intents"}
    )
)

# Add expectations to suite
for expectation in expectations:
    suite.add_expectation(expectation_configuration=expectation)

# Save suite
context.add_or_update_expectation_suite(expectation_suite=suite)
print(f"Suite '{suite_name}' créée avec succès !")

# Create a temporary datasource for validation
try:
    # Define a runtime datasource
    datasource_config = {
        "name": "temp_runtime_datasource",
        "class_name": "Datasource",
        "execution_engine": {"class_name": "PandasExecutionEngine"},
        "data_connectors": {
            "runtime_data_connector": {
                "class_name": "RuntimeDataConnector",
                "batch_identifiers": ["runtime_batch"]
            }
        }
    }
    context.add_datasource(**datasource_config)

    # Validate using the temporary datasource
    batch_request = gx.core.batch.RuntimeBatchRequest(
        datasource_name="temp_runtime_datasource",
        data_asset_name="json_asset",
        data_connector_name="runtime_data_connector",
        runtime_parameters={"batch_data": df},
        batch_identifiers={"runtime_batch": "intents_batch"}
    )
    validator = context.get_validator(
        batch_request=batch_request,
        expectation_suite_name=suite_name
    )
    result = validator.validate()
    print("Résultat de la validation :")
    print(f"Resultat de la validation : {result.success}")
    if not result.success:
        print("Erreurs détectées :")
        print(json.dumps(result.to_json_dict(), indent=2, ensure_ascii=False))
except Exception as e:
    print(f"Erreur lors de la validation : {e}")