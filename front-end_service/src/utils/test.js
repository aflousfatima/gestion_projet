import axios from 'axios';
import { readFileSync } from 'fs';  // Utilisation de 'fs' pour lire le certificat
import { Agent } from 'https';  // Import de l'Agent HTTPS

const certPath = 'C:/Users/Admin/vault/certs/localhost.crt'; // Le chemin de ton certificat
const token = 'hvs.MHbUKBnwKC0jlI1va9zgVPC7';  // Ton token Vault

const options = {
  method: 'GET',
  url: 'https://localhost:8200/v1/secret/keycloak',
  headers: {
    'X-Vault-Token': token,
  },
  httpsAgent: new Agent({
    ca: readFileSync(certPath),  // Utilisation du certificat pour vérifier la connexion
    rejectUnauthorized: true,  // On garde la vérification SSL pour éviter les problèmes de sécurité
  }),
};

axios(options)
  .then(response => {
    console.log('✅ Réponse cURL:', response.data);
  })
  .catch(error => {
    console.error('❌ Erreur cURL:', error);
  });
