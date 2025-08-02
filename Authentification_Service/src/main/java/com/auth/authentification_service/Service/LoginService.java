package com.auth.authentification_service.Service;

import com.auth.authentification_service.DTO.KeycloakTokenResponse;
import com.auth.authentification_service.DTO.TokenDto;
import com.auth.authentification_service.DTO.UserInfoDto;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);
    @Value("${keycloak.resource}")
    private String keycloakClientId;

    private final RestTemplate restTemplate;
    private final  VaultService vaultService;
    private final KeycloakService keycloakService;

    public LoginService(RestTemplate restTemplate , VaultService vaultService ,KeycloakService keycloakService) {
        this.restTemplate = restTemplate;
        this.vaultService = vaultService;
        this.keycloakService= keycloakService;
    }


    public TokenDto authenticateUser(String email, String password) throws Exception {
        logger.info("Récupération du client secret depuis Vault...");
        String keycloakClientSecret = vaultService.getClientSecret();
        logger.info("Client Secret from Vault: {}", keycloakClientSecret);
        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
        logger.info("URL de token : {}", tokenUrl);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", keycloakClientId);
        params.add("client_secret", keycloakClientSecret);
        params.add("username", email);
        params.add("password", password);
        params.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        logger.info("Envoi de la requête POST à Keycloak...");
        ResponseEntity<KeycloakTokenResponse> responseEntity = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, entity, KeycloakTokenResponse.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            logger.info("Tokens récupérés avec succès !");
            KeycloakTokenResponse tokenResponse = responseEntity.getBody();
            return new TokenDto(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
        } else {
            logger.error("Échec de l'authentification avec Keycloak !");
            throw new Exception("Échec de l'authentification avec Keycloak");
        }
    }

    @RateLimiter(name = "LoginServiceLimiter", fallbackMethod = "refreshRateLimiterFallback")
    @Bulkhead(name = "LoginServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "refreshBulkheadFallback")
    @Retry(name = "LoginServiceRetry", fallbackMethod = "refreshRetryFallback")
    public TokenDto refreshToken(String refreshToken) throws Exception {
        System.out.println("Tentative de rafraîchissement du token...");

        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", keycloakClientId);
        params.add("client_secret", vaultService.getClientSecret());
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        System.out.println("Envoi de la requête de refresh à Keycloak...");
        ResponseEntity<KeycloakTokenResponse> responseEntity = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, entity, KeycloakTokenResponse.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            System.out.println("Nouveaux tokens récupérés avec succès !");
            KeycloakTokenResponse tokenResponse = responseEntity.getBody();

            return new TokenDto(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
        } else {
            System.out.println("Échec du rafraîchissement du token !");
            throw new Exception("Échec du rafraîchissement du token");
        }
    }

    public TokenDto refreshRateLimiterFallback(String refreshToken, Throwable t) {
        System.out.println("RateLimiter fallback for refresh: " + t.getMessage());
        return new TokenDto("", "");
    }

    public TokenDto refreshBulkheadFallback(String refreshToken, Throwable t) {
        System.out.println("Bulkhead fallback for refresh: " + t.getMessage());
        return new TokenDto("", "");
    }

    public TokenDto refreshRetryFallback(String refreshToken, Throwable t) {
        System.out.println("Retry fallback for refresh: " + t.getMessage());
        return new TokenDto("", "");
    }

    public String decodeToken(String token) {
        try {
            System.out.println(" Tentative de décodage du token : " + token);

            // Décoder le token sans valider la signature
            DecodedJWT decodedJWT = JWT.decode(token);
            System.out.println("✅ Token décodé avec succès.");

            // Extraire l'ID utilisateur (le "sub")
            String userId = decodedJWT.getSubject();  // Le 'sub' dans le JWT est l'ID utilisateur
            String issuer = decodedJWT.getIssuer();   // Le 'iss' peut aussi être utile
            long expirationTime = decodedJWT.getExpiresAt().getTime(); // Expiration du token

            System.out.println("User ID (sub): " + userId);
            System.out.println("Issuer: " + issuer);
            System.out.println("Expiration Time: " + expirationTime);

            return userId;  // Retourner l'ID utilisateur
        } catch (Exception e) {
            // Gérer les erreurs dans le cas où le token serait invalide ou mal formé
            System.out.println("❌ Erreur lors du décodage du token : " + e.getMessage());
            return null;
        }
    }

    public UserInfoDto getUserInfo(String accessToken) {
        try {
            DecodedJWT decodedJWT = JWT.decode(accessToken);
            String id = decodedJWT.getSubject(); // Ajoute l'ID (sub)
            String firstName = decodedJWT.getClaim("given_name").asString();
            String lastName = decodedJWT.getClaim("family_name").asString();
            String email = decodedJWT.getClaim("email").asString();
            return new UserInfoDto(id,firstName,lastName,email);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de décoder le token", e);
        }
    }



    @RateLimiter(name = "LoginServiceLimiter", fallbackMethod = "assignManagerRoleRateLimiterFallback")
    @Bulkhead(name = "LoginServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "assignManagerRoleBulkheadFallback")
    @Retry(name = "LoginServiceRetry", fallbackMethod = "assignManagerRoleRetryFallback")
    public void assignManagerRoleToUser(String userId) throws Exception {
        System.out.println("🔄 Attribution du rôle MANAGER à l'utilisateur : " + userId);

        // Obtenir le token d'administration
        String adminToken = keycloakService.getAdminToken();

        // URL pour récupérer la liste des rôles dans le realm
        String rolesUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/roles";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);  // Définir le type de contenu à JSON

        // Requête pour récupérer la liste des rôles
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> rolesResponse = restTemplate.exchange(rolesUrl, HttpMethod.GET, entity, String.class);

        if (rolesResponse.getStatusCode() == HttpStatus.OK) {
            // Extraire l'ID du rôle MANAGER
            String roleId = extractRoleIdFromResponse(rolesResponse.getBody(), "MANAGER");

            if (roleId != null) {
                // URL de l'API Keycloak pour affecter un rôle à l'utilisateur
                String roleMappingUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/role-mappings/realm";

                // Définir le rôle MANAGER en utilisant l'ID obtenu dynamiquement
                String roleJson = "[{\"id\": \"" + roleId + "\", \"name\": \"MANAGER\"}]";
                HttpEntity<String> roleMappingEntity = new HttpEntity<>(roleJson, headers);

                // Effectuer la requête pour attribuer le rôle
                ResponseEntity<String> response = restTemplate.exchange(roleMappingUrl, HttpMethod.POST, roleMappingEntity, String.class);

                if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                    System.out.println("✅ Rôle MANAGER attribué avec succès !");
                } else {
                    System.out.println("❌ Échec de l'attribution du rôle MANAGER : " + response.getBody());
                    throw new Exception("Erreur lors de l'attribution du rôle.");
                }
            } else {
                System.out.println("❌ Le rôle MANAGER n'a pas été trouvé.");
                throw new Exception("Rôle MANAGER non trouvé.");
            }
        } else {
            System.out.println("❌ Erreur lors de la récupération des rôles : " + rolesResponse.getBody());
            throw new Exception("Erreur lors de la récupération des rôles.");
        }
    }


    public void assignManagerRoleRateLimiterFallback(String userId, Throwable t) {
        System.out.println("RateLimiter fallback for assignManagerRole: " + t.getMessage());
    }

    public void assignManagerRoleBulkheadFallback(String userId, Throwable t) {
        System.out.println("Bulkhead fallback for assignManagerRole: " + t.getMessage());
    }

    public void assignManagerRoleRetryFallback(String userId, Throwable t) {
        System.out.println("Retry fallback for assignManagerRole: " + t.getMessage());
    }
    // Fonction pour extraire dynamiquement l'ID du rôle MANAGER
    public  String extractRoleIdFromResponse(String jsonResponse, String roleName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode roles = objectMapper.readTree(jsonResponse);

            // Chercher le rôle avec le nom spécifié (MANAGER)
            for (JsonNode role : roles) {
                if (role.get("name").asText().equals(roleName)) {
                    return role.get("id").asText();  // Retourner l'ID du rôle
                }
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;  // Si le rôle n'a pas été trouvé
    }


    @RateLimiter(name = "LoginServiceLimiter", fallbackMethod = "logoutRateLimiterFallback")
    @Bulkhead(name = "LoginServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "logoutBulkheadFallback")
    @Retry(name = "LoginServiceRetry", fallbackMethod = "logoutRetryFallback")
    public void logout(String refreshToken) throws Exception {
        String logoutUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", keycloakClientId);
        params.add("client_secret", vaultService.getClientSecret());
        params.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        System.out.println("Envoi de la requête de déconnexion à Keycloak");

        ResponseEntity<String> responseEntity = restTemplate.exchange(
                logoutUrl, HttpMethod.POST, entity, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.NO_CONTENT) {
            System.out.println("Déconnexion réussie !");
        } else {
            throw new Exception("Échec de la déconnexion auprès de Keycloak");
        }
    }

    public void logoutRateLimiterFallback(String refreshToken, Throwable t) {
        System.out.println("RateLimiter fallback for logout: " + t.getMessage());
    }

    public void logoutBulkheadFallback(String refreshToken, Throwable t) {
        System.out.println("Bulkhead fallback for logout: " + t.getMessage());
    }

    public void logoutRetryFallback(String refreshToken, Throwable t) {
        System.out.println("Retry fallback for logout: " + t.getMessage());
    }
}
