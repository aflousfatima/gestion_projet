# create_suite.py
import great_expectations as gx
from great_expectations.core.expectation_suite import ExpectationSuite
from great_expectations.core.expectation_configuration import ExpectationConfiguration

# Obtenir le contexte
context = gx.get_context()

# Nom de la suite
suite_name = "tasks_dataset_suite"

# Supprimer la suite si elle existe
if suite_name in context.list_expectation_suite_names():
    context.delete_expectation_suite(expectation_suite_name=suite_name)

# Créer une nouvelle suite
suite = ExpectationSuite(expectation_suite_name=suite_name, data_context=context)

# Ajouter des règles
expectations = [
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_not_be_null",
        kwargs={"column": "title"},
        meta={"notes": "Task title must not be empty"}
    ),
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_be_of_type",
        kwargs={"column": "estimationTime", "type_": "int64"},  # Changé de "int" à "int64"
        meta={"notes": "Estimation time must be an integer"}
    ),
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_be_between",
        kwargs={"column": "estimationTime", "min_value": 0.0001},
        meta={"notes": "Estimation time must be positive"}
    ),
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_match_strftime_format",
        kwargs={"column": "creationDate", "strftime_format": "%Y-%m-%d"},
        meta={"notes": "Creation date must be in YYYY-MM-DD format"}
    ),
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_be_in_set",
        kwargs={"column": "status", "value_set": ["TO_DO", "IN_PROGRESS", "DONE", "BLOCKED", "ARCHIVED", "CANCELLED"]},  # Mise à jour des valeurs
        meta={"notes": "Status must be one of the allowed values"}
    ),
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_be_in_set",
        kwargs={"column": "priority", "value_set": ["LOW", "MEDIUM", "HIGH", "CRITICAL"]},  # Mise à jour des valeurs
        meta={"notes": "Priority must be one of the allowed values"}
    ),
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_be_between",
        kwargs={"column": "progress", "min_value": 0.0, "max_value": 100.0},
        meta={"notes": "Progress must be between 0 and 100"}
    ),
    ExpectationConfiguration(
        expectation_type="expect_column_values_to_not_be_null",
        kwargs={"column": "projectId"},
        meta={"notes": "Project ID must not be empty"}
    )
]

for expectation in expectations:
    suite.add_expectation(expectation_configuration=expectation)

# Sauvegarder la suite
context.add_or_update_expectation_suite(expectation_suite=suite)
print(f"Suite '{suite_name}' créée avec succès !")