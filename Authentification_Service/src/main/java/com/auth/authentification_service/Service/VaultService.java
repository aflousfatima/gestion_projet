package com.auth.authentification_service.Service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
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
    @RateLimiter(name = "VaultServiceLimiter", fallbackMethod = "clientSecretRateLimiterFallback")
    @Bulkhead(name = "VaultServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "clientSecretBulkheadFallback")
    @Retry(name = "VaultServiceRetry", fallbackMethod = "clientSecretRetryFallback")
    public String getClientSecret() {
        // Lire un secret spécifique depuis Vault à l'emplacement "secret/keycloak"
        VaultResponse response = vaultTemplate.read("secret/keycloak");
        if (response != null && response.getData() != null) {
            String secret = (String) response.getData().get("credentials.secret");
            return  secret;
        }
        return "Aucun secret trouvé dans Vault.";
    }
    public String clientSecretRateLimiterFallback(Throwable t) {
        System.out.println("RateLimiter fallback for getClientSecret: " + t.getMessage());
        return "Rate limit exceeded for Vault access.";
    }

    public String clientSecretBulkheadFallback(Throwable t) {
        System.out.println("Bulkhead fallback for getClientSecret: " + t.getMessage());
        return "Too many concurrent Vault requests.";
    }

    public String clientSecretRetryFallback(Throwable t) {
        System.out.println("Retry fallback for getClientSecret: " + t.getMessage());
        return "Failed to retrieve secret from Vault after retries.";
    }

    @RateLimiter(name = "VaultServiceLimiter", fallbackMethod = "testClientSecretRateLimiterFallback")
    @Bulkhead(name = "VaultServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "testClientSecretBulkheadFallback")
    @Retry(name = "VaultServiceRetry", fallbackMethod = "testClientSecretRetryFallback")
    public String getTestClientSecret() {
        // Lire un secret spécifique depuis Vault à l'emplacement "secret/keycloak"
        VaultResponse response = vaultTemplate.read("secret/keycloak");
        if (response != null && response.getData() != null) {
            String secret = (String) response.getData().get("credentials.secret.test");
            return  secret;
        }
        return "Aucun secret trouvé dans Vault.";
    }
    public String testClientSecretRateLimiterFallback(Throwable t) {
        System.out.println("RateLimiter fallback for getTestClientSecret: " + t.getMessage());
        return "Rate limit exceeded for Vault access.";
    }

    public String testClientSecretBulkheadFallback(Throwable t) {
        System.out.println("Bulkhead fallback for getTestClientSecret: " + t.getMessage());
        return "Too many concurrent Vault requests.";
    }

    public String testClientSecretRetryFallback(Throwable t) {
        System.out.println("Retry fallback for getTestClientSecret: " + t.getMessage());
        return "Failed to retrieve test secret from Vault after retries.";
    }
}