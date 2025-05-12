package com.auth.authentification_service.utils;

import com.auth.authentification_service.Repository.InvitationRepository;
import com.auth.authentification_service.Repository.ProjectMemberRepository;
import com.auth.authentification_service.Service.VaultService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@ActiveProfiles("test")
@SpringBootTest
public class IsUserExist {

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.resource}")
    private String keycloakClientId;

    private final RestTemplate restTemplate;
    private final VaultService vaultService;

    public IsUserExist(RestTemplate restTemplate , VaultService vaultService) {
        this.restTemplate = restTemplate;
        this.vaultService = vaultService;
        System.out.println("Keycloak URL : " + keycloakUrl);
        System.out.println("Keycloak Realm : " + keycloakRealm);
        System.out.println("Keycloak Client ID : " + keycloakClientId);

    }

    public String getAdminToken() {
        String clientSecret = vaultService.getTestClientSecret();
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

    public boolean userExists(String email, String accessToken) {
        String usersUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users?email=" + email;
        System.out.println("Vérification utilisateur à l'URL : " + usersUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(usersUrl, HttpMethod.GET, request, List.class);
        return response.getStatusCode() == HttpStatus.OK && response.getBody() != null && !response.getBody().isEmpty();
    }
}
