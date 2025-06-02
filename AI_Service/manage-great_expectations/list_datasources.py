import great_expectations as gx

# Obtenir le contexte
context = gx.get_context()

# Lister les datasources
print("Datasources disponibles :")
print(context.list_datasources())