package com.auth.authentification_service.Service;

import com.auth.authentification_service.DTO.KeycloakTokenResponse;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class LoginService {


    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.resource}")
    private String keycloakClientId;

    private final RestTemplate restTemplate;
    private final  VaultService vaultService;

    public LoginService(RestTemplate restTemplate , VaultService vaultService) {
        this.restTemplate = restTemplate;
        this.vaultService = vaultService;
    }

    public String authenticateUser(String email, String password) throws Exception {
        System.out.println("Récupération du client secret depuis Vault...");
        String keycloakClientSecret = vaultService.getClientSecret();
        System.out.println("Client secret récupéré : " + keycloakClientSecret);

        // Endpoint pour obtenir un token d'accès depuis Keycloak
        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
        System.out.println("URL de token : " + tokenUrl);

        // Paramètres nécessaires pour obtenir le token
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", keycloakClientId);
        params.add("client_secret", keycloakClientSecret);
        params.add("username", email);  // Utilisez l'email comme identifiant
        params.add("password", password);  // Mot de passe de l'utilisateur
        params.add("grant_type", "password");

        System.out.println("Paramètres envoyés :");
        System.out.println("client_id: " + keycloakClientId);
        System.out.println("username: " + email);
        System.out.println("grant_type: password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        // Envoi de la requête POST à Keycloak pour obtenir les tokens
        System.out.println("Envoi de la requête POST à Keycloak...");
        ResponseEntity<KeycloakTokenResponse> responseEntity = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, entity, KeycloakTokenResponse.class);

        System.out.println("Réponse de Keycloak reçue : " + responseEntity.getStatusCode());

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            System.out.println("Token d'accès récupéré avec succès !");
            return responseEntity.getBody().getAccessToken();
        } else {
            System.out.println("Échec de l'authentification avec Keycloak, code d'état : " + responseEntity.getStatusCode());
            throw new Exception("Échec de l'authentification avec Keycloak");
        }
    }

    public String decodeToken(String token) {
        // Décoder le token sans valider la signature
        DecodedJWT decodedJWT = JWT.decode(token);

        // Extraire l'ID utilisateur (le "sub")
        String userId = decodedJWT.getSubject();  // Le 'sub' dans le JWT est l'ID utilisateur
        String issuer = decodedJWT.getIssuer();   // Le 'iss' peut aussi être utile
        long expirationTime = decodedJWT.getExpiresAt().getTime(); // Expiration du token

        // Afficher ou retourner l'ID utilisateur
        System.out.println("User ID (sub): " + userId);
        System.out.println("Issuer: " + issuer);
        System.out.println("Expiration Time: " + expirationTime);

        return userId;  // Retourner l'ID utilisateur
    }
}
