from pydrive2.auth import GoogleAuth

gauth = GoogleAuth()
gauth.settings['client_config_file'] = "client_secrets.json"
gauth.LocalWebserverAuth()  # Cela ouvre un navigateur pour l'authentification

gauth.SaveCredentialsFile("credentials.json")  # Sauvegarde le token valide dans ce fichier
print("Token d'authentification sauvegardé dans credentials.json")
