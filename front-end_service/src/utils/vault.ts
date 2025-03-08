import fs from 'fs';
import https from 'https';
import vault from 'node-vault';

// Charger le certificat auto-signé
const certPath = 'C:/Users/Admin/vault/certs/localhost.crt';

// Créer un agent HTTPS avec le certificat
const agent = new https.Agent({
  ca: fs.readFileSync(certPath),  // Ajouter explicitement le certificat
  rejectUnauthorized: true,       // Garde la vérification SSL pour éviter des risques de sécurité
});

// Initialiser le client Vault avec l'agent configuré
const client = vault({
  apiVersion: 'v1',
  endpoint: process.env.VAULT_URL, // Utiliser la variable d'environnement VAULT_URL
  token: process.env.VAULT_TOKEN,   // Utiliser la variable d'environnement VAULT_TOKEN
  requestOptions: { // Utilise requestOptions au lieu de httpOptions
    agent,
  },
});

// Fonction pour récupérer un secret de Vault
export const getSecretFromVault = async (path: string) => {
  try {
    console.log(`Récupération du secret depuis Vault : ${path}`);
    const response = await client.read(path);
    console.log('Réponse de Vault:', response.data);
    return response.data;
  } catch (error) {
    console.error('❌ Erreur Vault:', error);
    throw new Error('Erreur Vault');
  }
};

// Exportation par défaut de la fonction
export default getSecretFromVault;