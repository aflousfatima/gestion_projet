import axios from "axios";

export const POST = async (request: Request) => {
    try {
        const { email, password } = await request.json();

        // Récupération du client ID et URL Keycloak
        const keycloakClientId = process.env.KEYCLOAK_CLIENT_ID;
        const keycloakUrl = process.env.KEYCLOAK_URL;
        const keycloakRealm = process.env.KEYCLOAK_REALM;

        if (!keycloakClientId || !keycloakUrl || !keycloakRealm) {
            return new Response(JSON.stringify({ message: "Configuration Keycloak manquante" }), { status: 500 });
        }

        // Demande de token avec email & password
        const tokenResponse = await axios.post(
            `${keycloakUrl}/realms/${keycloakRealm}/protocol/openid-connect/token`,
            new URLSearchParams({
                grant_type: "password",
                client_id: keycloakClientId,
                username: email,
                password: password,
            }),
            {
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
            }
        );

        const { access_token, refresh_token } = tokenResponse.data;

        // Retourne les tokens
        return new Response(JSON.stringify({ access_token, refresh_token }), { status: 200 });

    } catch (error) {
        console.error("Erreur d'authentification :", error);

        if (axios.isAxiosError(error)) {
            return new Response(JSON.stringify({ message: error.response?.data?.error_description || "Erreur d'authentification" }), { status: 400 });
        }

        return new Response(JSON.stringify({ message: "Erreur serveur" }), { status: 500 });
    }
};
