package com.auth.authentification_service.Service;

import com.auth.authentification_service.DTO.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class KeycloakService {
    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.resource}")
    private String keycloakClientId;

    private final VaultService vaultService;
    private final RestTemplate restTemplate;

    public KeycloakService(VaultService vaultService, RestTemplate restTemplate) {
        this.vaultService = vaultService;
        this.restTemplate = restTemplate;
    }

    public String getAdminToken() {
        String clientSecret = vaultService.getClientSecret();
        System.out.println("Client Secret utilisé : " + clientSecret);
        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

        System.out.println("URL pour récupérer le token : " + tokenUrl);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", keycloakClientId);
        params.add("client_secret", clientSecret);
        System.out.println("Paramètres envoyés : " + params);

        HttpHeaders headers = new HttpHeaders();
        headers.forEach((key, value) -> System.out.println("Header : " + key + " = " + value));

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        System.out.println("Headers envoyés après ajout : " + headers);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        System.out.println("Envoi de la requête pour récupérer le token...");

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        System.out.println("Statut de la réponse : " + response.getStatusCode());

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            System.out.println("Token récupéré avec succès !");
            return response.getBody().get("access_token").toString();
        }

        System.out.println("Erreur lors de la récupération du token !");
        throw new RuntimeException("Impossible de récupérer le token d'admin Keycloak");
    }

    public ResponseEntity<String> createUser(UserDto userDTO) {
        System.out.println("Début de la création de l'utilisateur : " + userDTO.getUsername());

        String accessToken = getAdminToken();
        String createUserUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users";

        System.out.println("URL pour créer l'utilisateur : " + createUserUrl);

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", userDTO.getUsername());
        userPayload.put("firstName", userDTO.getFirstName());
        userPayload.put("lastName", userDTO.getLastName());
        userPayload.put("email", userDTO.getEmail());
        userPayload.put("enabled", true);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", userDTO.getPassword());
        credentials.put("temporary", "false");

        userPayload.put("credentials", new Map[]{credentials});

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userPayload, headers);

        System.out.println("Envoi de la requête pour créer l'utilisateur...");

        ResponseEntity<String> response = restTemplate.postForEntity(createUserUrl, request, String.class);

        System.out.println("Statut de la réponse : " + response.getStatusCode());

        if (response.getStatusCode() == HttpStatus.CREATED) {
            System.out.println("Utilisateur créé avec succès !");
        } else {
            System.out.println("Échec de la création de l'utilisateur : " + response.getBody());
        }

        return response;
    }
}
