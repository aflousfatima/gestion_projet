package com.auth.authentification_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private VaultTemplate vaultTemplate;

    @GetMapping("/vault-secret")
    public String getVaultSecret() {
        // Lire un secret spécifique depuis Vault à l'emplacement "secret/keycloak"
        VaultResponse response = vaultTemplate.read("secret/keycloak");
        if (response != null && response.getData() != null) {
            String secret = (String) response.getData().get("credentials.secret");
            return "Le secret Keycloak récupéré de Vault est : " + secret;
        }
        return "Aucun secret trouvé dans Vault.";
    }


    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @GetMapping("/keycloak-status")
    public ResponseEntity<String> checkKeycloakStatus() {
        String testUrl = keycloakUrl + "/realms/" + keycloakRealm;

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(testUrl, String.class);
            return ResponseEntity.ok("Keycloak est accessible ! Réponse : " + response.getStatusCode());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur d'accès à Keycloak : " + e.getMessage());
        }
    }
}

