package com.auth.authentification_service.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

@Service
public class VaultService {
    private final VaultTemplate vaultTemplate;

    // Injection de VaultTemplate via le constructeur
    @Autowired
    public VaultService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    public String getClientSecret() {
        // Lire un secret spécifique depuis Vault à l'emplacement "secret/keycloak"
        VaultResponse response = vaultTemplate.read("secret/keycloak");
        if (response != null && response.getData() != null) {
            String secret = (String) response.getData().get("credentials.secret");
            return  secret;
        }
        return "Aucun secret trouvé dans Vault.";
    }

    public String getTestClientSecret() {
        // Lire un secret spécifique depuis Vault à l'emplacement "secret/keycloak"
        VaultResponse response = vaultTemplate.read("secret/keycloak");
        if (response != null && response.getData() != null) {
            String secret = (String) response.getData().get("credentials.secret.test");
            return  secret;
        }
        return "Aucun secret trouvé dans Vault.";
    }
}