import joblib

model_path = r"app/models/best_model.pkl"
model = joblib.load(model_path)

# Vérifier les features attendues (si le modèle est un modèle scikit-learn ou compatible)
if hasattr(model, "feature_names_in_"):
    print("Features attendues par le modèle :")
    print(model.feature_names_in_)
else:
    print("Le modèle n'a pas d'attribut feature_names_in_ (peut-être un modèle LightGBM ou CatBoost)")