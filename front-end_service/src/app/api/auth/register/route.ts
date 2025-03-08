import axios from 'axios';
import getSecretFromVault from "../../../../utils/vault";

export const POST = async (request: Request) => {
    try {
        const body = await request.json(); // Extraction du corps de la requête
        const { firstName, lastName, username, email, password } = body;

        console.log('Données reçues du client:', body);

        // Récupération du secret du client (Client Secret) depuis Vault
        console.log('Tentative de récupération des secrets depuis Vault...');
        const vaultSecrets = await getSecretFromVault('secret/keycloak'); // Vérifie le bon chemin
        console.log('Vault Secrets récupérés:', vaultSecrets);

        if (!vaultSecrets || !vaultSecrets['credentials.secret']) {
            console.log('Aucun secret trouvé dans Vault');
            return new Response(JSON.stringify({ message: 'Erreur dans la récupération du secret' }), { status: 500 });
        }

        const keycloakClientSecret = vaultSecrets['credentials.secret'];
        console.log('Client Secret récupéré:', keycloakClientSecret);

        // Récupération du Client ID depuis les variables d'environnement
        const keycloakClientId = process.env.KEYCLOAK_CLIENT_ID;
        if (!keycloakClientId) {
            console.log('Client ID manquant dans les variables d\'environnement');
            return new Response(JSON.stringify({ message: 'Client ID manquant' }), { status: 500 });
        }
        console.log('Client ID récupéré:', keycloakClientId);

        // Étape 1: Récupération du token d'accès (Admin token) depuis Keycloak
        const tokenResponse = await axios.post(
            `${process.env.KEYCLOAK_URL}/realms/${process.env.KEYCLOAK_REALM}/protocol/openid-connect/token`,
            new URLSearchParams({
                grant_type: 'client_credentials',
                client_id: keycloakClientId,
                client_secret: keycloakClientSecret,
            }),
            {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
            }
        );
        const accessToken = tokenResponse.data.access_token;
        console.log('Token d\'accès récupéré:', accessToken);

        // Étape 2: Envoi de la requête pour créer un nouvel utilisateur dans Keycloak
        const response = await axios.post(
            `${process.env.KEYCLOAK_URL}/admin/realms/${process.env.KEYCLOAK_REALM}/users`,
            {
                username,
                credentials: [{ type: 'password', value: password, temporary: false }],
                email,
                firstName,
                lastName,
                enabled: true,
            },
            {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`,
                },
            }
        );

        console.log('Réponse de Keycloak:', response.status);

        if (response.status === 201) {
            return new Response(JSON.stringify({ message: 'Inscription réussie!' }), { status: 200 });
        } else {
            console.log('Erreur lors de l\'inscription, réponse non 201:', response.status);
            return new Response(JSON.stringify({ message: 'Erreur lors de l\'inscription.' }), { status: 400 });
        }
    } catch (error) {
        console.error('Erreur lors de l\'inscription:', error);

        if (axios.isAxiosError(error)) {
            console.error('Détails de l\'erreur Axios:', error.response?.data);
        }

        return new Response(JSON.stringify({ message: 'Erreur serveur' }), { status: 500 });
    }
};
